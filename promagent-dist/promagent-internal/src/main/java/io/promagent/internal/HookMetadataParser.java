// Copyright 2017 The Promagent Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.promagent.internal;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Returned;
import io.promagent.annotations.Thrown;
import io.promagent.internal.HookMetadata.MethodSignature;
import net.bytebuddy.jar.asm.*;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Inspect all hook classes in a JAR file and load the metadata from the @Hook, @Before, and @After annotations.
 * <p/>
 * The tricky part is that we cannot create an instance of the hook classes, because they might reference
 * classes that are not available in the agent's premain phase. We need to parse the metadata without instantiating
 * the hook classes.
 */
public class HookMetadataParser {

    private final SortedSet<Path> hookJars;

    public HookMetadataParser(Collection<Path> hookJars) {
        this.hookJars = Collections.unmodifiableSortedSet(new TreeSet<>(hookJars));
    }

    /**
     * Get the {@link HookMetadata} for the hook classes without loading the hook classes.
     * <p/>
     * We unsuccessfully tried two alternative implementations before we ended up with the current one.
     * The first attempt was to use reflection like this:
     * <pre>
     *     Class hookClass = HookConfig.class.getClassLoader().loadClass(hookClassName);
     *     for (Method m : hookClass.getMethods()) {
     *         // ... get methodConfig
     *     }
     * </pre>
     * However, this throws a <tt>ClassNotFoundException</tt> in <tt>hookClass.getMethods()</tt>, because
     * a hook method may have parameters like <tt>ServletRequest</tt> that are not available during agent startup.
     * <p/>
     * The second attempt was to analyze the hook using Byte buddy like this:
     * <pre>
     *     TypeDescription typeDesc = typePool.describe(hookClassName).resolve();
     *     for (MethodDescription methodDesc : typeDesc.getDeclaredMethods()) {
     *         TypeList params = methodDesc.getParameters().asTypeList().asErasures();
     *         // ... get methodConfig
     *     }
     * </pre>
     * However, this throws a <tt>ClassNotFoundException</tt> in <tt>methodDesc.getParameters().asTypeList()</tt>.
     * We did not find any way in the Byte buddy API to access a method's parameter types without loading these types.
     * <p/>
     * The current implementation uses ASM directly to analyze the hook class's byte code.
     */
    SortedSet<HookMetadata> parse() throws IOException, ClassNotFoundException {
        return parse(className -> true); // Parse all classes found in the JAR files.
    }

    /**
     * See {@link #parse()}.
     *
     * The classNameFilter is used to parse only specific classes from the JAR files.
     */
    public SortedSet<HookMetadata> parse(Predicate<String> classNameFilter) throws IOException, ClassNotFoundException {
        SortedSet<HookMetadata> result = new TreeSet<>();
        for (String className : listAllJavaClasses(hookJars, classNameFilter)) {
            byte[] binaryRepresentation = readBinaryRepresentation(className);
            ClassReader classReader = new ClassReader(binaryRepresentation);
            HookMetadataBuilder hookMetadata = new HookMetadataBuilder(className);
            classReader.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (visible && typeEquals(desc, io.promagent.annotations.Hook.class)) {
                        return new AnnotationValueCollector("instruments", hookMetadata::addInstruments, Opcodes.ASM5, super.visitAnnotation(desc, visible));
                    } else {
                        return super.visitAnnotation(desc, visible);
                    }
                }

                @Override
                public MethodVisitor visitMethod(int i, String method, String desc, String signature, String[] strings) {
                    List<String> parameterTypes = Arrays.stream(Type.getArgumentTypes(desc))
                            .map(Type::getClassName)
                            .collect(Collectors.toList());
                    MethodSignatureBuilder builder = hookMetadata.newMethodSignature(parameterTypes);
                    return new MethodVisitor(Opcodes.ASM5, super.visitMethod(i, method, desc, signature, strings)) {
                        @Override
                        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                            if (visible && typeEquals(desc, Returned.class, Thrown.class)) {
                                builder.markReturnedOrThrown(parameter);
                            }
                            return super.visitParameterAnnotation(parameter, desc, visible);
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (visible && typeEquals(desc, Before.class, After.class)) {
                                return new AnnotationValueCollector("method", builder::addMethodName, Opcodes.ASM5, super.visitAnnotation(desc, visible));
                            } else {
                                return super.visitAnnotation(desc, visible);
                            }
                        }
                    };
                }

            }, ClassReader.SKIP_CODE);
            if (hookMetadata.isComplete()) {
                result.add(hookMetadata.build());
            }
        }
        return result;
    }

    private byte[] readBinaryRepresentation(String className) throws ClassNotFoundException {
        String classFileName = "/" + className.replace(".", "/") + ".class";
        try(InputStream stream = getResourceAsStream(classFileName)) {
            if (stream == null) {
                throw new ClassNotFoundException(className);
            }
            return IOUtils.toByteArray(stream);
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        }
    }

    private InputStream getResourceAsStream(String name) throws IOException {
        for (Path hookJar : hookJars) {
            try {
                URL url;
                // For convenient testing, hookJar may be a classes/ directory instead of a JAR file.
                if (Files.isDirectory(hookJar)) {
                    url = hookJar.toUri().resolve("." + name).toURL();
                } else if (Files.isRegularFile(hookJar)) {
                    url = new URL("jar:" + hookJar.toUri().toURL().toString() + "!" + name);
                } else {
                    throw new IOException("Invalid JAR file or classes directory: " + hookJar);
                }
                return url.openStream();
            } catch (FileNotFoundException e) {}
        }
        throw new FileNotFoundException(name + " not found in [" + hookJars.stream().map(Path::toString).reduce("", (s1, s2) -> s1 + ", " + s2) + "]");
    }

    private boolean typeEquals(String typeDescriptor, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (clazz.getName().equals(Type.getType(typeDescriptor).getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Helper to create {@link HookMetadata} isntances.
     */
    private static class HookMetadataBuilder {

        private String hookClassName;
        private List<String> instruments = new ArrayList<>();
        private List<MethodSignatureBuilder> methods = new ArrayList<>();

        private HookMetadataBuilder(String hookClassName) {
            this.hookClassName = hookClassName;
        }

        private void addInstruments(String instruments) {
            this.instruments.add(instruments);
        }

        private MethodSignatureBuilder newMethodSignature(List<String> parameterTypes) {
            MethodSignatureBuilder builder = new MethodSignatureBuilder();
            for (String parameterType : parameterTypes) {
                builder.addParameterType(parameterType);
            }
            methods.add(builder);
            return builder;
        }

        private HookMetadata build() {
            SortedSet<MethodSignature> methodSignatures = new TreeSet<>();
            for (MethodSignatureBuilder builder : methods) {
                methodSignatures.addAll(builder.build());
            }
            return new HookMetadata(hookClassName, instruments, methodSignatures);
        }

        private boolean isComplete() {
            return hookClassName != null && instruments.size() > 0 && methods.size() > 0;
        }
    }

    /**
     * Helper to create {@link MethodSignature} instances.
     * <p/>
     * There's a slight mismatch between a {@link MethodSignatureBuilder} and a {@link MethodSignature}:
     * While a {@link MethodSignature} can only contain a single method name,
     * the {@link MethodSignatureBuilder} may have multiple method names.
     * Therefore, the {@link #build()} method might create multiple {@link MethodSignature} instances
     * out of a single {@link MethodSignatureBuilder}.
     */
    private static class MethodSignatureBuilder {

        private static class ParameterType {
            final String type;
            boolean isReturnedOrThrown; // method parameters annotated with @Returned or @Thrown will be ignored.

            private ParameterType(String type) {
                this.type = type;
                this.isReturnedOrThrown = false;
            }
        }

        SortedSet<String> methodNames = new TreeSet<>();
        List<ParameterType> parameterTypes = new ArrayList<>();

        private void addMethodName(String methodName) {
            methodNames.add(methodName);
        }

        private void addParameterType(String parameterType) {
            parameterTypes.add(new ParameterType(parameterType));
        }

        private SortedSet<MethodSignature> build() {
            List<String> strippedParameterTypes = parameterTypes.stream()
                    .filter(p -> ! p.isReturnedOrThrown)
                    .map(p -> p.type)
                    .collect(Collectors.toList());
            SortedSet<MethodSignature> result = new TreeSet<>();
            for (String methodName : methodNames) {
                result.add(new MethodSignature(methodName, strippedParameterTypes));
            }
            return result;
        }

        public void markReturnedOrThrown(int parameter) {
            // We know that parameter is a valid index in parameterTypes.
            parameterTypes.get(parameter).isReturnedOrThrown = true;
        }
    }

    /**
     * Collect annotation values from @Hook(instruments = ...) or @Before(method = ...) or @After(method = ...)
     * <p/>
     * Example: If the annotation is @Before(method={"service", "doFilter"})
     * then using a AnnotationValueCollector with methodName=="method" will add the call
     * consumer.accept("service") and consumer.accept("doFilter")
     */
    private static class AnnotationValueCollector extends AnnotationVisitor {
        private final Consumer<String> consumer;
        private final String methodName;
        private AnnotationValueCollector(String methodName, Consumer<String> consumer, int api, AnnotationVisitor av) {
            super(api, av);
            this.consumer = consumer;
            this.methodName = methodName;
        }
        @Override
        public AnnotationVisitor visitArray(String name) {
            if (methodName.equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM5, super.visitArray(name)) {
                    @Override
                    public void visit(String name, Object value) {
                        consumer.accept(value.toString());
                    }
                };
            } else {
                return super.visitArray(name);
            }
        }
    }

    /**
     * List all Java classes found in the JAR files.
     *
     */
    private static Set<String> listAllJavaClasses(Set<Path> hookJars, Predicate<String> classNameFilter) throws IOException {
        Set<String> result = new TreeSet<>();
        for (Path hookJar : hookJars) {
            // For convenient testing, hookJar may be a classes/ directory instead of a JAR file.
            if (hookJar.toFile().isDirectory()) {
                try (Stream<Path> dirEntries = Files.walk(hookJar)) {
                    addClassNames(dirEntries.map(hookJar::relativize).map(Path::toString), result, classNameFilter);
                }
            }
            else if (hookJar.toFile().isFile()) {
                try (ZipFile zipFile = new ZipFile(hookJar.toFile())) {
                    addClassNames(zipFile.stream().map(ZipEntry::getName), result, classNameFilter);
                }
            } else {
                throw new IOException(hookJar + ": Failed to read file or directory.");
            }
        }
        return result;
    }

    /**
     * Convert class file paths to class names and add them to result.
     */
    private static void addClassNames(Stream<String> paths, Collection<String> result, Predicate<String> classNameFilter) {
        paths
                .filter(name -> name.endsWith(".class"))
                .map(name -> name.substring(0, name.length() - ".class".length()))
                .map(name -> name.startsWith("/") ? name.substring(1) : name)
                .map(name -> name.replace("/", "."))
                .filter(classNameFilter)
                .collect(Collectors.toCollection(() -> result));
    }
}

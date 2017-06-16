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

import io.promagent.agent.annotations.After;
import io.promagent.agent.annotations.Before;
import io.promagent.agent.annotations.Hook;
import net.bytebuddy.jar.asm.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class telling which classes / interfaces / methods should be instrumented.
 * <p/>
 * The configuration is created from analyzing the hook classes' byte code, see {@link #of(String...)}.
 */
public class HookConfig {

    static class ClassOrInterfaceConfig implements Comparable<ClassOrInterfaceConfig> {

        private final String classOrInterfaceName;
        private final Set<MethodConfig> instrumentedMethods;

        ClassOrInterfaceConfig(String classOrInterfaceName, Set<MethodConfig> instrumentedMethods) {
            this.classOrInterfaceName = classOrInterfaceName;
            this.instrumentedMethods = Collections.unmodifiableSet(new TreeSet<>(instrumentedMethods));
        }

        String getClassOrInterfaceName() {
            return classOrInterfaceName;
        }

        Set<MethodConfig> getInstrumentedMethods() {
            return instrumentedMethods;
        }

        @Override
        public String toString() {
            String delimiter = System.lineSeparator() + "  * ";
            return classOrInterfaceName + delimiter + String.join(delimiter, strings(instrumentedMethods));
        }

        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass() && compareTo((ClassOrInterfaceConfig) o) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(toString());
        }

        @Override
        public int compareTo(ClassOrInterfaceConfig o) {
            return toString().compareTo(o.toString());
        }
    }

    static class MethodConfig implements Comparable<MethodConfig> {

        private final String methodName;
        private final List<String> parameterTypes;

        private MethodConfig(String methodName, List<String> parameterTypes) {
            this.methodName = methodName;
            this.parameterTypes = Collections.unmodifiableList(parameterTypes);
        }

        String getMethodName() {
            return methodName;
        }

        List<String> getParameterTypes() {
            return parameterTypes;
        }

        @Override
        public String toString() {
            return methodName + "(" + String.join(", ", parameterTypes) + ")";
        }

        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass() && compareTo((MethodConfig) o) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(toString());
        }

        @Override
        public int compareTo(MethodConfig o) {
            return toString().compareTo(o.toString());
        }
    }

    private final Set<ClassOrInterfaceConfig> instrumentedClassesOrInterfaces;

    private HookConfig(Set<ClassOrInterfaceConfig> instrumentedClassesOrInterfaces) {
        this.instrumentedClassesOrInterfaces = Collections.unmodifiableSet(new TreeSet<>(instrumentedClassesOrInterfaces));
    }

    Set<ClassOrInterfaceConfig> getInstrumentedClassesOrInterfaces() {
        return instrumentedClassesOrInterfaces;
    }

    @Override
    public String toString() {
        return String.join(System.lineSeparator(), strings(instrumentedClassesOrInterfaces));
    }

    /**
     * Get the {@link HookConfig} for the hook classes without loading the hook class.
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
    public static HookConfig of(String... hookClassNames) throws ClassNotFoundException {
        Set<ClassOrInterfaceConfig> classOrInterfaceConfigs = new TreeSet<>();
        for (String hookClassName : hookClassNames) {
            byte[] binaryRepresentation = readBinaryRepresentation(hookClassName);
            ClassReader classReader = new ClassReader(binaryRepresentation);
            List<String> hookAnnotationValue = new ArrayList<>();
            List<BeforeOrAfterMethod> beforeOrAfterMethods = new ArrayList<>();
            classReader.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (visible && typeEquals(desc, Hook.class)) {
                        return new AnnotationValueCollector("instruments", hookAnnotationValue, Opcodes.ASM5, super.visitAnnotation(desc, visible));
                    } else {
                        return super.visitAnnotation(desc, visible);
                    }
                }

                @Override
                public MethodVisitor visitMethod(int i, String method, String desc, String signature, String[] strings) {
                    BeforeOrAfterMethod beforeOrAfterMethod = new BeforeOrAfterMethod();
                    beforeOrAfterMethods.add(beforeOrAfterMethod);
                    beforeOrAfterMethod.parameterTypes.addAll(Arrays.stream(Type.getArgumentTypes(desc)).map(Type::getClassName).collect(Collectors.toList()));
                    return new MethodVisitor(Opcodes.ASM5, super.visitMethod(i, method, desc, signature, strings)) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (visible && typeEquals(desc, Before.class, After.class)) {
                                return new AnnotationValueCollector("method", beforeOrAfterMethod.annotationValue, Opcodes.ASM5, super.visitAnnotation(desc, visible));
                            } else {
                                return super.visitAnnotation(desc, visible);
                            }
                        }
                    };
                }

            }, ClassReader.SKIP_CODE);
            for (String classOrInterface : hookAnnotationValue) {
                Set<MethodConfig> methodConfigs = new TreeSet<>();
                for (BeforeOrAfterMethod data : beforeOrAfterMethods) {
                    for (String method : data.annotationValue) {
                        methodConfigs.add(new MethodConfig(method, data.parameterTypes));
                    }
                }
                if (!methodConfigs.isEmpty()) {
                    classOrInterfaceConfigs.add(new ClassOrInterfaceConfig(classOrInterface, methodConfigs));
                }
            }
        }
        return new HookConfig(classOrInterfaceConfigs);
    }

    private static byte[] readBinaryRepresentation(String className) throws ClassNotFoundException {
        String classFileName = "/" + className.replace(".", "/") + ".class";
        try(InputStream stream = HookConfig.class.getResourceAsStream(classFileName)) {
            if (stream == null) {
                throw new ClassNotFoundException(className);
            }
            return IOUtils.toByteArray(stream);
        } catch (IOException e) {
            throw new ClassNotFoundException(className);
        }
    }

    private static boolean typeEquals(String typeDescriptor, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (clazz.getName().equals(Type.getType(typeDescriptor).getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Info about a method annotated with @Before or @After.
     * <p/>
     * Example:
     * <pre>
     *     &#064;Before(methods={"service", "doFilter"})
     *     public void before(ServletRequest req, ServletResponse resp) {
     *         // ...
     *     }
     * </pre>
     * For the example above, the values would be:
     * <ul>
     * <li>annotationValue: "service", "doFilter"
     * <li>parameterTypes: "javax.servlet.ServletRequest", "javax.servlet.ServletResponse"
     * </ul>
     */
    private static class BeforeOrAfterMethod {
        private final List<String> annotationValue = new ArrayList<>();
        private final List<String> parameterTypes = new ArrayList<>();
    }

    /**
     * If the annotation has a method with the given "methodName", and the method returns a String[], the String values will be added to the "result" list.
     * <p/>
     * Example: If the annotation is @Before(method={"service", "doFilter"})
     * then using a AnnotationValueCollector with methodName="method" will add the
     * Strings "service" and "doFilter" to the result list.
     */
    private static class AnnotationValueCollector extends AnnotationVisitor {
        private final List<String> result;
        private final String methodName;
        private AnnotationValueCollector(String methodName, List<String> result, int api, AnnotationVisitor av) {
            super(api, av);
            this.result = result;
            this.methodName = methodName;
        }
        @Override
        public AnnotationVisitor visitArray(String name) {
            if (methodName.equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM5, super.visitArray(name)) {
                    @Override
                    public void visit(String name, Object value) {
                        result.add(value.toString());
                    }
                };
            } else {
                return super.visitArray(name);
            }
        }
    }

    private static List<String> strings(Collection<?> list) {
        return list.stream().map(Object::toString).collect(Collectors.toList());
    }
}

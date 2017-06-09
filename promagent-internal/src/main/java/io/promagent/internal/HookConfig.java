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
import io.promagent.agent.annotations.Hook;
import io.promagent.agent.annotations.Before;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class telling which classes / interfaces / methods should be instrumented.
 * <p/>
 * The configuration is created from analyzing the hook class's byte code, see {@link #of(String, TypePool)}.
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
     * Get the {@link HookConfig} for a hook class without loading the hook class.
     * <p/>
     * If we did this with reflection like this...
     * <pre>
     *     Class hookClass = Thread.currentThread().getContextClassLoader().loadClass(hookClassName);
     *     for (Method m : hookClass.getMethods() {
     *         // ... get methodConfig
     *     }
     * </pre>
     * ... we would get a <tt>ClassNotFoundException</tt> in <tt>hookClass.getMethods()</tt>, because
     * a hook method may have parameters like <tt>ServletRequest</tt> that are not available during agent startup.
     * <p/>
     * To avoid this, we use Byte buddy to analyze the hook class's byte code directly without loading the class.
     */
    public static HookConfig of(String hookClassName, TypePool typePool) {
        Set<ClassOrInterfaceConfig> classOrInterfaceConfigs = new TreeSet<>();
        TypeDescription typeDesc = typePool.describe(hookClassName).resolve();
        for (String classOrInterfaceName : readAnnotation(typeDesc, Hook.class.getName(), "instruments")) {
            Set<MethodConfig> instrumentedMethods = new TreeSet<>();
            for (MethodDescription methodDesc : typeDesc.getDeclaredMethods()) {
                for (String methodName : readAnnotation(methodDesc, Before.class.getName(), "method")) {
                    instrumentedMethods.add(new MethodConfig(methodName, readArgumentTypes(methodDesc)));
                }
                for (String methodName : readAnnotation(methodDesc, After.class.getName(), "method")) {
                    instrumentedMethods.add(new MethodConfig(methodName, readArgumentTypes(methodDesc)));
                }
            }
            if (! instrumentedMethods.isEmpty()) {
                classOrInterfaceConfigs.add(new ClassOrInterfaceConfig(classOrInterfaceName, instrumentedMethods));
            }
        }
        return new HookConfig(classOrInterfaceConfigs);
    }

    private static List<String> readArgumentTypes(MethodDescription methodDesc) {
        List<String> result = new ArrayList<>();
        for (TypeDescription type :  methodDesc.getParameters().asTypeList().asErasures()) {
            result.add(type.getCanonicalName());
        }
        return result;
    }

    /**
     * Assuming the annotation has a String[] as value, we read the value and return it as a Lists&lt;String&gt;.
     */
    private static List<String> readAnnotation(AnnotationSource desc, String annotationClass, String annotationMethod) {
        for (AnnotationDescription annotation : desc.getDeclaredAnnotations()) {
            if (annotation.getAnnotationType().getName().equals(annotationClass)) {
                MethodDescription methodDesc = annotation
                        .getAnnotationType()
                        .getDeclaredMethods()
                        .filter(ElementMatchers.named(annotationMethod))
                        .get(0);
                String[] result = annotation.getValue(methodDesc.asDefined()).resolve(String[].class);
                if (result != null) {
                    return Arrays.asList(result);
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<String> strings(Collection<?> list) {
        return list.stream().map(Object::toString).collect(Collectors.toList());
    }
}

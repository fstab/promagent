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

package io.promagent.agent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Delegate calls from the PromagentAdvice to the actual Hooks.
 */
public class Delegator {

    private static final String ANNOTATION_PACKAGE = "io.promagent.annotations";
    private static final String ANNOTATION_METHOD = "method";

    private static SortedSet<HookMetadata> hookMetadata;
    private static Class<? extends Annotation> before;
    private static Class<? extends Annotation> after;

    @SuppressWarnings("unchecked")
    public static void init(SortedSet<HookMetadata> hookMetadata) throws ClassNotFoundException {
        Delegator.hookMetadata = hookMetadata;
        ClassLoader cl = ClassLoaderCache.getInstance().currentClassLoader();
        before = (Class<? extends Annotation>) cl.loadClass(ANNOTATION_PACKAGE + ".Before");
        after = (Class<? extends Annotation>) cl.loadClass(ANNOTATION_PACKAGE + ".After");
    }

    /**
     * Create a new instance of each hook class satisfying the following criterea:
     * <ul>
     *     <li>that.getClass() is assignable to the value of the Hook's instruments annotation
     *     <li>The name of the instrumented method and the number of arguments match.
     * </ul>
     * The result may still contain hooks that don't match. This happens if the Hook method differs
     * only in the argument types of the intercepted method. However, these Hooks will
     * be ignored when calling {@link #invokeBefore(Object, Method, Object...)}
     * and {@link #invokeAfter(Object, Method, Object...)}, so it's ok to include them here.
     */
    public static List<Object> createHookInstances(Object that, Method method) {
        return hookMetadata.stream()
                .filter(hook -> classOrInterfaceMatches(that.getClass(), hook))
                .filter(hook -> methodNameAndNumArgsMatch(method, hook))
                .map(hook -> createHookClass(hook))
                .map(hookClass -> createHookInstance(hookClass))
                .collect(Collectors.toList());
    }

    /**
     * Invoke the matching Hook methods annotated with @Before
     */
    public static void invokeBefore(Object hookInstance, Method method, Object... args) throws HookException {
        invoke(before, hookInstance, method.getName(), args);
    }

    /**
     * Invoke the matching Hook methods annotated with @After
     */
    public static void invokeAfter(Object hookInstance, Method method, Object... args) throws HookException {
        invoke(after, hookInstance, method.getName(), args);
    }

    private static boolean classOrInterfaceMatches(Class<?> classToBeInstrumented, HookMetadata hook) {
        Set<String> classesAndInterfaces = getAllSuperClassesAndInterfaces(classToBeInstrumented);
        return hook.getInstruments().stream().anyMatch(classesAndInterfaces::contains);
    }

    private static Set<String> getAllSuperClassesAndInterfaces(Class<?> clazz) {
        Set<String> result = new HashSet<>();
        addAllSuperClassesAndInterfaces(clazz, result);
        return result;
    }

    private static void addAllSuperClassesAndInterfaces(Class<?> clazz, Set<String> result) {
        if (clazz == null) {
            return;
        }
        if (result.contains(clazz.getName())) {
            return;
        }
        result.add(clazz.getName());
        for (Class<?> ifc : clazz.getInterfaces()) {
            addAllSuperClassesAndInterfaces(ifc, result);
        }
        addAllSuperClassesAndInterfaces(clazz.getSuperclass(), result);
    }

    private static boolean methodNameAndNumArgsMatch(Method method, HookMetadata hook) {
        return hook.getMethods().stream().anyMatch(m -> methodNameAndNumArgsMatch(method, m));
    }

    private static boolean methodNameAndNumArgsMatch(Method method, HookMetadata.MethodSignature hookMethod) {
        if (!method.getName().equals(hookMethod.getMethodName())) {
            return false;
        }
        if (method.getParameterCount() != hookMethod.getParameterTypes().size()) {
            return false;
        }
        return true;
    }

    private static Class<?> createHookClass(HookMetadata hook) {
        try {
            return ClassLoaderCache.getInstance().currentClassLoader().loadClass(hook.getHookClassName());
        } catch (ClassNotFoundException e) {
            throw new HookException("Failed to load Hook class " + hook.getHookClassName() + ": " + e.getMessage(), e);
        }
    }

    private static Object createHookInstance(Class<?> hookClass) {
        try {
            return hookClass.newInstance();
        } catch (Exception e) {
            throw new HookException("Failed to create new instance of hook " + hookClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static void invoke(Class<? extends Annotation> annotation, Object hookInstance, String methodName, Object... args) throws HookException {
        Method method = findHookMethod(annotation, hookInstance, methodName, args);
        try {
            method.invoke(hookInstance, args);
        } catch (Exception e) {
            throw new HookException("Failed to call " + method.getName() + "() on " + hookInstance.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static Method findHookMethod(Class<? extends Annotation> annotation, Object hookInstance, String methodName, Object... args) throws HookException {
        Method result = null;
        for (Method method : allAnnotatedMethods(annotation, hookInstance, methodName)) {
            if (parameterTypesMatch(method, args)) {
                if (result != null) {
                    throw new HookException(errorMessage("More than one", annotation, hookInstance, methodName, args));
                }
                result = method;
            }
        }
        if (result == null) {
            throw new HookException(errorMessage("No", annotation, hookInstance, methodName, args));
        }
        return result;
    }


    /**
     * Example: Find all methods annotated with @Before(method="service").
     * In the example, annotation is Before.class, and methodName is "service".
     */
    private static List<Method> allAnnotatedMethods(Class<? extends Annotation> annotation, Object hookInstance, String methodName) throws HookException {
        List<Method> result = new ArrayList<>();
        try {
            for (Method method : hookInstance.getClass().getMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    for (String arg : getMethodNames(method.getAnnotation(annotation))) {
                        if (arg.equals(methodName)) {
                            result.add(method);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new HookException("Failed to read @" + annotation.getSimpleName() + " annotations from " + hookInstance.getClass().getName() + ": " + e.getMessage(), e);
        }
        if (result.isEmpty()) {
            throw new HookException(errorMessage("No", annotation, hookInstance, methodName));
        }
        return result;
    }

    private static String[] getMethodNames(Annotation annotation) throws HookException {
        if (before.isAssignableFrom(annotation.getClass()) || after.isAssignableFrom(annotation.getClass())) {
            try {
                return (String[]) annotation.getClass().getMethod(ANNOTATION_METHOD).invoke(annotation);
            } catch (Exception e) {
                throw new HookException("Failed to read @Before or @After annotation: " + e.getMessage(), e);
            }
        } else {
            return new String[]{};
        }
    }

    // TODO: We could extend this to find the "closest" match, like in Java method calls.
    private static boolean parameterTypesMatch(Method method, Object... args) {
        if (args.length != method.getParameterCount()) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (!unboxed(method.getParameterTypes()[i]).isAssignableFrom(unboxed(args[i].getClass()))) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> unboxed(Class<?> clazz) {
        if (clazz == boolean.class) {
            return Boolean.class;
        }
        if (clazz == byte.class) {
            return Byte.class;
        }
        if (clazz == char.class) {
            return Character.class;
        }
        if (clazz == short.class) {
            return Short.class;
        }
        if (clazz == int.class) {
            return Integer.class;
        }
        if (clazz == long.class) {
            return Long.class;
        }
        if (clazz == float.class) {
            return Float.class;
        }
        if (clazz == double.class) {
            return Double.class;
        }
        return clazz;
    }

    private static String errorMessage(String prefix, Class<? extends Annotation> annotation, Object hookInstance, String methodName) {
        return errorMessage(prefix, annotation, hookInstance, methodName, null);
    }

    private static String errorMessage(String prefix, Class<? extends Annotation> annotation, Object hookInstance, String methodName, Object... args) {
        StringBuilder result = new StringBuilder(prefix);
        result.append(" method annotated with @").append(annotation.getSimpleName()).append("(method=\"").append(methodName).append("\")");
        if (args != null) {
            boolean first = true;
            result.append(" with argument types matching (");
            for (Object arg : args) {
                if (!first) {
                    result.append(", ");
                }
                first = false;
                result.append(arg.getClass().getSimpleName());
            }
            result.append(")");
        }
        result.append(" found in ").append(hookInstance.getClass().getSimpleName());
        return result.toString();
    }
}

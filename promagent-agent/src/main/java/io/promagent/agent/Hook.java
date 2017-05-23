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

import io.promagent.agent.annotations.After;
import io.promagent.agent.annotations.Before;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an instance of a class annotated with @Hook.
 * This Wrapper is loaded from the bootstrap class loader and does not have any direct references
 * to classes from the promagent-internal module.
 * The "delegate" is the instance of the class annotated with @Hook.
 * The "delegate" is loaded using the {@link ClassLoaderCache}.
 *
 * @see HookFactory
 */
public class Hook {

    private final Object delegate; // instance of the class annotated with @Hook

    Hook(Object delegate) {
        this.delegate = delegate;
    }

    public void before(String methodName, Object... args) throws HookException {
        invoke(Before.class, methodName, args);
    }

    public void after(String methodName, Object... args) throws HookException {
        invoke(After.class, methodName, args);
    }

    private void invoke(Class<? extends Annotation> annotation, String methodName, Object... args) throws HookException {
        Method method = findHookMethod(annotation, methodName, args);
        try {
            method.invoke(delegate, args);
        } catch (Exception e) {
            throw new HookException("Failed to call " + method.getName() + "() on " + delegate.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private Method findHookMethod(Class<? extends Annotation> annotation, String methodName, Object... args) throws HookException {
        Method result = null;
        for (Method method : allAnnotatedMethods(annotation, methodName)) {
            if (parameterTypesMatch(method, args)) {
                if (result != null) {
                    throw new HookException(errorMessage("More than one", annotation, methodName, args));
                }
                result = method;
            }
        }
        if (result == null) {
            throw new HookException(errorMessage("No", annotation, methodName, args));
        }
        return result;
    }


    /**
     * Example: Find all methods annotated with @Before(method="service").
     * In the example, annotation is Before.class, and methodName is "service".
     */
    private List<Method> allAnnotatedMethods(Class<? extends Annotation> annotation, String methodName) throws HookException {
        List<Method> result = new ArrayList<>();
        try {
            for (Method method : delegate.getClass().getMethods()) {
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
            throw new HookException("Failed to read @" + annotation.getSimpleName() + " annotations from " + delegate.getClass().getName() + ": " + e.getMessage(), e);
        }
        if (result.isEmpty()) {
            throw new HookException(errorMessage("No", annotation, methodName));
        }
        return result;
    }

    private String[] getMethodNames(Annotation annotation) throws HookException {
        if (Before.class.isAssignableFrom(annotation.getClass())) {
            return ((Before) annotation).method();
        }
        if (After.class.isAssignableFrom(annotation.getClass())) {
            return ((After) annotation).method();
        }
        throw new IllegalArgumentException("Unsupported annotation type " + annotation.getClass().getName() + " in Hook.");
    }

    // TODO: We could extend this to find the "closest" match, like in Java method calls.
    private boolean parameterTypesMatch(Method method, Object... args) {
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

    private Class<?> unboxed(Class<?> clazz) {
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

    private String errorMessage(String prefix, Class<? extends Annotation> annotation, String methodName) {
        return errorMessage(prefix, annotation, methodName, null);
    }

    private String errorMessage(String prefix, Class<? extends Annotation> annotation, String methodName, Object... args) {
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
        result.append(" found in ").append(delegate.getClass().getSimpleName());
        return result.toString();
    }
}

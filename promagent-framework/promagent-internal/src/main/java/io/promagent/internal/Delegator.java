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

import io.promagent.agent.ClassLoaderCache;
import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Returned;
import io.promagent.annotations.Thrown;
import io.promagent.hookcontext.MetricsStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Delegator is called from the Byte Buddy Advice, and calls the Hook's @Before and @After methods.
 * <p>
 * TODO: This is called often, should be performance optimized, e.g. caching hook method handles, etc.
 */
public class Delegator {

    private static Delegator instance; // not thread-safe, but it is set only once in the agent's premain method.

    private final SortedSet<HookMetadata> hookMetadata;
    private final MetricsStore metricsStore;
    private final ClassLoaderCache classLoaderCache;
    private final ThreadLocal<Map<Class<?>, Object>> threadLocal;

    private Delegator(SortedSet<HookMetadata> hookMetadata, MetricsStore metricsStore, ClassLoaderCache classLoaderCache) {
        this.hookMetadata = hookMetadata;
        this.metricsStore = metricsStore;
        this.classLoaderCache = classLoaderCache;
        this.threadLocal = ThreadLocal.withInitial(HashMap::new);
    }

    public static void init(SortedSet<HookMetadata> hookMetadata, MetricsStore metricsStore, ClassLoaderCache classLoaderCache) {
        instance = new Delegator(hookMetadata, metricsStore, classLoaderCache);
    }

    /**
     * Should be called from the Advice's @OnMethodEnter method. Returns the list of Hooks to be passed on to after()
     */
    public static List<HookInstance> before(Class<?> interceptedClass, Method interceptedMethod, Object[] args) {
        return instance.doBefore(interceptedClass, interceptedMethod, args);
    }

    private List<HookInstance> doBefore(Class<?> interceptedClass, Method interceptedMethod, Object[] args) {
        List<HookInstance> hookInstances = loadFromThreadLocalOrCreate(interceptedClass, interceptedMethod);
        for (HookInstance hookInstance : hookInstances) {
            invokeBefore(hookInstance.getInstance(), interceptedMethod, args);
        }
        return hookInstances;
    }

    /**
     * Should be called from the Advice's @OnMethodExit method. First parameter is the list of hooks returned by before()
     */
    public static void after(List<HookInstance> hookInstances, Method interceptedMethod, Object[] args, Object returned, Throwable thrown) {
        instance.doAfter(hookInstances, interceptedMethod, args, returned, thrown);
    }

    private void doAfter(List<HookInstance> hookInstances, Method interceptedMethod, Object[] args, Object returned, Throwable thrown) {
        if (hookInstances != null) {
            for (HookInstance hookInstance : hookInstances) {
                invokeAfter(hookInstance.getInstance(), interceptedMethod, args, returned, thrown);
                if (!hookInstance.isRecursiveCall()) {
                    threadLocal.get().remove(hookInstance.getInstance().getClass());
                }
            }
        }
    }

    private List<HookInstance> loadFromThreadLocalOrCreate(Class<?> interceptedClass, Method interceptedMethod) {
        return hookMetadata.stream()
                .filter(hook -> classOrInterfaceMatches(interceptedClass, hook))
                .filter(hook -> methodNameAndNumArgsMatch(interceptedMethod, hook))
                .map(hook -> loadHookClass(hook))
                .filter(hookClass -> argumentTypesMatch(hookClass, interceptedMethod))
                .filter(hookClass -> ! shouldBeSkipped(hookClass))
                .map(hookClass -> loadFromTheadLocalOrCreate(hookClass))
                .collect(Collectors.toList());
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

    private static boolean methodNameAndNumArgsMatch(Method interceptedMethod, HookMetadata hook) {
        return hook.getMethods().stream()
                .anyMatch(instrumentedMethod -> methodNameAndNumArgsMatch(interceptedMethod, instrumentedMethod));
    }

    private static boolean methodNameAndNumArgsMatch(Method interceptedMethod, HookMetadata.MethodSignature instrumentedMethod) {
        if (!interceptedMethod.getName().equals(instrumentedMethod.getMethodName())) {
            return false;
        }
        if (interceptedMethod.getParameterCount() != instrumentedMethod.getParameterTypes().size()) {
            return false;
        }
        return true;
    }

    private Class<?> loadHookClass(HookMetadata hook) {
        try {
            return classLoaderCache.currentClassLoader().loadClass(hook.getHookClassName());
        } catch (ClassNotFoundException e) {
            throw new HookException("Failed to load Hook class " + hook.getHookClassName() + ": " + e.getMessage(), e);
        }
    }

    private static boolean argumentTypesMatch(Class<?> hookClass, Method interceptedMethod) {
        List<Method> before = findHookMethods(Before.class, hookClass, interceptedMethod);
        List<Method> after = findHookMethods(After.class, hookClass, interceptedMethod);
        return ! (before.isEmpty() && after.isEmpty());
    }

    private static List<Method> findHookMethods(Class<? extends Annotation> annotation, Class<?> hookClass, Method interceptedMethod) throws HookException {
        return Stream.of(hookClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .filter(method -> getMethodNames(method.getAnnotation(annotation)).contains(interceptedMethod.getName()))
                .filter(method -> parameterTypesMatch(method, interceptedMethod))
                .collect(Collectors.toList());
    }

    private static List<String> getMethodNames(Annotation annotation) throws HookException {
        if (Before.class.isAssignableFrom(annotation.getClass())) {
            return Arrays.asList(((Before) annotation).method());
        } else if (After.class.isAssignableFrom(annotation.getClass())) {
            return Arrays.asList(((After) annotation).method());
        } else {
            throw new HookException("Unsupported Annotation: @" + annotation.getClass().getSimpleName() + ".");
        }
    }

    // TODO: We could extend this to find the "closest" match, like in Java method calls.
    private static boolean parameterTypesMatch(Method hookMethod, Method interceptedMethod) {
        List<Class<?>> hookParameterTypes = stripReturnedAndThrown(hookMethod);
        if (hookParameterTypes.size() != interceptedMethod.getParameterCount()) {
            return false;
        }
        for (int i = 0; i < hookParameterTypes.size(); i++) {
            Class<?> hookParam = hookParameterTypes.get(i);
            Class<?> interceptedParam = interceptedMethod.getParameterTypes()[i];
            if (!hookParam.equals(interceptedParam)) {
                return false;
            }
        }
        return true;
    }

    private static List<Class<?>> stripReturnedAndThrown(Method hookMethod) {
        Class<?>[] allTypes = hookMethod.getParameterTypes();
        Annotation[][] annotations = hookMethod.getParameterAnnotations();
        if (allTypes.length != annotations.length) {
            throw new HookException("Method.getParameterAnnotations() returned an unexpected value. This is a bug in promagent.");
        }
        List<Class<?>> result = new ArrayList<>();
        for (int i=0; i<allTypes.length; i++) {
            if (Arrays.stream(annotations[i])
                    .map(Annotation::annotationType)
                    .noneMatch(a -> Returned.class.equals(a) || Thrown.class.equals(a))) {
                result.add(allTypes[i]);
            }
        }
        return result;
    }

    private boolean shouldBeSkipped(Class<?> hookClass) {
        return hookClass.getAnnotation(io.promagent.annotations.Hook.class).skipNestedCalls()
                && threadLocal.get().containsKey(hookClass);
    }

    private HookInstance loadFromTheadLocalOrCreate(Class<?> hookClass) {
        Object existingHookInstance = threadLocal.get().get(hookClass);
        if (existingHookInstance != null) {
            return new HookInstance(existingHookInstance, true);
        } else {
            String errMsg = "Failed to create new instance of hook " + hookClass.getSimpleName() + ": ";
            try {
                Object newHookInstance = hookClass.getConstructor(MetricsStore.class).newInstance(metricsStore);
                threadLocal.get().put(hookClass, newHookInstance);
                return new HookInstance(newHookInstance, false);
            } catch (NoSuchMethodException e) {
                throw new HookException(errMsg + "Hook classes must have a public constructor with a single parameter of type " + MetricsStore.class.getSimpleName(), e);
            } catch (Exception e) {
                throw new HookException(errMsg + e.getMessage(), e);
            }
        }
    }

    /**
     * Invoke the matching Hook methods annotated with @Before
     */
    private static void invokeBefore(Object hookInstance, Method interceptedMethod, Object[] args) throws HookException {
        invoke(Before.class, hookInstance, interceptedMethod, args, null, null);
    }

    /**
     * Invoke the matching Hook methods annotated with @After
     */
    private static void invokeAfter(Object hookInstance, Method interceptedMethod, Object[] args, Object returned, Throwable thrown) throws HookException {
        invoke(After.class, hookInstance, interceptedMethod, args, returned, thrown);
    }

    private static void invoke(Class<? extends Annotation> annotation, Object hookInstance, Method interceptedMethod, Object[] args, Object returned, Throwable thrown) throws HookException {
        if (args.length != interceptedMethod.getParameterCount()) {
            throw new IllegalArgumentException("Number of provided arguments is " + args.length + ", but interceptedMethod expects " + interceptedMethod.getParameterCount() + " argument(s).");
        }
        for (Method method : findHookMethods(annotation, hookInstance.getClass(), interceptedMethod)) {
            try {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(hookInstance, addReturnedAndThrownArgs(method, args, returned, thrown));
            } catch (Exception e) {
                throw new HookException("Failed to call " + method.getName() + "() on " + hookInstance.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
        }
    }

    private static Object[] addReturnedAndThrownArgs(Method hookMethod, Object[] args, Object returned, Throwable thrown) {
        Annotation[][] annotations = hookMethod.getParameterAnnotations();
        List<Object> result = new ArrayList<>();
        int arg = 0;
        for (Annotation[] annotation : annotations) {
            if (Arrays.stream(annotation)
                    .map(Annotation::annotationType)
                    .anyMatch(Returned.class::equals)) {
                result.add(returned);
            } else if (Arrays.stream(annotation)
                    .map(Annotation::annotationType)
                    .anyMatch(Thrown.class::equals)) {
                result.add(thrown);
            } else {
                result.add(args[arg++]);
            }
        }
        return result.toArray();
    }
}

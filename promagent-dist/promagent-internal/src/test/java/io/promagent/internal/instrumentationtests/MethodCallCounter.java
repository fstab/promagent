package io.promagent.internal.instrumentationtests;

import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Track method calls, used in {@link ParameterTypesTest}
 */
public class MethodCallCounter {

    public static void reset() {
        captures.clear();
    }

    public static void observe(Object hook, String methodName, Object... args) {
        captures.add(new Capture(hook, methodName, args));
    }

    public static void assertNumCalls(int expectedNumberOfCalls, Class<?> hookClass, String hookMethod, Object... expectedArgs) {
        List<Capture> matching = captures.stream()
                .filter(c -> c.hook.getClass().equals(hookClass))
                .filter(c -> c.hookMethod.equals(hookMethod))
                .filter(c -> Arrays.equals(expectedArgs, c.args))
                .collect(Collectors.toList());
        Assertions.assertEquals(expectedNumberOfCalls, matching.size());
    }

    // special case for the varargsMixed test
    public static void assertNumCalls(int expectedNumberOfCalls, Class<?> hookClass, String hookMethod, String firstString, String... moreStrings) {
        List<Capture> matching = captures.stream()
                .filter(c -> c.hook.getClass().equals(hookClass))
                .filter(c -> c.hookMethod.equals(hookMethod))
                .filter(c -> c.args.length == 2)
                .filter(c -> Objects.equals(firstString, c.args[0]))
                .filter(c -> Arrays.equals(moreStrings, (String[]) c.args[1]))
                .collect(Collectors.toList());
        Assertions.assertEquals(expectedNumberOfCalls, matching.size());
    }

    public static void assertNumHookInstances(int expectedNumberOfInstances, Class<?> hookClass) {
        long actualNumberOfInstances = captures.stream()
                .filter(c -> c.hook.getClass().equals(hookClass))
                .map(c -> c.hook)
                .distinct()
                .count();
        Assertions.assertEquals(expectedNumberOfInstances, (int) actualNumberOfInstances);
    }

    private static class Capture {
        final Object hook;
        final String hookMethod;
        final Object[] args;

        Capture(Object hook, String hookMethod, Object[] args) {
            this.hook = hook;
            this.hookMethod = hookMethod;
            this.args = args;
        }
    }

    private static final List<Capture> captures = Collections.synchronizedList(new ArrayList<>());

    private MethodCallCounter() {}
}

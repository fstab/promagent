package io.promagent.internal.examples.classes;

import io.promagent.internal.Delegator;
import io.promagent.internal.HookInstance;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

/**
 * Calls to Delegator.before() and Delegator.after() are explicit in this class.
 * In the real agent, these calls would be performed implicitly through Byte code instrumentation.
 * <p/>
 * TODO: Instrument this class with byte buddy instead of explicitly calling Delegator.before() and Delegator.after(), because there are some cases with varargs where it is not obvious what byte buddy would do.
 *       note: use new ByteBuddy().subclass(InstrumentedClass.class)...load(InstrumentedClass.class.getClassLoader())
 */
public class InstrumentedClass {

    public static class Fruit {}

    public static class Orange extends Fruit {}

    public void noParam() {
        List<HookInstance> hooks = before();
        after(hooks);
    }

    public void primitiveTypes(byte b, short s, int i, long l, float f, double d, boolean x, char c) {
        List<HookInstance> hooks = before(b, s, i, l, f, d, x, c);
        after(hooks, b, s, i, l, f, d, x, c);
    }

    public void boxedTypes(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean x, Character c) {
        List<HookInstance> hooks = before(b, s, i, l, f, d, x, c);
        after(hooks, b, s, i, l, f, d, x, c);
    }

    public void objects(Object o, Fruit f, Orange x) {
        List<HookInstance> hooks = before(o, f, x);
        after(hooks, o, f, x);
    }

    public void primitiveArrays(byte[] b, short[] s, int[] i, long[] l, float[] f, double[] d, boolean[] x, char[] c) {
        List<HookInstance> hooks = before(b, s, i, l, f, d, x, c);
        after(hooks, b, s, i, l, f, d, x, c);
    }

    public void boxedArrays(Byte[] b, Short[] s, Integer[] i, Long[] l, Float[] f, Double[] d, Boolean[] x, Character[] c) {
        List<HookInstance> hooks = before(b, s, i, l, f, d, x, c);
        after(hooks, b, s, i, l, f, d, x, c);
    }

    public void objectArrays(Object[] o, Fruit[] f, Orange[] x) {
        List<HookInstance> hooks = before(o, f, x);
        after(hooks, o, f, x);
    }

    public void generics(List<Object> objectList, List<Fruit> fruitList, List<Orange> orangeList) {
        List<HookInstance> hooks = before(objectList, fruitList, orangeList);
        after(hooks, objectList, fruitList, orangeList);
    }

    public void varargsExplicit(Object... args) {
        Object[] runtimeArgs = new Object[]{args};
        List<HookInstance> hooks = before(runtimeArgs); // TODO: Verify if this mimics correctly the behavior of classes instrumented with byte buddy.
        after(hooks, runtimeArgs);
    }

    public void varargsImplicit(Object[] args) {
        Object[] runtimeArgs = new Object[]{args};
        List<HookInstance> hooks = before(runtimeArgs); // TODO: Verify if this mimics correctly the behavior of classes instrumented with byte buddy.
        after(hooks, runtimeArgs);
    }

    public void varargsMixed(String s, String... more) {
        List<HookInstance> hooks = before(s, more); // TODO: Verify if this mimics correctly the behavior of classes instrumented with byte buddy.
        after(hooks, s, more);
    }

    public void recursive(int nRecursiveCalls) {
        List<HookInstance> hooks = before(nRecursiveCalls);
        if (nRecursiveCalls > 0) {
            recursive(nRecursiveCalls - 1);
        }
        after(hooks, nRecursiveCalls);
    }

    private List<HookInstance> before(Object... args) {
        return Delegator.before(this, currentMethod(), args);
    }

    private void after(List<HookInstance> hooks, Object... args) {
        Delegator.after(hooks, currentMethod(), args);
    }

    // Returns the method that called the method that called currentMethod()
    private Method currentMethod() {
        String methodName = new Throwable().getStackTrace()[2].getMethodName();
        return Stream.of(this.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(methodName + ": No such method."));
    }
}

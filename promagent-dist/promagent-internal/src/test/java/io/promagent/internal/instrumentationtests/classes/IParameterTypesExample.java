package io.promagent.internal.instrumentationtests.classes;

import io.promagent.internal.instrumentationtests.Instrumentor;

import java.util.List;

/**
 * Example methods to be instrumented with hooks.
 * We use an interface to call these methods, because the actual implementation will come from a temporary
 * class loader defined in {@link Instrumentor}, which cannot be used directly.
 */
public interface IParameterTypesExample {

    // TODO: add tests for enums and lambdas

    void noParam();

    void primitiveTypes(byte b, short s, int i, long l, float f, double d, boolean x, char c);

    void boxedTypes(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean x, Character c);

    void objects(Object o, Fruit f, Fruit.Orange x);

    void primitiveArrays(byte[] b, short[] s, int[] i, long[] l, float[] f, double[] d, boolean[] x, char[] c);

    void boxedArrays(Byte[] b, Short[] s, Integer[] i, Long[] l, Float[] f, Double[] d, Boolean[] x, Character[] c);

    void objectArrays(Object[] o, Fruit[] f, Fruit.Orange[] x);

    void generics(List<Object> objectList, List<Fruit> fruitList, List<Fruit.Orange> orangeList);

    void varargsExplicit(Object... args);

    void varargsImplicit(Object[] args);

    void varargsMixed(String s, String... more);

    void recursive(int nRecursiveCalls);
}

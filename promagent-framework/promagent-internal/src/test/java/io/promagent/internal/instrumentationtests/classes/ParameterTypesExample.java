package io.promagent.internal.instrumentationtests.classes;

import io.promagent.internal.instrumentationtests.Instrumentor;

import java.util.List;

/**
 * These methods will be instrumented by {@link Instrumentor}.
 */
public class ParameterTypesExample implements IParameterTypesExample {

    @Override
    public void noParam() {}

    @Override
    public void primitiveTypes(byte b, short s, int i, long l, float f, double d, boolean x, char c) {}

    @Override
    public void boxedTypes(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean x, Character c) {}

    @Override
    public void objects(Object o, Fruit f, Fruit.Orange x) {}

    @Override
    public void primitiveArrays(byte[] b, short[] s, int[] i, long[] l, float[] f, double[] d, boolean[] x, char[] c) {}

    @Override
    public void boxedArrays(Byte[] b, Short[] s, Integer[] i, Long[] l, Float[] f, Double[] d, Boolean[] x, Character[] c) {}

    @Override
    public void objectArrays(Object[] o, Fruit[] f, Fruit.Orange[] x) {}

    @Override
    public void generics(List<Object> objectList, List<Fruit> fruitList, List<Fruit.Orange> orangeList) {}

    @Override
    public void varargsExplicit(Object... args) {}

    @Override
    public void varargsImplicit(Object[] args) {}

    @Override
    public void varargsMixed(String s, String... more) {}

    @Override
    public void recursive(int nRecursiveCalls) {
        if (nRecursiveCalls > 0) {
            recursive(nRecursiveCalls - 1);
        }
    }
}

package io.promagent.internal.instrumentationtests.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.instrumentationtests.MethodCallCounter;
import io.promagent.internal.instrumentationtests.classes.Fruit;
import io.promagent.internal.instrumentationtests.classes.Fruit.Orange;
import io.promagent.internal.instrumentationtests.classes.ParameterTypesExample;

import java.util.List;

/**
 * Instrument all methods in {@link ParameterTypesExample}.
 */
@Hook(instruments = "io.promagent.internal.instrumentationtests.classes.ParameterTypesExample")
public class ParameterTypesHook {

    public ParameterTypesHook(MetricsStore m) {}

    @Before(method = "noParam")
    public void before() {
        MethodCallCounter.observe(this, "before");
    }

    @After(method = "noParam")
    public void after() {
        MethodCallCounter.observe(this, "after");
    }

    @Before(method = {"primitiveTypes", "boxedTypes"}) // "boxedTypes" should be ignored because different method signature
    public void before(byte b, short s, int i, long l, float f, double d, boolean x, char c) {
        MethodCallCounter.observe(this, "before", b, s, i, l, f, d, x, c);
    }

    @After(method = {"primitiveTypes", "boxedTypes"}) // "boxedTypes" should be ignored because different method signature
    public void after(byte b, short s, int i, long l, float f, double d, boolean x, char c) {
        MethodCallCounter.observe(this, "after", b, s, i, l, f, d, x, c);
    }

    @Before(method = {"primitiveTypes", "boxedTypes"}) // "primitiveTypes" should be ignored because different method signature
    public void before(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean x, Character c) {
        MethodCallCounter.observe(this, "before", b, s, i, l, f, d, x, c);
    }

    @After(method = {"primitiveTypes", "boxedTypes"}) // "primitiveTypes" should be ignored because different method signature
    public void after(Byte b, Short s, Integer i, Long l, Float f, Double d, Boolean x, Character c) {
        MethodCallCounter.observe(this, "after", b, s, i, l, f, d, x, c);
    }

    @Before(method = "objects")
    public void before(Object o, Fruit f, Orange x) {
        MethodCallCounter.observe(this, "before", o, f, x);
    }

    @After(method = "objects")
    public void after(Object o, Fruit f, Orange x) {
        MethodCallCounter.observe(this, "after", o, f, x);
    }

    @Before(method = "objects")
    public void before2(Object o, Fruit f, Orange x) {
        MethodCallCounter.observe(this, "before2", o, f, x);
    }

    @After(method = "objects")
    public void after2(Object o, Fruit f, Orange x) {
        MethodCallCounter.observe(this, "after2", o, f, x);
    }

    @Before(method = "objects") // should not be called, because method signature differs
    public void beforeTooLoose(Object o, Fruit f, Fruit x) {
        MethodCallCounter.observe(this, "beforeTooLoose", o, f, x);
    }

    @Before(method = "objects") // should not be called, because method signature differs
    public void beforeTooStrict(Object o, Orange f, Orange x) {
        MethodCallCounter.observe(this, "beforeTooStrict", o, f, x);
    }

    @Before(method = "objects")
    @After(method = "objects")
    public void beforeAndAfter(Object o, Fruit f, Orange x) {
        MethodCallCounter.observe(this, "beforeAndAfter", o, f, x);
    }

    @Before(method = "primitiveArrays")
    public void before(byte[] b, short[] s, int[] i, long[] l, float[] f, double[] d, boolean[] x, char[] c) {
        MethodCallCounter.observe(this, "before", b, s, i, l, f, d, x, c);
    }

    @After(method = "primitiveArrays")
    public void after(byte[] b, short[] s, int[] i, long[] l, float[] f, double[] d, boolean[] x, char[] c) {
        MethodCallCounter.observe(this, "after", b, s, i, l, f, d, x, c);
    }

    @Before(method = "boxedArrays")
    public void before(Byte[] b, Short[] s, Integer[] i, Long[] l, Float[] f, Double[] d, Boolean[] x, Character[] c) {
        MethodCallCounter.observe(this, "before", b, s, i, l, f, d, x, c);
    }

    @After(method = "boxedArrays")
    public void after(Byte[] b, Short[] s, Integer[] i, Long[] l, Float[] f, Double[] d, Boolean[] x, Character[] c) {
        MethodCallCounter.observe(this, "after", b, s, i, l, f, d, x, c);
    }

    @Before(method = "objectArrays")
    public void before(Object[] o, Fruit[] f, Orange[] x) {
        MethodCallCounter.observe(this, "before", o, f, x);
    }

    @After(method = "objectArrays")
    public void after(Object[] o, Fruit[] f, Orange[] x) {
        MethodCallCounter.observe(this, "after", o, f, x);
    }

    @Before(method = "generics")
    public void before(List<Object> o, List<Fruit> f, List<Orange> x) {
        MethodCallCounter.observe(this, "before", o, f, x);
    }

    @After(method = "generics")
    public void after(List<Object> o, List<Fruit> f, List<Orange> x) {
        MethodCallCounter.observe(this, "after", o, f, x);
    }

    @Before(method = "varargsExplicit")
    public void before(Object... args) {
        MethodCallCounter.observe(this, "before", args);
    }

    @After(method = "varargsExplicit")
    public void after(Object... args) {
        MethodCallCounter.observe(this, "after", args);
    }

    @Before(method = "varargsImplicit")
    public void before2(Object[] args) {
        MethodCallCounter.observe(this, "before", args);
    }

    @After(method = "varargsImplicit")
    public void after2(Object[] args) {
        MethodCallCounter.observe(this, "after", args);
    }

    @Before(method = "varargsMixed")
    public void before(String s, String... more) {
        MethodCallCounter.observe(this, "before", s, more);
    }

    @After(method = "varargsMixed")
    public void after(String s, String... more) {
        MethodCallCounter.observe(this, "after", s, more);
    }

    @Before(method = "recursive")
    public void before(int n) {
        MethodCallCounter.observe(this, "before", n);
    }

    @After(method = "recursive")
    public void after(int n) {
        MethodCallCounter.observe(this, "after", n);
    }
}

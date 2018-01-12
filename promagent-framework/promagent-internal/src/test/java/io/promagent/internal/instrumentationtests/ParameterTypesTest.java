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

package io.promagent.internal.instrumentationtests;

import io.promagent.agent.ClassLoaderCache;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.Delegator;
import io.promagent.internal.HookMetadata;
import io.promagent.internal.instrumentationtests.classes.Fruit;
import io.promagent.internal.instrumentationtests.classes.IParameterTypesExample;
import io.promagent.internal.instrumentationtests.classes.ParameterTypesExample;
import io.promagent.internal.instrumentationtests.hooks.OnlyAfterHook;
import io.promagent.internal.instrumentationtests.hooks.OnlyBeforeHook;
import io.promagent.internal.instrumentationtests.hooks.ParameterTypesHook;
import io.promagent.internal.instrumentationtests.hooks.TwoHooks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class ParameterTypesTest {

    private IParameterTypesExample parameterTypesExample;

    private final byte b = (byte) 0x23;
    private final short s = (short) 42;
    private final int i = 7;
    private final long l = 3L;
    private final float f = 0.4f;
    private final double d = 0.5d;
    private final boolean x = true;
    private final char c = 'a';

    private final Fruit.Orange obj1 = new Fruit.Orange();
    private final Fruit.Orange obj2 = new Fruit.Orange();
    private final Fruit.Orange obj3 = new Fruit.Orange();

    @BeforeEach
    void setUp() throws Exception {
        SortedSet<HookMetadata> hookMetadata = Util.loadHookMetadata(
                ParameterTypesHook.class,
                TwoHooks.HookOne.class,
                TwoHooks.HookTwo.class,
                OnlyBeforeHook.class,
                OnlyAfterHook.class
        );
        ClassLoaderCache classLoaderCache = Util.mockClassLoaderCache();
        parameterTypesExample = Instrumentor.instrument(ParameterTypesExample.class, hookMetadata);
        MetricsStore metricsStore = Util.mockMetricsStore();
        Delegator.init(hookMetadata, metricsStore, classLoaderCache);
        MethodCallCounter.reset();
    }

    @Test
    void testNullArgs() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.objects(null, obj2, null);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", null, obj2, null);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", null, obj2, null);
        }
    }

    @Test
    void testBeforeOrAfterMissing() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.objects(obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, OnlyBeforeHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, OnlyAfterHook.class, "after", obj1, obj2, obj3);
        }
    }

    @Test
    void testNoArg() {
        for (int i=1; i<=2; i++) {
            parameterTypesExample.noParam();
            MethodCallCounter.assertNumCalls(i, ParameterTypesHook.class, "before");
            MethodCallCounter.assertNumCalls(i, ParameterTypesHook.class, "after");
        }
    }

    @Test
    void testPrimitiveTypes() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.primitiveTypes(b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", b, s, i, l, f, d, x, c);
        }
    }

    @Test
    void testBoxedTypes() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.boxedTypes(b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", b, s, i, l, f, d, x, c);
        }
    }

    @Test
    void testObjects() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.objects(obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before2", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after2", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(0, ParameterTypesHook.class, "beforeTooLoose", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(0, ParameterTypesHook.class, "beforeTooStrict", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(2*n, ParameterTypesHook.class, "beforeAndAfter", obj1, obj2, obj3);
        }
    }

    @Test
    void testTwoHooks() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.objects(obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TwoHooks.HookOne.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TwoHooks.HookTwo.class, "before", obj1, obj2, obj3);
        }
    }

    @Test
    void testPrimitiveArrays() {
        byte[] byteArray = new byte[] {b};
        short[] shortArray = new short[] {s};
        int[] intArray = new int[] {i};
        long[] longArray = new long[] {l};
        float[] floatArray = new float[] {f};
        double[] doubleArray = new double[] {d};
        boolean[] booleanArray = new boolean[] {x};
        char[] charArray = new char[] {c};
        for (int n=1; n<=2; n++) {
            parameterTypesExample.primitiveArrays(byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
        }
    }

    @Test
    void testBoxedArrays() {
        Byte[] byteArray = new Byte[] {b};
        Short[] shortArray = new Short[] {s};
        Integer[] intArray = new Integer[] {i};
        Long[] longArray = new Long[] {l};
        Float[] floatArray = new Float[] {f};
        Double[] doubleArray = new Double[] {d};
        Boolean[] booleanArray = new Boolean[] {x};
        Character[] charArray = new Character[] {c};
        for (int n=1; n<=2; n++) {
            parameterTypesExample.boxedArrays(byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
        }
    }

    @Test
    void testObjectArrays() {
        Object[] arr1 = new Object[] {obj1, obj2};
        Fruit[] arr2 = new Fruit[] {obj3};
        Fruit.Orange[] arr3 = new Fruit.Orange[0];
        for (int n=1; n<=2; n++) {
            parameterTypesExample.objectArrays(arr1, arr2, arr3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", arr1, arr2, arr3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", arr1, arr2, arr3);
        }
    }

    @Test
    void testGenerics() {
        List<Object> list1 = Arrays.asList(obj1, obj2);
        List<Fruit> list2 = Collections.singletonList(obj3);
        List<Fruit.Orange> list3 = new ArrayList<>();
        for (int n=1; n<=2; n++) {
            parameterTypesExample.generics(list1, list2, list3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", list1, list2, list3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", list1, list2, list3);
        }
    }

    @Test
    void testVarargsExplicit() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.varargsExplicit(obj1, obj2, obj3);
            parameterTypesExample.varargsExplicit();
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before");
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after");
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", obj1, obj2, obj3);
        }
    }

    @Test
    void testVarargsImplicit() {
        Object[] arr1 = new Object[] {obj1};
        Object[] arr2 = new Object[] {};
        Object[] arr3 = null;
        for (int n=1; n<=2; n++) {
            parameterTypesExample.varargsImplicit(arr1);
            parameterTypesExample.varargsImplicit(arr2);
            parameterTypesExample.varargsImplicit(arr3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", arr1);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", arr1);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", arr2);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", arr2);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", arr3);
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", arr3);
        }
    }

    @Test
    void testVarargsMixed() {
        for (int n=1; n<=2; n++) {
            parameterTypesExample.varargsMixed("hello");
            parameterTypesExample.varargsMixed("hello", "world");
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", "hello");
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", "hello");
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "before", "hello", "world");
            MethodCallCounter.assertNumCalls(n, ParameterTypesHook.class, "after", "hello", "world");
        }
    }
}

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
import io.promagent.internal.examples.classes.InstrumentedClass;
import io.promagent.internal.examples.classes.InstrumentedClass.Fruit;
import io.promagent.internal.examples.classes.InstrumentedClass.Orange;
import io.promagent.internal.examples.hooks.OnlyAfterHook;
import io.promagent.internal.examples.hooks.OnlyBeforeHook;
import io.promagent.internal.examples.hooks.TestHook;
import io.promagent.internal.examples.hooks.TwoHooks;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

class DelegatorTest {

    private final InstrumentedClass instrumentedInstance = new InstrumentedClass();

    private final byte b = (byte) 0x23;
    private final short s = (short) 42;
    private final int i = 7;
    private final long l = 3L;
    private final float f = 0.4f;
    private final double d = 0.5d;
    private final boolean x = true;
    private final char c = 'a';

    private final Orange obj1 = new Orange();
    private final Orange obj2 = new Orange();
    private final Orange obj3 = new Orange();

    @BeforeEach
    void setUp() throws IOException, ClassNotFoundException {
        SortedSet<HookMetadata> hookMetadata = loadHookMetadata(
                TestHook.class,
                TwoHooks.HookOne.class,
                TwoHooks.HookTwo.class,
                OnlyBeforeHook.class,
                OnlyAfterHook.class
        );
        ClassLoaderCache classLoaderCache = mockClassLoaderCache();
        Delegator.init(hookMetadata, CollectorRegistry.defaultRegistry, classLoaderCache);
        MethodCallCounter.reset();
    }

    @Test
    void testNullArgs() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.objects(null, obj2, null);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", null, obj2, null);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", null, obj2, null);
        }
    }

    @Test
    void testBeforeOrAfterMissing() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.objects(obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, OnlyBeforeHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, OnlyAfterHook.class, "after", obj1, obj2, obj3);
        }
    }

    @Test
    void testNoArg() {
        for (int i=1; i<=2; i++) {
            instrumentedInstance.noParam();
            MethodCallCounter.assertNumCalls(i, TestHook.class, "before");
            MethodCallCounter.assertNumCalls(i, TestHook.class, "after");
        }
    }

    @Test
    void testPrimitiveTypes() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.primitiveTypes(b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", b, s, i, l, f, d, x, c);
        }
    }

    @Test
    void testBoxedTypes() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.boxedTypes(b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", b, s, i, l, f, d, x, c);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", b, s, i, l, f, d, x, c);
        }
    }

    @Test
    void testObjects() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.objects(obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before2", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after2", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(0, TestHook.class, "beforeTooLoose", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(0, TestHook.class, "beforeTooStrict", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(2*n, TestHook.class, "beforeAndAfter", obj1, obj2, obj3);
        }
    }

    @Test
    void testTwoHooks() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.objects(obj1, obj2, obj3);
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
            instrumentedInstance.primitiveArrays(byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
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
            instrumentedInstance.boxedArrays(byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", byteArray, shortArray, intArray, longArray, floatArray, doubleArray, booleanArray, charArray);
        }
    }

    @Test
    void testObjectArrays() {
        Object[] arr1 = new Object[] {obj1, obj2};
        Fruit[] arr2 = new Fruit[] {obj3};
        Orange[] arr3 = new Orange[0];
        for (int n=1; n<=2; n++) {
            instrumentedInstance.objectArrays(arr1, arr2, arr3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", arr1, arr2, arr3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", arr1, arr2, arr3);
        }
    }

    @Test
    void testGenerics() {
        List<Object> list1 = Arrays.asList(obj1, obj2);
        List<Fruit> list2 = Collections.singletonList(obj3);
        List<Orange> list3 = new ArrayList<>();
        for (int n=1; n<=2; n++) {
            instrumentedInstance.generics(list1, list2, list3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", list1, list2, list3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", list1, list2, list3);
        }
    }

    @Test
    void testVarargsExplicit() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.varargsExplicit(obj1, obj2, obj3);
            instrumentedInstance.varargsExplicit();
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before");
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after");
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", obj1, obj2, obj3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", obj1, obj2, obj3);
        }
    }

    @Test
    void testVarargsImplicit() {
        Object[] arr1 = new Object[] {obj1};
        Object[] arr2 = new Object[] {};
        Object[] arr3 = null;
        for (int n=1; n<=2; n++) {
            instrumentedInstance.varargsImplicit(arr1);
            instrumentedInstance.varargsImplicit(arr2);
            instrumentedInstance.varargsImplicit(arr3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", arr1);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", arr1);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", arr2);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", arr2);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", arr3);
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", arr3);
        }
    }

    @Test
    void testVarargsMixed() {
        for (int n=1; n<=2; n++) {
            instrumentedInstance.varargsMixed("hello");
            instrumentedInstance.varargsMixed("hello", "world");
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", "hello");
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", "hello");
            MethodCallCounter.assertNumCalls(n, TestHook.class, "before", "hello", "world");
            MethodCallCounter.assertNumCalls(n, TestHook.class, "after", "hello", "world");
        }
    }

    // test that the same hook instance is re-used for recursive calls,
    // but it is not re-used across different calls or different threads.
    @Test
    void testLivecycle() throws Exception {
        List<RecursiveRunConfig> runConfigs = Arrays.asList(
                new RecursiveRunConfig(3, 4),
                new RecursiveRunConfig(2, 3),
                new RecursiveRunConfig(2, 4)
        );
        ExecutorService executor = Executors.newFixedThreadPool(runConfigs.size());
        for (RecursiveRunConfig runConfig : runConfigs) {
            executor.submit(() -> {
                for (int i=0; i<runConfig.nRuns; i++) {
                    instrumentedInstance.recursive(runConfig.nRecursiveCalls);
                    Thread.sleep(100); // sleep a bit to make sure all threads are really running in parallel.
                }
                return null;
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Maximum argument for the InstrumentedClass.recursive() method.
        int maxNRecursiveCalls = runConfigs.stream()
                .mapToInt(cfg -> cfg.nRecursiveCalls)
                .max()
                .getAsInt();

        for (int nRecursiveCalls = maxNRecursiveCalls; nRecursiveCalls >= 0; nRecursiveCalls--) {
            final int n = nRecursiveCalls; // copy to final variable so it can be used in lambda
            int expectedNumCalls = runConfigs.stream()
                    .filter(cfg -> cfg.nRecursiveCalls >= n)
                    .mapToInt(cfg -> cfg.nRuns)
                    .sum();
            MethodCallCounter.assertNumCalls(expectedNumCalls, TestHook.class, "before", nRecursiveCalls);
        }
        MethodCallCounter.assertNumHookInstances(runConfigs.stream().mapToInt(cfg -> cfg.nRuns).sum(), TestHook.class);
    }

    private static class RecursiveRunConfig {
        final int nRuns; // number of runs within the same thread
        final int nRecursiveCalls; // number of recursive calls within each run

        private RecursiveRunConfig(int nRuns, int nRecursiveCalls) {
            this.nRuns = nRuns;
            this.nRecursiveCalls = nRecursiveCalls;
        }
    }

    private SortedSet<HookMetadata> loadHookMetadata(Class<?>... hooks) throws IOException, ClassNotFoundException {
        List<Path> classesDir = new ArrayList<>();
        classesDir.add(Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()));
        HookMetadataParser parser = new HookMetadataParser(classesDir);
        return parser.parse(className -> Stream.of(hooks).anyMatch(hookClass -> hookClass.getName().equals(className)));
    }

    private ClassLoaderCache mockClassLoaderCache() {
        ClassLoaderCache mockedClassLoaderCache = Mockito.mock(ClassLoaderCache.class);
        Mockito.when(mockedClassLoaderCache.currentClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        return mockedClassLoaderCache;
    }
}

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
import io.promagent.internal.instrumentationtests.hooks.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class LifecycleTest {

    private IParameterTypesExample parameterTypesExample;

    @BeforeEach
    void setUp() throws Exception {
        SortedSet<HookMetadata> hookMetadata = Util.loadHookMetadata(
                LifecycleHookSkipTrue.class,
                LifecycleHookSkipFalse.class
        );
        ClassLoaderCache classLoaderCache = Util.mockClassLoaderCache();
        parameterTypesExample = Instrumentor.instrument(ParameterTypesExample.class, hookMetadata);
        MetricsStore metricsStore = Util.mockMetricsStore();
        Delegator.init(hookMetadata, metricsStore, classLoaderCache);
        MethodCallCounter.reset();
    }

    /**
     * Expected behavior:
     * <ul>
     * <li>For LifecycleHookSkipTrue, only the first call to parameterTypesExample.recursive() should be instrumented,
     *     all recursive calls should not be instrumented.
     * <li>For LifecycleHookSkipFalse, all calls should be instrumented, but recursive calls are executed with the existing Hook instance,
     *     while non-recursive calls are executed with a new instance.
     * </ul>
     */
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
                    parameterTypesExample.recursive(runConfig.nRecursiveCalls);
                    Thread.sleep(100); // sleep a bit to make sure all threads are really running in parallel.
                }
                return null;
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Maximum argument for the ParameterTypesExample.recursive() method.
        int maxNRecursiveCalls = runConfigs.stream()
                .mapToInt(cfg -> cfg.nRecursiveCalls)
                .max()
                .getAsInt();

        for (int nRecursiveCalls = maxNRecursiveCalls; nRecursiveCalls >= 0; nRecursiveCalls--) {
            final int n = nRecursiveCalls; // copy to final variable so it can be used in lambda
            int expectedNumCallsSkipFalse = runConfigs.stream()
                    .filter(cfg -> cfg.nRecursiveCalls >= n) // recursive calls executed: >= n
                    .mapToInt(cfg -> cfg.nRuns)
                    .sum();
            int expectedNumCallsSkipTrue = runConfigs.stream()
                    .filter(cfg -> cfg.nRecursiveCalls == n) // recursive calls skipped: == n
                    .mapToInt(cfg -> cfg.nRuns)
                    .sum();
            MethodCallCounter.assertNumCalls(expectedNumCallsSkipFalse, LifecycleHookSkipFalse.class, "before", nRecursiveCalls);
            MethodCallCounter.assertNumCalls(expectedNumCallsSkipTrue, LifecycleHookSkipTrue.class, "before", nRecursiveCalls);
        }
        // Number of instances should be the same because recursive calls are executed with an existing instance.
        MethodCallCounter.assertNumHookInstances(runConfigs.stream().mapToInt(cfg -> cfg.nRuns).sum(), LifecycleHookSkipFalse.class);
        MethodCallCounter.assertNumHookInstances(runConfigs.stream().mapToInt(cfg -> cfg.nRuns).sum(), LifecycleHookSkipTrue.class);
    }

    private static class RecursiveRunConfig {
        final int nRuns; // number of runs within the same thread
        final int nRecursiveCalls; // number of recursive calls within each run

        private RecursiveRunConfig(int nRuns, int nRecursiveCalls) {
            this.nRuns = nRuns;
            this.nRecursiveCalls = nRecursiveCalls;
        }
    }
}

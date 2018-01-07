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
import io.promagent.internal.instrumentationtests.classes.IReturnedAndThrownExample;
import io.promagent.internal.instrumentationtests.classes.ReturnedAndThrownExample;
import io.promagent.internal.instrumentationtests.hooks.ReturnedAndThrownHook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SortedSet;

class ReturnedAndThrownTest {

    private IReturnedAndThrownExample returnedAndThrownExample;
    private final Fruit.Orange orange = new Fruit.Orange();

    @BeforeEach
    void setUp() throws Exception {
        SortedSet<HookMetadata> hookMetadata = Util.loadHookMetadata(
                ReturnedAndThrownHook.class
        );
        ClassLoaderCache classLoaderCache = Util.mockClassLoaderCache();
        returnedAndThrownExample = Instrumentor.instrument(ReturnedAndThrownExample.class, hookMetadata);
        MetricsStore metricsStore = Util.mockMetricsStore();
        Delegator.init(hookMetadata, metricsStore, classLoaderCache);
        MethodCallCounter.reset();
    }

    @Test
    void testVoid() {
        for (int n=1; n<=2; n++) {
            returnedAndThrownExample.returnVoid(orange);
            MethodCallCounter.assertNumCalls(n, ReturnedAndThrownHook.class, "beforeVoid", orange);
            MethodCallCounter.assertNumCalls(n, ReturnedAndThrownHook.class, "afterVoid", orange);
        }
    }

    @Test
    void testPrimitive() {
        for (int n=1; n<=2; n++) {
            int ret = returnedAndThrownExample.returnPrimitive(orange);
            MethodCallCounter.assertNumCalls(n, ReturnedAndThrownHook.class, "beforePrimitive", orange);
            MethodCallCounter.assertNumCalls(n, ReturnedAndThrownHook.class, "afterPrimitive", orange, ret);
        }
    }

    @Test
    void testObject() {
        Fruit ret1 = returnedAndThrownExample.returnObject();
        Fruit ret2 = returnedAndThrownExample.returnObject();
        MethodCallCounter.assertNumCalls(2, ReturnedAndThrownHook.class, "beforeObject");
        MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "afterObject", ret1);
        MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "afterObject", ret2);
    }

    @Test
    void testArray() {
        int[] params = {1, 2, 3};
        int[] ret1 = returnedAndThrownExample.returnArray(params);
        int[] ret2 = returnedAndThrownExample.returnArray(params);
        MethodCallCounter.assertNumCalls(2, ReturnedAndThrownHook.class, "beforeArray", new Object[]{params});
        MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "afterArray", ret1, params);
        MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "afterArray", ret2, params);
    }

    @Test
    void testGenerics() {
        for (int n=1; n<=2; n++) {
            List<Fruit.Orange> ret = returnedAndThrownExample.returnGenerics(orange);
            MethodCallCounter.assertNumCalls(n, ReturnedAndThrownHook.class, "beforeGenerics", orange);
            // The following works because List.equals() compares the elements within the lists, not the lists itself.
            // Therefore both return values are considered equal, because they are both lists containing the same element.
            MethodCallCounter.assertNumCalls(n, ReturnedAndThrownHook.class, "afterGenerics", orange, ret);
        }
    }

    @Test
    void testRuntimeException() {
        try {
            returnedAndThrownExample.throwsRuntimeException(7, orange);
        } catch (Exception e) {
            MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "beforeThrowsRuntimeException", 7, orange);
            MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "afterThrowsRuntimeException", 7, orange, null, e);
            return; // success
        }
        Assertions.fail("exception not thrown.");
    }

    @Test
    void testCheckedException() {
        try {
            returnedAndThrownExample.throwsCheckedException();
        } catch (Exception e) {
            MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "beforeThrowsCheckedException");
            MethodCallCounter.assertNumCalls(1, ReturnedAndThrownHook.class, "afterThrowsCheckedException", 0, e);
            return; // success
        }
        Assertions.fail("exception not thrown.");
    }
}

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

import io.promagent.agent.Delegator;

import java.lang.reflect.Method;
import java.util.List;

import static net.bytebuddy.asm.Advice.*;

public class PromagentAdvice {

    @OnMethodEnter
    public static List<Object> before(
            @This Object that,
            @Origin Method method,
            @AllArguments Object[] args
    ) {
        try {
            List<Object> hooks = Delegator.createHookInstances(that, method);
            for (Object hook : hooks) {
                Delegator.invokeBefore(hook, method, args);
            }
            return hooks;
        } catch (Exception e) {
            System.err.println("Error executing Prometheus hook on " + that.getClass().getSimpleName());
            e.printStackTrace();
            return null;
        }
    }

    @OnMethodExit
    public static void after(
            @Enter List<Object> hooks,
            @This Object that,
            @Origin Method method,
            @AllArguments Object[] args
    ) {
        try {
            if (hooks != null) {
                for (Object hook : hooks) {
                    Delegator.invokeAfter(hook, method, args);
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing Prometheus hook on " + that.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
}

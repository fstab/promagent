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
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.List;

import static net.bytebuddy.asm.Advice.*;

public class PromagentAdvice {

    // TODO:
    // Should we move this class into it's own maven module
    // to make clear that it cannot reference other classes from promagent-internal?

    @OnMethodEnter
    @SuppressWarnings("unchecked")
    public static List<Object> before(
            @This(optional = true) Object that,
            @Origin Method method,
            @AllArguments Object[] args
    ) {
        // that is null when instrumenting static methods.
        Class<?> clazz = that != null ? that.getClass() : method.getDeclaringClass();
        try {
            // The following code is equivalent to:
            //     return Delegator.before(that, method, args);
            // However, the Delegator class will not be available in the context of the instrumented method,
            // so we must use our agent class loader to load the Delegator class and do the call via reflection.
            Class<?> delegator = ClassLoaderCache.getInstance().currentClassLoader().loadClass("io.promagent.internal.Delegator");
            Method beforeMethod = delegator.getMethod("before", Class.class, Method.class, Object[].class);
            return (List<Object>) beforeMethod.invoke(null, clazz, method, args);
        } catch (Exception e) {
            System.err.println("Error executing Prometheus hook on " + clazz.getSimpleName());
            e.printStackTrace();
            return null;
        }
    }

    @OnMethodExit(onThrowable = Throwable.class)
    public static void after(
            @Enter List<Object> hooks,
            @This(optional = true) Object that,
            @Origin Method method,
            @AllArguments Object[] args,
            @Return(typing = Assigner.Typing.DYNAMIC) Object returned, // support void == null and int == Integer
            @Thrown Throwable thrown
    ) {
        try {
            // The following code is equivalent to:
            //     Delegator.after(hooks, method, args);
            // However, the Delegator class will not be available in the context of the instrumented method,
            // so we must use our agent class loader to load the Delegator class and do the call via reflection.
            Class<?> delegator = ClassLoaderCache.getInstance().currentClassLoader().loadClass("io.promagent.internal.Delegator");
            Method afterMethod = delegator.getMethod("after", List.class, Method.class, Object[].class, Object.class, Throwable.class);
            afterMethod.invoke(null, hooks, method, args, returned, thrown);
        } catch (Exception e) {
            Class<?> clazz = that != null ? that.getClass() : method.getDeclaringClass();
            System.err.println("Error executing Prometheus hook on " + clazz.getSimpleName());
            e.printStackTrace();
        }
    }
}

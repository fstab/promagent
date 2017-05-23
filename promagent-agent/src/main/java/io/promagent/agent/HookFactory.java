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

package io.promagent.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Create an instance of a class annotated with @Hook and wrap it into a {@link Hook} instance.
 */
public class HookFactory {

    private static final String HOOK_PACKAGE = "io.promagent.internal.hooks.";

    public static final String[] hooks = {
            HOOK_PACKAGE + "ServletHook",
            HOOK_PACKAGE + "JdbcHook"
    };

    public static List<Hook> createHooks(Class<?> instrumentedInterfaces) {
        try {
            List<Hook> result = new ArrayList<>();
            ClassLoaderCache classLoaderCache = ClassLoaderCache.getInstance();
            Class contextClass = classLoaderCache.loadClass(HOOK_PACKAGE + "Context");
            for (String hook : hooks) {
                Class<?> hookClass = classLoaderCache.loadClass(hook);
                Class<?>[] interfaces = hookClass.getAnnotation(io.promagent.agent.annotations.Hook.class).instruments();
                for (Class<?> ifc : interfaces) {
                    if (ifc.isAssignableFrom(instrumentedInterfaces)) {
                        Object context = contextClass.newInstance();
                        result.add(new Hook(hookClass.getConstructor(contextClass).newInstance(context)));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Create an instance of a class annotated with @Hook and wrap it into a {@link Hook} instance.
 */
public class HookFactory {

    private static final String HOOK_PACKAGE = "io.promagent.hooks.";

    public static final String[] hooks = {
            HOOK_PACKAGE + "ServletHook",
            HOOK_PACKAGE + "JdbcHook"
    };

    public static List<Hook> createHooks(Class<?> classToBeInstrumented) {
        ClassLoaderCache classLoaders = ClassLoaderCache.getInstance();
        try {
            Set<String> classesAndInterfaces = getAllSuperClassesAndInterfaces(classToBeInstrumented);
            List<Hook> result = new ArrayList<>();
            for (String hook : hooks) {
                Class<?> hookClass = classLoaders.currentClassLoader().loadClass(hook);
                for (String instruments : hookClass.getAnnotation(io.promagent.agent.annotations.Hook.class).instruments()) {
                    if (classesAndInterfaces.contains(instruments)) {
                        result.add(new Hook(hookClass.newInstance()));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> getAllSuperClassesAndInterfaces(Class<?> clazz) {
        Set<String> result = new HashSet<>();
        addAllSuperClassesAndInterfaces(clazz, result);
        return result;
    }

    private static void addAllSuperClassesAndInterfaces(Class<?> clazz, Set<String> result) {
        if (clazz == null) {
            return;
        }
        if (result.contains(clazz.getName())) {
            return;
        }
        result.add(clazz.getName());
        for (Class<?> ifc : clazz.getInterfaces()) {
            addAllSuperClassesAndInterfaces(ifc, result);
        }
        addAllSuperClassesAndInterfaces(clazz.getSuperclass(), result);
    }
}

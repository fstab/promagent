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

import java.lang.instrument.Instrumentation;

/**
 * We want as little dependencies as possible on the system class loader,
 * so the actual agent is loaded in its own class loader and this agent delegates to it.
 */
public class Promagent {

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        ClassLoader agentClassLoader = ClassLoaderCache.getInstance().currentClassLoader();
        Class<?> agentClass = agentClassLoader.loadClass("io.promagent.internal.Promagent");
        agentClass.getMethod("premain", String.class, Instrumentation.class).invoke(null, agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        premain(agentArgs, inst);
    }
}

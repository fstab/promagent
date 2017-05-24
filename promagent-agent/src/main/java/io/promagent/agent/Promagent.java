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
 * so the agent just delegates to the real agent which is loaded using our own class loader.
 */
public class Promagent {
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        Object realAgent = ClassLoaderCache.getInstance().loadClass("io.promagent.internal.Promagent").newInstance();
        realAgent.getClass().getMethod("premain", String.class, Instrumentation.class).invoke(realAgent, agentArgs, inst);
    }
}

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

import io.promagent.agent.HookFactory;
import io.promagent.agent.annotations.Before;
import io.promagent.agent.annotations.Hook;
import io.promagent.internal.metrics.Exporter;
import io.promagent.internal.metrics.PromagentCollectorRegistry;
import io.prometheus.client.CollectorRegistry;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class Promagent {

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            PromagentCollectorRegistry registry = new PromagentCollectorRegistry();
            ManagementFactory.getPlatformMBeanServer().registerMBean(new Exporter(registry), new ObjectName("io.promagent:type=exporter"));
            Map<String, String> args = parseCmdline(agentArgs);
            if (args.containsKey("port")) {
                BuiltInServer.run(args.get("host"), args.get("port"), registry);
            }
            AgentBuilder agentBuilder = new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE);
            for (String hook : HookFactory.hooks) {
                Class<?> hookClass = initHookClass(hook, registry);
                agentBuilder = applyHook(agentBuilder, hookClass);
            }
            agentBuilder.installOn(inst);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Load Hook class and call static method init(registry)
     */
    private static Class<?> initHookClass(String className, CollectorRegistry registry) {
        try {
            Class<?> hookClass = Promagent.class.getClassLoader().loadClass(className);
            hookClass.getMethod("init", CollectorRegistry.class).invoke(null, registry);
            return hookClass;
        } catch (Exception e) {
            throw new RuntimeException("Error initializing " + className + ": " + e.getMessage(), e);
        }
    }

    /**
     * For each @Before method in the hook class add a corresponding ElementMatcher to the agentBuilder
     */
    private static AgentBuilder applyHook(AgentBuilder agentBuilder, Class<?> hookClass) {
        if (!hookClass.isAnnotationPresent(Hook.class)) {
            System.err.println("Skipping " + hookClass.getSimpleName() + ", because it has no @" + Hook.class.getSimpleName() + " annotation.");
        } else {
            for (Class<?> instrumentedInterface : hookClass.getAnnotation(Hook.class).instruments()) {
                ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers.none();
                for (Method method : hookClass.getMethods()) {
                    if (method.isAnnotationPresent(Before.class)) {
                        for (String methodName : method.getAnnotation(Before.class).method()) {
                            methodMatcher = methodMatcher
                                    .or(ElementMatchers
                                            .named(methodName)
                                            .and(not(isAbstract()))
                                            .and(isPublic())
                                            .and(takesArguments(method.getParameterTypes()))
                                    );
                        }
                    }
                }
                agentBuilder = agentBuilder
                        .type(ElementMatchers.hasSuperType(named(instrumentedInterface.getName())))
                        .transform(new AgentBuilder.Transformer.ForAdvice()
                                .include(Promagent.class.getClassLoader())
                                .advice(methodMatcher, PromagentAdvice.class.getName()));
            }
        }
        return agentBuilder;
    }

    /**
     * Parse a comma-separated list of key/value pairs. Example: "host=localhost,port=9300"
     */
    private static Map<String, String> parseCmdline(String agentArgs) {
        Map<String, String> result = new HashMap<>();
        if (agentArgs != null) {
            for (String keyValueString : agentArgs.split(",")) {
                String[] keyValue = keyValueString.split("=");
                if (keyValue.length != 2) {
                    throw new RuntimeException("Failed to parse command line arguments '" + agentArgs + "'. " +
                            "Expecting a comma-separated list of key/value pairs, as for example 'host=localhost,port=9300'.");
                }
                result.put(keyValue[0], keyValue[1]);
            }
        }
        return result;
    }
}

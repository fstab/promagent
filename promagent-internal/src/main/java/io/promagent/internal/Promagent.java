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
import io.promagent.agent.HookFactory;
import io.promagent.internal.jmx.Exporter;
import io.promagent.internal.jmx.PromagentCollectorRegistry;
import io.prometheus.client.CollectorRegistry;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
            List<Path> hookJars = ClassLoaderCache.getInstance().getPerDeploymentJars();
            HookConfig hookConfig = HookConfig.from(hookJars).parse(HookFactory.hooks);
            agentBuilder = applyHooks(agentBuilder, hookConfig);
            agentBuilder.installOn(inst);
            initMetrics(registry);
            System.out.println("Promagent instrumenting the following classes or interfaces:");
            System.out.println(hookConfig);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void initMetrics(CollectorRegistry registry) {
        String className = "io.promagent.metrics.Metrics";
        try {
            Class<?> metricsClass = Promagent.class.getClassLoader().loadClass(className);
            metricsClass.getMethod("init", CollectorRegistry.class).invoke(null, registry);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing " + className + ": " + e.getMessage(), e);
        }
    }

    /**
     * Add {@link ElementMatcher} for the hooks.
     */
    private static AgentBuilder applyHooks(AgentBuilder agentBuilder, HookConfig hookConfig) {
        for (HookConfig.ClassOrInterfaceConfig instruments : hookConfig.getInstrumentedClassesOrInterfaces()) {
            ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers.none();
            for (HookConfig.MethodConfig method : instruments.getInstrumentedMethods()) {
                // If you are using Eclipse or Visual Studio Code and see a syntax error here, it could be this one:
                // https://github.com/eclipse/eclipse.jdt.ls/issues/291
                ElementMatcher.Junction<MethodDescription> junction = ElementMatchers
                        .named(method.getMethodName())
                        .and(not(isAbstract()))
                        .and(isPublic())
                        .and(takesArguments(method.getParameterTypes().size()));
                for (int i = 0; i < method.getParameterTypes().size(); i++) {
                    // TODO: Tested for Objects (javax.servlet.Servlet) and primitive types (int), but not for arrays and generics yet.
                    junction = junction.and(takesArgument(i, hasSuperType(named(method.getParameterTypes().get(i)))));
                }
                methodMatcher = methodMatcher.or(junction);
            }
            agentBuilder = agentBuilder
                    .type(ElementMatchers.hasSuperType(named(instruments.getClassOrInterfaceName())))
                    .transform(new AgentBuilder.Transformer.ForAdvice()
                            .include(ClassLoaderCache.getInstance().currentClassLoader())
                            .advice(methodMatcher, PromagentAdvice.class.getName())
                    );
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

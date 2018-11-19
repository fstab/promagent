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

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import javax.management.ObjectName;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import io.promagent.agent.ClassLoaderCache;
import io.promagent.hookcontext.MetricsStore;
import io.promagent.internal.HookMetadata.MethodSignature;
import io.promagent.internal.jmx.Exporter;
import io.promagent.internal.jmx.PromagentCollectorRegistry;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class Promagent {

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            PromagentCollectorRegistry registry = new PromagentCollectorRegistry();
            ManagementFactory.getPlatformMBeanServer().registerMBean(new Exporter(registry), new ObjectName("io.promagent:type=exporter"));
            Map<String, String> args = parseCmdline(agentArgs);
            if (args.containsKey("port")) {
                BuiltInServer.run(args.get("host"), args.get("port"), registry);
            }
            ClassLoaderCache classLoaderCache = ClassLoaderCache.getInstance();
            List<Path> hookJars = classLoaderCache.getPerDeploymentJars();
            SortedSet<HookMetadata> hookMetadata = new HookMetadataParser(hookJars).parse();
            MetricsStore metricsStore = new MetricsStore(registry);
            Delegator.init(hookMetadata, metricsStore, classLoaderCache);
            printHookMetadata(hookMetadata);

            AgentBuilder agentBuilder = new AgentBuilder.Default();
            agentBuilder = applyHooks(agentBuilder, hookMetadata, classLoaderCache);
            agentBuilder
                    .disableClassFormatChanges()
                    // .with(AgentBuilder.Listener.StreamWriting.toSystemError()) // use this to see exceptions thrown in instrumented code
                    .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .installOn(inst);

            // TODO -- the following is an experiment supporting collectors directly (in addition to hooks)
            // io.prometheus.client.Collector jmxCollector = (io.prometheus.client.Collector) classLoaderCache.currentClassLoader().loadClass("io.promagent.collectors.JmxCollector").newInstance();
            // registry.registerNoJmx(jmxCollector);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Add {@link ElementMatcher} for the hooks.
     */
    private static AgentBuilder applyHooks(AgentBuilder agentBuilder, SortedSet<HookMetadata> hookMetadata, ClassLoaderCache classLoaderCache) {
        Map<String, SortedSet<MethodSignature>> instruments = getInstruments(hookMetadata);
        for (Map.Entry<String, SortedSet<MethodSignature>> entry : instruments.entrySet()) {
            String instrumentedClassName = entry.getKey();
            Set<MethodSignature> instrumentedMethods = entry.getValue();
            agentBuilder = agentBuilder
                    .type(ElementMatchers.hasSuperType(named(instrumentedClassName)))
                    .transform(new AgentBuilder.Transformer.ForAdvice()
                            .include(classLoaderCache.currentClassLoader()) // must be able to load PromagentAdvice
                            .advice(matchAnyMethodIn(instrumentedMethods), PromagentAdvice.class.getName())
                    );
        }
        return agentBuilder;
    }

    /**
     * key: name of instrumented class or interface, value: set of instrumented methods for that class or interface
     */
    public static Map<String, SortedSet<MethodSignature>> getInstruments(Set<HookMetadata> hooks) {
        Map<String, SortedSet<MethodSignature>> result = new TreeMap<>();
        for (HookMetadata hookMetadata : hooks) {
            for (String instruments : hookMetadata.getInstruments()) {
                if (!result.containsKey(instruments)) {
                    result.put(instruments, new TreeSet<>());
                }
                result.get(instruments).addAll(hookMetadata.getMethods());
            }
        }
        return result;
    }

    /**
     * Returns a byte buddy matcher matching any method contained in methodSignatures.
     */
    public static ElementMatcher<MethodDescription> matchAnyMethodIn(Set<MethodSignature> methodSignatures) {
        ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers.none();
        for (MethodSignature methodSignature : methodSignatures) {
            ElementMatcher.Junction<MethodDescription> junction = ElementMatchers
                    .named(methodSignature.getMethodName())
                    .and(not(isAbstract()))
                    .and(takesArguments(methodSignature.getParameterTypes().size()));
            for (int i = 0; i < methodSignature.getParameterTypes().size(); i++) {
                junction = junction.and(takesArgument(i, named(methodSignature.getParameterTypes().get(i))));
            }
            methodMatcher = methodMatcher.or(junction);
        }
        return methodMatcher;
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

    private static void printHookMetadata(SortedSet<HookMetadata> hookMetadata) {
        System.out.println("Promagent instrumenting the following classes or interfaces:");
        for (HookMetadata m : hookMetadata) {
            System.out.println(m);
        }
    }
}

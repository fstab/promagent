package io.promagent.internal;


import com.alibaba.fastjson.JSON;
import io.promagent.agent.ClassLoaderCache;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/9/10
 */
public class CustomHooksUtils {
    public static AgentBuilder applyHooks(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        if (Boolean.valueOf(System.getProperty("agent.skip"))) {
            return agentBuilder;
        }
        if (Boolean.valueOf(System.getProperty("agent.debug"))) {
            agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder = applyAnnotationMethodHooks(agentBuilder, classLoaderCache);
        agentBuilder = applyAnnotationClassesHooks(agentBuilder, classLoaderCache);
        agentBuilder = applyRegHooks(agentBuilder, classLoaderCache);
        return agentBuilder;
    }

    private static AgentBuilder applyAnnotationMethodHooks(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        String annotationMethodHook = System.getProperty("agent.hooks.annMethodHook");
        if (annotationMethodHook == null) {
            return agentBuilder;
        }
        Map<String, List<String>> annotationMethodHookMap = JSON.parseObject(annotationMethodHook, Map.class);

        for (Map.Entry<String, List<String>> entry : annotationMethodHookMap.entrySet()) {
            for (String value : entry.getValue()) {
                agentBuilder = agentBuilder
                        .type(nameMatches(entry.getKey()))
                        .transform(getForAdvice(classLoaderCache)
                                .advice(ElementMatchers.isAnnotatedWith(ElementMatchers.named(value.split(":")[0])), CustomPromagentAdvice.class.getName()));
            }
        }
        return agentBuilder;
    }

    private static AgentBuilder applyAnnotationClassesHooks(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        String annotationClassHook = System.getProperty("agent.hooks.annClassHook");
        if (annotationClassHook == null) {
            return agentBuilder;
        }
        Map<String, List<String>> annotationClassesHookMap = JSON.parseObject(annotationClassHook, Map.class);
        for (Map.Entry<String, List<String>> entry : annotationClassesHookMap.entrySet()) {
            for (String value : entry.getValue()) {
                agentBuilder = agentBuilder
                        .type(ElementMatchers.isAnnotatedWith(ElementMatchers.named(value.split(":")[0])))
                        .and(ElementMatchers.nameMatches(entry.getKey()))
                        .transform(getForAdvice(classLoaderCache).advice(ElementMatchers.nameMatches("[a-zA-Z].*"), CustomPromagentAdvice.class.getName()));
            }
        }
        return agentBuilder;
    }

    private static AgentBuilder applyRegHooks(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        String regHooks = System.getProperty("agent.hooks.regHook");
        if (regHooks == null) {
            return agentBuilder;
        }
        Map<String, List<String>> regHookMap = JSON.parseObject(regHooks, Map.class);
        for (Map.Entry<String, List<String>> entry : regHookMap.entrySet()) {
            agentBuilder = agentBuilder
                    .type(ElementMatchers.nameMatches(entry.getKey().split(":")[0]))
                    .transform(getForAdvice(classLoaderCache).advice(methodMatchRegMethodIn(entry.getValue()), CustomPromagentAdvice.class.getName()));
        }
        return agentBuilder;
    }


    private static ElementMatcher<MethodDescription> methodMatchRegMethodIn(List<String> methods) {
        ElementMatcher.Junction<MethodDescription> methodMatcher = ElementMatchers.none();
        for (String method : methods) {
            String[] methodArray = method.split(":");
            ElementMatcher.Junction<MethodDescription> junction = ElementMatchers.nameMatches(methodArray[0]);
            if (methodArray.length == 2) {
                Integer arguments = Integer.parseInt(methodArray[1]);
                junction = junction.and(takesArguments(arguments));
            }
            methodMatcher = methodMatcher.or(junction);
        }
        return methodMatcher;
    }

    private static AgentBuilder.Transformer.ForAdvice getForAdvice(ClassLoaderCache classLoaderCache) {
        return new AgentBuilder.Transformer.ForAdvice().include(classLoaderCache.currentClassLoader());
    }
}
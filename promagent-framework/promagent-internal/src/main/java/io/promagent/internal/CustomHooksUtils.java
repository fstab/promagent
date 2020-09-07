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
        agentBuilder = applyAnnMethodHook(agentBuilder, classLoaderCache);
        agentBuilder = applyAnnClassHook(agentBuilder, classLoaderCache);
        agentBuilder = applyRegHook(agentBuilder, classLoaderCache);
        return agentBuilder;
    }

    private static AgentBuilder applyAnnMethodHook(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        String annMethodHook = System.getProperty("agent.hooks.annMethodHook");
        if (annMethodHook == null) {
            return agentBuilder;
        }
        Map<String, List<String>> annMethodHookMap = JSON.parseObject(annMethodHook, Map.class);
        for (Map.Entry<String, List<String>> entry : annMethodHookMap.entrySet()) {
            for (String value : entry.getValue()) {
                agentBuilder = agentBuilder
                        .type(nameMatches(entry.getKey()))
                        .transform(getForAdvice(classLoaderCache).advice(ElementMatchers.isAnnotatedWith(ElementMatchers.named(value.split(":")[0])), CustomPromagentAdvice.class.getName()));
            }
        }
        return agentBuilder;
    }

    private static AgentBuilder applyAnnClassHook(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        String annClassHook = System.getProperty("agent.hooks.annClassHook");
        if (annClassHook == null) {
            return agentBuilder;
        }
        Map<String, List<String>> annClassHookMap = JSON.parseObject(annClassHook, Map.class);
        for (Map.Entry<String, List<String>> entry : annClassHookMap.entrySet()) {
            for (String value : entry.getValue()) {
                agentBuilder = agentBuilder
                        .type(ElementMatchers.isAnnotatedWith(ElementMatchers.named(value.split(":")[0])))
                        .and(ElementMatchers.nameMatches(entry.getKey()))
                        .transform(getForAdvice(classLoaderCache).advice(ElementMatchers.nameMatches("[a-zA-Z].*"), CustomPromagentAdvice.class.getName()));
            }
        }
        return agentBuilder;
    }

    private static AgentBuilder applyRegHook(AgentBuilder agentBuilder, ClassLoaderCache classLoaderCache) {
        String regHook = System.getProperty("agent.hooks.regHook");
        if (regHook == null) {
            return agentBuilder;
        }
        Map<String, List<String>> regHookMap = JSON.parseObject(regHook, Map.class);
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
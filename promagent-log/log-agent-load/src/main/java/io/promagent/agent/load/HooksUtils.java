package io.promagent.agent.load;

import org.springframework.util.CollectionUtils;

import java.util.*;

public class HooksUtils {

    private static List<String> controllerHook = Arrays.asList(
            "org.springframework.web.bind.annotation.RequestMapping:ACCESS",
            "org.springframework.web.bind.annotation.PostMapping:ACCESS",
            "org.springframework.web.bind.annotation.GetMapping:ACCESS",
            "org.springframework.web.bind.annotation.DeleteMapping:ACCESS",
            "org.springframework.web.bind.annotation.PutMapping:ACCESS");

    private static List<String> scheduledHook = Arrays.asList(
            "org.springframework.scheduling.annotation.Scheduled:CRON");

    public static void addControllerHook(String packageName, AgentConfig.Hooks hooks) {
        addCommonAnnMethod(packageName, hooks, controllerHook);
    }

    public static void addScheduledHook(String packageName, AgentConfig.Hooks hooks) {
        addCommonAnnMethod(packageName, hooks, scheduledHook);
    }
    private static void addCommonAnnMethod(String packageName, AgentConfig.Hooks hooks, List<String> defaultHook) {
        packageName = "^" + packageName + ".*";
        if (CollectionUtils.isEmpty(hooks.getAnnMethodHook().get(packageName))) {
            hooks.getAnnMethodHook().put(packageName, new ArrayList<String>());
        }
        hooks.getAnnMethodHook().get(packageName).addAll(defaultHook);
    }
    public static Map<String, String> getRegType(AgentConfig.Hooks hooks) {
        Map<String, String> regType = new HashMap();
        for (String key : hooks.getRegHook().keySet()) {
            String ann[] = key.split(":");
            regType.put(ann[0], ann.length == 2 ? ann[1] : "SYSTEM");
        }
        return regType;
    }

    public static Map<String, String> getAnnMethodType(AgentConfig.Hooks hooks) {
        return getAnnType(hooks.getAnnMethodHook());
    }

    public static Map<String, String> getAnnClassType(AgentConfig.Hooks hooks) {
        return getAnnType(hooks.getAnnClassHook());
    }

    private static Map<String, String> getAnnType(Map<String, List<String>> annotationTypeHooks) {
        Map<String, String> annotationType = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : annotationTypeHooks.entrySet()) {
            for (String key : entry.getValue()) {
                String ann[] = key.split(":");
                annotationType.put(ann[0], ann.length == 2 ? ann[1] : "SYSTEM");
            }
        }
        return annotationType;
    }
}

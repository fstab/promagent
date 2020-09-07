package io.promagent.log.spring;

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

    public static void addControllerHook(String type, Hooks hooks) {
        addDefaultAnnMethod(type, hooks, controllerHook);
    }

    public static void addScheduledHook(String type, Hooks hooks) {
        addDefaultAnnMethod(type, hooks, scheduledHook);
    }

    private static void addDefaultAnnMethod(String type, Hooks hooks, List<String> defaultHook) {
        type = "^" + type + ".*";
        if (CollectionUtils.isEmpty(hooks.getAnnMethodHook().get(type))) {
            hooks.getAnnMethodHook().put(type, new ArrayList<String>());
        }
        hooks.getAnnMethodHook().get(type).addAll(defaultHook);
    }

    public static Map<String, String> getRegType(Hooks hooks) {
        if (CollectionUtils.isEmpty(hooks.getRegHook())) {
            return new HashMap<>();
        }
        Map<String, String> regType = new HashMap();
        for (String key : hooks.getRegHook().keySet()) {
            String ann[] = key.split(":");
            regType.put(ann[0], ann.length == 2 ? ann[1] : "SYSTEM");
        }
        return regType;
    }

    public static Map<String, String> getAnnMethodType(Hooks hooks) {
        return getAnnType(hooks.getAnnMethodHook());
    }

    public static Map<String, String> getAnnClassType(Hooks hooks) {
        return getAnnType(hooks.getAnnClassHook());
    }

    private static Map<String, String> getAnnType(Map<String, List<String>> annotationTypeHooks) {
        if (CollectionUtils.isEmpty(annotationTypeHooks)) {
            return new HashMap<>();
        }
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

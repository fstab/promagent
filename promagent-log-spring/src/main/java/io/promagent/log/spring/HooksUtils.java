package io.promagent.log.spring;


import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HooksUtils {
    public static Map<String, String> getRegType(Hooks hooks) {
        if (CollectionUtils.isEmpty(hooks.getRegHooks())) {
            return new HashMap<>();
        }
        Map<String, String> regType = new HashMap();
        for (String key : hooks.getRegHooks().keySet()) {
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

//    public static List<String> defaultAnnMethodHookList() {
//        return Arrays.asList(
//                "org.springframework.web.bind.annotation.RequestMapping:ACCESS" ,
//                "org.springframework.web.bind.annotation.PostMapping:ACCESS",
//                "org.springframework.web.bind.annotation.GetMapping:ACCESS",
//                "org.springframework.web.bind.annotation.DeleteMapping:ACCESS",
//                "org.springframework.web.bind.annotation.PutMapping:ACCESS",
//                "org.springframework.scheduling.annotation.Scheduled:CRON");
//    }
}

package io.promagent.agent.core.utils;

import java.util.Collection;
import java.util.Map;

public class CollectionUtils {
    // copy from org.springframework.util.CollectionUtils
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

}

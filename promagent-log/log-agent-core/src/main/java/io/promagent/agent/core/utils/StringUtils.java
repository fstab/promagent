package io.promagent.agent.core.utils;

public class StringUtils {
//    copy from org.springframework.util.StringUtils
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }
}

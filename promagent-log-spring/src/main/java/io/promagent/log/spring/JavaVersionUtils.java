package io.promagent.log.spring;

/**
 * @author hengyunabc 2018-11-21
 */
public class JavaVersionUtils {
    private static final String VERSION_PROP_NAME = "java.specification.version";
    private static final String JAVA_VERSION_STR = System.getProperty(VERSION_PROP_NAME);
    private static final float JAVA_VERSION = Float.parseFloat(JAVA_VERSION_STR);

    public static boolean isLessThanJava9() {
        return JAVA_VERSION < 9.0f;
    }

    public static boolean isGreaterThanJava8() {
        return JAVA_VERSION > 1.8f;
    }
}

package io.promagent.internal.instrumentationtests.classes;

public class StaticFinalExample {

    public String helloPublic(String name) {
        return "hello public " + name;
    }

    public final String helloPublicFinal(String name) {
        return "hello public final " + name;
    }

    public static String helloPublicStatic(String name) {
        return "hello public static " + name;
    }

    public static String helloPublicStaticFinal(String name) {
        return "hello public static final " + name;
    }
}

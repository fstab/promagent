package io.promagent.log.core;

public class HttpContext {

    private static final ThreadLocal<LogObject> threadLocal = ThreadLocal.withInitial(LogObject::new);

    protected static LogObject get() {
        return threadLocal.get();
    }

    protected static void clean() {
        threadLocal.remove();
    }

}

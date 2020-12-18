package io.promagent.agent.core.utils;


import io.promagent.agent.core.config.LogConstants;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ThrowableUtils {
    //    org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace() copy
    public static String getStackTrace(Throwable t) {
        if (t == null) {
            return LogConstants.null_string;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}

package io.promagent.log.utils;

import io.promagent.log.config.LogConfig;
import io.promagent.log.config.LogConstants;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;


public class MdcUtils {

    public static void setLogId(String val) {
        val = StringUtils.isEmpty(val) ? UUID.randomUUID().toString() : val;
        MDC.put(LogConstants.mdc_logId, val);
        MDC.put(LogConstants.mdc_appName, LogConfig.appName);
        MDC.put(LogConstants.mdc_appEvn, LogConfig.appEvn);
    }

    public static String getLogId() {
        String logId = MDC.get(LogConstants.mdc_logId);
        if (StringUtils.isEmpty(logId)) {
            logId = UUID.randomUUID().toString();
            MDC.put(LogConstants.mdc_logId, logId);
            return logId;
        }
        return logId;
    }

    public static String getPspanId() {
        return MDC.get(LogConstants.basic_PtxId);
    }

    public static void mdcClear() {
        MDC.clear();
    }


}

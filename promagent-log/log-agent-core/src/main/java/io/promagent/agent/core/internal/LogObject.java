package io.promagent.agent.core.internal;

import com.alibaba.fastjson.JSONObject;


import io.promagent.agent.core.config.GradeConstants;
import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.config.LogConstants;
import io.promagent.agent.core.config.TypeConstants;
import io.promagent.agent.core.utils.LogObjectUtils;
import io.promagent.agent.core.utils.MdcUtils;
import io.promagent.agent.core.utils.StringUtils;
import io.promagent.agent.core.utils.ThrowableUtils;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogObject {

    private int sn = 0;
    private String type;
    private String grade;
    @Getter
    private Map<String, Object> request = new ConcurrentHashMap<>();
    private Map<String, Object> method = new ConcurrentHashMap<>();

    @Getter
    private JSONObject tempData = new JSONObject();
    private String beforeError;

    protected LogObject setMethod(Long exec, Throwable error, Object ret, String sign, Object[] args, String type, String grade) {

        this.type = StringUtils.isEmpty(type) ? TypeConstants.DEFAULT : type;
        this.grade = StringUtils.isEmpty(grade) ? GradeConstants.DEFAULT : grade;

        if (StringUtils.isEmpty(exec)) {
            method.remove(LogConstants.met_exec);
        } else {
            method.put(LogConstants.met_exec, exec);
        }
        if (StringUtils.isEmpty(sign)) {
            method.remove(LogConstants.met_sign);
        } else {
            method.put(LogConstants.met_sign, sign);
        }
        if (StringUtils.isEmpty(args) || args.length == 0) {
            method.remove(LogConstants.met_args);
        } else {
            method.put(LogConstants.met_args, LogObjectUtils.getArgs(args));
        }
        if (StringUtils.isEmpty(ret)) {
            method.remove(LogConstants.met_ret);
        } else {
            String metRet = LogConfig.skipRetSignatures.contains(sign) ? LogConstants.skip : LogObjectUtils.getReturn(ret);
            method.put(LogConstants.met_ret, metRet);
        }

        if (StringUtils.isEmpty(error)) {
            method.remove(LogConstants.met_thrown);
        } else {
            // 避免对同一个异常多次打印
            if (StringUtils.isEmpty(beforeError) || !error.getMessage().contains(beforeError)) {
                this.grade = GradeConstants.EXCEPTION;
                method.put(LogConstants.met_thrown, ThrowableUtils.getStackTrace(error));
                beforeError = error.getMessage();
                tempData.put(LogConstants.LogPrinted, true);
            } else {
                method.put(LogConstants.met_thrown, LogConstants.LogPrinted);
                tempData.remove(LogConstants.LogPrinted);
            }
        }
        return this;
    }

    protected String getLogJson() {
        return new JSONObject()
                .fluentPut(LogConstants.basic, new JSONObject()
                        .fluentPut(LogConstants.basic_sn, ++sn)
                        .fluentPut(LogConstants.basic_ip, LogConfig.IP)
                        .fluentPut(LogConstants.basic_grade, grade))
                .fluentPut(LogConstants.mdc, new JSONObject()
                        .fluentPut(LogConstants.mdc_appEvn, LogConfig.appEvn)
                        .fluentPut(LogConstants.mdc_appName, LogConfig.appName)
                        .fluentPut(LogConstants.mdc_type, type)
                        .fluentPut(LogConstants.mdc_logId, MdcUtils.getLogId()))
                .fluentPut(LogConstants.request, request)
                .fluentPut(LogConstants.method, method)
                .toString();
    }
}

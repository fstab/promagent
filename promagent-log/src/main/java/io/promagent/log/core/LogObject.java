package io.promagent.log.core;

import com.alibaba.fastjson.JSONObject;

import io.promagent.log.config.LogConfig;
import io.promagent.log.config.LogConstants;
import io.promagent.log.config.TypeConstants;
import io.promagent.log.utils.MdcUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class LogObject {

    private int sn = 0;
    private String PtxId;

    private String type = TypeConstants.DEFAULT;

    private Map<String, Object> request = new HashMap<>();
    private Map<String, Object> method = new HashMap<>();

    private Map<String, Object> tempData = new HashMap<>();

    protected void setRequest(Map<String, String> header, String uri, Map<String, String> params) {
        request.put(LogConstants.reg_header, header);
        request.put(LogConstants.reg_url, uri);
        request.put(LogConstants.reg_params, params);
    }

    protected void setMethod(Long exce, Throwable thrown, Object ret, Method sig, Object[] args, String type) {
        method.put(LogConstants.met_exec, exce);
        method.put(LogConstants.met_sig, LogObjectUtils.getSignature(sig));
        method.put(LogConstants.met_args, LogObjectUtils.getArgs(args));
        method.put(LogConstants.met_ret, LogObjectUtils.getReturn(ret));
        method.put(LogConstants.met_thrown, LogObjectUtils.thrownToString(thrown));
        this.type = type;
    }

    public Object getTempData(String tempKey) {
        return tempData.get(tempKey);
    }

    public void setTempData(String key, Object tempData) {
        this.tempData.put(key, tempData);
    }

    protected void setMsg(Throwable msg) {
        method.put(LogConstants.met_thrown, LogObjectUtils.thrownToString(msg));
    }

    protected String getLogJson() {
        sn++;
        PtxId = MdcUtils.getPspanId();
        PtxId = StringUtils.isEmpty(PtxId) ? LogConstants.NULL : PtxId;
        JSONObject resultJson = new JSONObject();

        resultJson.put(LogConstants.request, request);
        resultJson.put(LogConstants.method, method);
        resultJson.put(LogConstants.basic, getBasic());
        resultJson.put(LogConstants.mdc, getMdc());
        return resultJson.toJSONString();
    }

    private Map<String, Object> getBasic() {
        Map<String, Object> basic = new HashMap<>();
        basic.put(LogConstants.basic_PtxId, PtxId);
        basic.put(LogConstants.basic_sn, sn);
        basic.put(LogConstants.basic_logStamp, System.currentTimeMillis());
        basic.put(LogConstants.basic_ip, LogConfig.IP);
        return basic;
    }

    private Map<String, Object> getMdc() {
        Map<String, Object> mdc = new HashMap<>();
        mdc.put(LogConstants.mdc_appEvn, LogConfig.appEvn);
        mdc.put(LogConstants.mdc_appName, LogConfig.appName);
        mdc.put(LogConstants.mdc_type, type);
        mdc.put(LogConstants.mdc_logId, MdcUtils.getLogId());
        return mdc;
    }

    protected void setType(String type) {
        if (type != null) {
            this.type = type;
        }
    }
}

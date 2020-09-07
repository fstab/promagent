package io.promagent.log.config;

public interface LogConstants {

    String request = "request";
    String reg_url = "url";
    String reg_header = "header";
    String reg_params = "params";

    String method = "method";
    String met_args = "args";
    String met_ret = "ret";
    String met_sig = "sig";
    String met_exec = "exec";
    String met_thrown = "thrown";

    String basic = "basic";
    String basic_sn = "sn";
    String basic_PtxId = "PtxId";
    String basic_logStamp = "logStamp";
    String basic_ip = "ip";

    String mdc = "mdc";
    String mdc_logId = System.getProperty("agent.mdcLogId");
    String mdc_appName = "appName";
    String mdc_appEvn = "appEvn";
    String mdc_type = "type";

    String NULL = "NULL";
    String SKIP = "SKIP";
    String preSignature = "preSignature";

}

package io.promagent.agent.core.config;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogConfig {
    public static String TRACE_ID = System.getProperty("agent.traceId");
    public static String IP = System.getProperty("agent.ip");
    public static int RET_MAX_LENGTH = Integer.parseInt(System.getProperty("agent.retMaxLength"));
    public static String appName = System.getProperty("agent.appName");
    public static String appEvn = System.getProperty("agent.appEvn");
    public static List<String> headers = JSONArray.parseArray(System.getProperty("agent.headers"), String.class);
    public static List<String> ignoreSignatures = JSONArray.parseArray(System.getProperty("agent.ignoreSignatures"), String.class);
    public static List<String> skipRetSignatures = JSONArray.parseArray(System.getProperty("agent.skipRetSignatures"), String.class);

    public static Map<String, String> typeCache = new ConcurrentHashMap<>();
    public static Map<String, String> annClassType = JSON.parseObject(System.getProperty("agent.hooks.annClassType"), Map.class);
    public static Map<String, String> annMethodType = JSON.parseObject(System.getProperty("agent.hooks.annMethodType"), Map.class);
    public static Map<String, String> regType = JSON.parseObject(System.getProperty("agent.hooks.regType"), Map.class);

}

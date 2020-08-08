package io.promagent.spring;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AgentConfig {
    private String groupId = "io.promagent";
    private String artifactId = "promagent-log";
    private String metadataXml = "maven-metadata.xml";

    private String metadataUrl;
    private String metadataVersionUrl;
    private String remoteDownloadUrl;

    private String mvmName;

    private String mvmPass;

    @Value("${promagent.agent.skip:false}")
    private Boolean skip;

    @Value("${promagent.agent.dir:${user.home}}")
    private String agentDir;

    @Value("${promagent.agent.jarFile:}")
    private String jarFile;

    @Value("${promagent.agent.debug:false}")
    private String debug;

    @Value("${promagent.agent.appEvn}")
    private String appEvn;

    @Value("${promagent.agent.appName}")
    private String appName;

    @Value("#{'${promagent.agent.headers:all}'.split(':')}")
    private List<String> headers;

    @Value("${promagent.agent.headerRequestId:X-REQUEST-ID}")
    private String headerRequestId;

    @Value("${promagent.agent.maxMsgLength:20480}")
    private int maxMsgLength;

    @Value("${promagent.agent.callClass:io.promagent.log.Logger}")
    private String callClass;

    @Value("${promagent.agent.callMethod:log}")
    private String callMethod;

    @Value("${promagent.agent.callErrorMethod:frameError}")
    private String callErrorMethod;

    @Value("${promagent.agent.hookYml:/hook.yml}")
    private String hookYml;

    @Value("${promagent.agent.userMap:{\"colony\":\"default\"}}")
    private String userMap;

    private Hooks hooks;
    private String pid;

    @PostConstruct
    public void init() {
        initMetadata();
        initHooks();
        initAgentDir();
        initPid();
        initProperty();
        initUserMap();
    }

    public void initUserMap() {
        try {
            JSON.parseObject(this.userMap, Map.class);
        } catch (Exception ignore) {
            this.userMap = "{\"userMap\":\"error\"}";
        }
    }


    public void initMetadata() {
        metadataVersionUrl = metadataUrl + "/" + groupId + "/" + artifactId + "/{0}/" + metadataXml;
        remoteDownloadUrl = metadataUrl + "/" + groupId + "/" + artifactId + "/{0}/" + artifactId + "-{1}.jar";
        metadataUrl = metadataUrl + "/" + groupId + "/" + artifactId + "/" + metadataXml;
    }

    private void initHooks() {
        InputStream configIn = new BufferedInputStream(getClass().getResourceAsStream(hookYml));
        String hooksStr = JSON.toJSONString(new Yaml().load(configIn));
        Hooks hooks = JSONObject.parseObject(hooksStr, Hooks.class);
        this.setHooks(hooks);
    }

    private void initAgentDir() {
        File agentFile;
        try {
            agentFile = new File(agentDir + File.separator + ".agentLog" + File.separator + "lib");
            agentFile.mkdirs();
            agentDir = agentFile.getAbsolutePath();
            return;
        } catch (Throwable ignore) {
            log.error(ignore.getMessage());
        }

        try {
            agentFile = new File(System.getProperty("java.io.tmpdir") + File.separator + ".agentLog" + File.separator + "lib");
            agentFile.mkdirs();
            agentDir = agentFile.getAbsolutePath();
        } catch (Throwable ignore) {
            log.error(ignore.getMessage());
        }
    }

    public void initPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        this.setPid(pid);
    }

    public void initProperty() {
        System.setProperty("agent.skip", this.skip.toString());
        System.setProperty("agent.debug", this.debug.toString());
        System.setProperty("agent.appEvn", this.appEvn);
        System.setProperty("agent.appName", this.appName);
        System.setProperty("agent.requestId", this.headerRequestId);
        System.setProperty("agent.maxMsg", String.valueOf(this.maxMsgLength));
        System.setProperty("agent.callClass", this.callClass);
        System.setProperty("agent.callMethod", this.callMethod);
        System.setProperty("agent.callErrorMethod", this.callErrorMethod);
        System.setProperty("agent.headers", JSONObject.toJSONString(this.headers));
        System.setProperty("agent.userMap", this.userMap);

        if (!CollectionUtils.isEmpty(this.hooks.getAnnMethodHook())) {
            System.setProperty("agent.hooks.annMetHook", JSONObject.toJSONString(this.hooks.getAnnMethodHook()));
            System.setProperty("agent.hooks.annMetType", JSONObject.toJSONString(HooksUtils.getAnnMethodType(hooks)));
        }
        if (!CollectionUtils.isEmpty(this.hooks.getAnnMethodHook())) {
            System.setProperty("agent.hooks.annClassHook", JSONObject.toJSONString(this.hooks.getAnnMethodHook()));
            System.setProperty("agent.hooks.annClassType", JSONObject.toJSONString(HooksUtils.getAnnClassType(hooks)));
        }
        if (!CollectionUtils.isEmpty(this.hooks.getRegHooks())) {
            System.setProperty("agent.hooks.regHooks", JSONObject.toJSONString(this.hooks.getRegHooks()));
            System.setProperty("agent.hooks.regTypes", JSONObject.toJSONString(HooksUtils.getRegType(hooks)));
        }
    }
}



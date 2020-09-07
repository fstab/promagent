package io.promagent.log.spring;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AgentConfig {

    @Value("${promagent.agent.appName}")
    private String appName;

    @Value("${promagent.agent.appEvn}")
    private String appEvn;

    @Value("${promagent.mvm.snapshotUrl:https://raw.githubusercontent.com/javazhangyi/promagent/master}")
    private String snapshotUrl;

    @Value("${promagent.mvm.releaseUrl:}")
    private String releaseUrl;

    @Value("${promagent.mvm.auth.token:}")
    private String token;
    @Value("${promagent.agent.skip:false}")
    private Boolean skip;
    @Value("${promagent.agent.jarFile:}")
    private String jarFile;
    @Value("${promagent.agent.debug:false}")
    private Boolean debug;

    @Value("${promagent.agent.mdcLogId:logId}")
    private String mdcLogId;
    @Value("#{'${promagent.agent.headers:none}'.split(':')}")
    private List<String> headers;
    @Value("#{'${promagent.agent.ignoreSignatures:none}'.split(':')}")
    private List<String> ignoreSignatures;
    @Value("${promagent.agent.traceId:X-REQUEST-ID}")
    private String traceId;
    @Value("${promagent.agent.maxMsg:20480}")
    private Integer maxMsg;
    @Value("${promagent.hooks.default.controllerPack:}")
    private String controllerPack;
    @Value("${promagent.hooks.default.scheduledPack:}")
    private String scheduledPack;

    private String groupId = "io/promagent";
    private String artifactId = "promagent-log";
    private String metadataTag = "version";
    private String metadataVersionTag = "value";
    private String metadataXml = "maven-metadata.xml";
    private String agentDir = System.getProperty("user.home") + File.separator + ".agent" + File.separator + "lib";
    private String callClass = "io.promagent.log.Logger";
    private String callInfoMethod = "info";
    private String callErrorMethod = "error";
    private String hookYml = "/hook.yml";

    private String metadataUrl;
    private String metadataVersionUrl;
    private String remoteDownloadUrl;
    private String ip;
    private String downloadTime;
    private String agentTime;
    private Boolean loadAgent;
    private Hooks hooks;
    private String pid;

    @PostConstruct
    public void init() {
        initMetadata();
        initHooks();
        initAgentDir();
        initPid();
        initIp();
        initProperty();
    }

    public void initIp() {
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignore) {
            ip = "error";
        }
    }

    public void initMetadata() {
        String mirrorUrl = StringUtils.isEmpty(releaseUrl) ? snapshotUrl : releaseUrl;
        metadataVersionUrl = mirrorUrl + "/" + groupId + "/" + artifactId + "/{0}/" + metadataXml;
        remoteDownloadUrl = mirrorUrl + "/" + groupId + "/" + artifactId + "/{0}/" + artifactId + "-{1}.jar";
        metadataUrl = mirrorUrl + "/" + groupId + "/" + artifactId + "/" + metadataXml;
    }

    private void initHooks() {
        try {
            InputStream configIn = new BufferedInputStream(getClass().getResourceAsStream(hookYml));
            String hooksStr = JSON.toJSONString(new Yaml().load(configIn));
            Hooks hooks = JSONObject.parseObject(hooksStr, Hooks.class);
            this.setHooks(hooks);
        } catch (Exception ignore) {
        }
    }

    private void initAgentDir() {
        File agentFile;
        try {
            agentFile = new File(agentDir);
            agentFile.mkdirs();
            agentDir = agentFile.getAbsolutePath();
            return;
        } catch (Throwable ignore) {
            log.error(ignore.getMessage());
        }

        try {
            agentFile = new File(System.getProperty("java.io.tmpdir") + File.separator + ".agent" + File.separator + "lib");
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
        System.setProperty("agent.ip", ip);
        System.setProperty("agent.skip", skip.toString());
        System.setProperty("agent.debug", debug.toString());
        System.setProperty("agent.appEvn", appEvn);
        System.setProperty("agent.appName", appName);
        System.setProperty("agent.traceId", traceId);
        System.setProperty("agent.mdcLogId", mdcLogId);
        System.setProperty("agent.maxMsg", String.valueOf(maxMsg));
        System.setProperty("agent.callClass", callClass);
        System.setProperty("agent.callInfoMethod", callInfoMethod);
        System.setProperty("agent.callErrorMethod", callErrorMethod);
        System.setProperty("agent.headers", JSONObject.toJSONString(headers));
        System.setProperty("agent.ignoreSignatures", JSONObject.toJSONString(ignoreSignatures));

        if (StringUtils.isEmpty(hooks)) {
            hooks = new Hooks();
        }
        if (CollectionUtils.isEmpty(hooks.getAnnMethodHook())) {
            hooks.setAnnMethodHook(new HashMap<String, List<String>>());
        }
        if (CollectionUtils.isEmpty(hooks.getAnnClassHook())) {
            hooks.setAnnClassHook(new HashMap<String, List<String>>());
        }
        if (CollectionUtils.isEmpty(hooks.getRegHook())) {
            hooks.setRegHook(new HashMap<String, List<String>>());
        }
        if (!StringUtils.isEmpty(controllerPack)) {
            HooksUtils.addControllerHook(controllerPack, hooks);
        }
        if (!StringUtils.isEmpty(scheduledPack)) {
            HooksUtils.addScheduledHook(scheduledPack, hooks);
        }
        System.setProperty("agent.hooks.annMethodHook", JSONObject.toJSONString(hooks.getAnnMethodHook()));
        System.setProperty("agent.hooks.annMethodType", JSONObject.toJSONString(HooksUtils.getAnnMethodType(hooks)));

        System.setProperty("agent.hooks.annClassHook", JSONObject.toJSONString(hooks.getAnnClassHook()));
        System.setProperty("agent.hooks.annClassType", JSONObject.toJSONString(HooksUtils.getAnnClassType(hooks)));

        System.setProperty("agent.hooks.regHook", JSONObject.toJSONString(hooks.getRegHook()));
        System.setProperty("agent.hooks.regType", JSONObject.toJSONString(HooksUtils.getRegType(hooks)));
    }
}
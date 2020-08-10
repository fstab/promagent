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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AgentConfig {

    @Value("#{'${promagent.mvm.groupId:io.promagent}'.replace('.','/')}")
    private String groupId;

    @Value("${promagent.mvm.artifactId:promagent-log}")
    private String artifactId;

    private String metadataXml = "maven-metadata.xml";

    @Value("${promagent.mvm.metadataUrl:https://raw.githubusercontent.com/javazhangyi/promagent/master}")
    private String metadataUrl;

    @Value("${promagent.mvm.metadataTag:version}")
    private String metadataTag;

    private String metadataVersionUrl;
    @Value("${promagent.mvm.metadataVersionTag:value}")
    private String metadataVersionTag;

    private String remoteDownloadUrl;

    @Value("${promagent.mvm.name:}")
    private String name;

    @Value("${promagent.mvm.pass:}")
    private String pass;

    @Value("${promagent.agent.upJarTask:false}")
    private Boolean upJarTask;

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

    @Value("${promagent.agent.requestId:X-REQUEST-ID}")
    private String requestId;

    @Value("${promagent.agent.maxMsg:20480}")
    private int maxMsg;

    @Value("${promagent.agent.callClass:io.promagent.log.Logger}")
    private String callClass;

    @Value("${promagent.agent.callMethod:info}")
    private String callMethod;

    @Value("${promagent.agent.callErrorMethod:error}")
    private String callErrorMethod;

    @Value("${promagent.agent.hookYml:/hook.yml}")
    private String hookYml;

    @Value("${promagent.agent.userMap:{\"colony\":\"default\"}}")
    private String userMap;

    @Value("#{'${promagent.agent.hookGroupId:[a-zA-Z]}'.concat('.*')}")
    private String hookGroupId;

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
        System.setProperty("agent.skip", skip.toString());
        System.setProperty("agent.debug", debug.toString());
        System.setProperty("agent.appEvn", appEvn);
        System.setProperty("agent.appName", appName);
        System.setProperty("agent.requestId", requestId);
        System.setProperty("agent.maxMsg", String.valueOf(maxMsg));
        System.setProperty("agent.callClass", callClass);
        System.setProperty("agent.callMethod", callMethod);
        System.setProperty("agent.callErrorMethod", callErrorMethod);
        System.setProperty("agent.headers", JSONObject.toJSONString(headers));
        System.setProperty("agent.userMap", userMap);


        if (StringUtils.isEmpty(hooks)) {
            hooks = new Hooks();
        }
        if (CollectionUtils.isEmpty(hooks.getAnnClassHook())) {
            hooks.setAnnClassHook(new HashMap<>());
        }
        if (CollectionUtils.isEmpty(hooks.getAnnMethodHook())) {
            hooks.setAnnMethodHook(new HashMap<>());
        }
        if (CollectionUtils.isEmpty(hooks.getRegHooks())) {
            hooks.setRegHooks(new HashMap<>());
        }

        System.setProperty("agent.hooks.annMethodHook", JSONObject.toJSONString(hooks.getAnnMethodHook()));
        System.setProperty("agent.hooks.annMethodType", JSONObject.toJSONString(HooksUtils.getAnnMethodType(hooks)));

        System.setProperty("agent.hooks.annClassHook", JSONObject.toJSONString(hooks.getAnnClassHook()));
        System.setProperty("agent.hooks.annClassType", JSONObject.toJSONString(HooksUtils.getAnnClassType(hooks)));

        System.setProperty("agent.hooks.regHook", JSONObject.toJSONString(hooks.getRegHooks()));
        System.setProperty("agent.hooks.regType", JSONObject.toJSONString(HooksUtils.getRegType(hooks)));
    }
}



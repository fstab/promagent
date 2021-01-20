package io.promagent.agent.load;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;


@Component
@Slf4j
public class AgentBootstrap {

    @Value("${promagent.agent.appEvn}")
    private String appEvn;

    private AgentConfig agentConfig = new AgentConfig();

    private AgentConfig.Hooks hooks;
    private AgentConfig.Agent agent;
    private AgentConfig.Load load;
    private AgentConfig.FastHooks fastHooks;

    @PostConstruct
    public void init() {
        try {
            if (!StringUtils.isEmpty(System.getProperty("agent.appEvn"))) {
                throw new RuntimeException("已加载agent");
            }
            initAgentConfig();
            initAgentJar();
            initSystemProperty();
            loadAgent();
        } catch (Throwable e) {
            log.error(ExceptionUtils.getStackTrace(e));
        } finally {
            System.err.println(JSONObject.toJSONString(agentConfig, true));
            log.info(JSONObject.toJSONString(agentConfig));
        }
    }

    private void loadAgent() {
        long start = System.currentTimeMillis();
        ByteBuddyAgent.attach(load.getAgentJar(), ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE);
        load.setResult(true);
        load.setTime(System.currentTimeMillis() - start);
    }

    private void initAgentJar() throws IOException {
        if (!StringUtils.isEmpty(load.getAgentJar())) {
            return;
        }
        String jarName = "/log-agent.jar";
        try (InputStream jarStream = getClass().getResourceAsStream(jarName)) {
            File agentJarFile = new File(System.getProperty("java.io.tmpdir") + File.separator + agent.getAppName(), jarName);
            if (jarStream != null) {
                FileUtils.copyInputStreamToFile(jarStream, agentJarFile);
                load.setAgentJar(agentJarFile);
                return;
            }else if (agentJarFile.exists()){
                load.setAgentJar(agentJarFile);
                return;
            }
        }
        throw new RuntimeException("开发环境需要执行 package 生命周期，才会打印日志");
    }

    private void initAgentConfig() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/hook.yml")) {
            agentConfig = new Yaml().loadAs(inputStream, AgentConfig.AgentConfigPrefix.class).getPromagent();
            this.agent = agentConfig.getAgent();
            this.load = agentConfig.getLoad();
            this.hooks = agentConfig.getHooks();
            this.fastHooks = agentConfig.getFastHooks();
        }
    }

    private void initSystemProperty() throws UnknownHostException {

        if (StringUtils.isEmpty(this.appEvn) || this.appEvn.startsWith("$")) {
            this.appEvn = agent.getAppEvn();
        } else {
            agent.setAppEvn(this.appEvn);
        }

        System.setProperty("agent.ip", InetAddress.getLocalHost().getHostAddress());
        System.setProperty("agent.appEvn", this.appEvn);

        System.setProperty("agent.debug", String.valueOf(agent.isDebug()));
        System.setProperty("agent.retMaxLength", String.valueOf(agent.getRetMaxLength()));

        System.setProperty("agent.appName", agent.getAppName());
        System.setProperty("agent.traceId", agent.getTraceId());
        System.setProperty("agent.mdcLogId", agent.getMdcLogId());

        System.setProperty("agent.callClass", agent.getCallClass());

        System.setProperty("agent.headers", JSONObject.toJSONString(agent.getHeaders()));
        System.setProperty("agent.ignoreSignatures", JSONObject.toJSONString(agent.getIgnoreSignatures()));
        System.setProperty("agent.skipRetSignatures", JSONObject.toJSONString(agent.getSkipRetSignatures()));

        if (!StringUtils.isEmpty(fastHooks.getControllerPackage())) {
            HooksUtils.addControllerHook(fastHooks.getControllerPackage(), hooks);
        }
        if (!StringUtils.isEmpty(fastHooks.getScheduledPackage())) {
            HooksUtils.addScheduledHook(fastHooks.getScheduledPackage(), hooks);
        }
        System.setProperty("agent.hooks.annMethodHook", JSONObject.toJSONString(hooks.getAnnMethodHook()));
        System.setProperty("agent.hooks.annMethodType", JSONObject.toJSONString(HooksUtils.getAnnMethodType(hooks)));

        System.setProperty("agent.hooks.annClassHook", JSONObject.toJSONString(hooks.getAnnClassHook()));
        System.setProperty("agent.hooks.annClassType", JSONObject.toJSONString(HooksUtils.getAnnClassType(hooks)));

        System.setProperty("agent.hooks.regHook", JSONObject.toJSONString(hooks.getRegHook()));
        System.setProperty("agent.hooks.regType", JSONObject.toJSONString(HooksUtils.getRegType(hooks)));
    }
}
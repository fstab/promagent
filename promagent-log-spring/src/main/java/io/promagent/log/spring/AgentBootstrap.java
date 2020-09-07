package io.promagent.log.spring;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class AgentBootstrap {
    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private DownloadUtils downloadUtils;

    @PostConstruct
    public void init() {
        try {
            log.info("agent bootstrap init");
            long startTime = System.currentTimeMillis();
            if (StringUtils.isEmpty(agentConfig.getJarFile())) {
                downloadUtils.updateJar();
            }
            List<String> attachArgs = Arrays.asList("-cp", agentConfig.getJarFile(), "io.promagent.agent.AgentBootstrap", agentConfig.getPid(), agentConfig.getJarFile());
            ProcessUtils.startAgent(attachArgs);
            String agentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " Sec";
            agentConfig.setAgentTime(agentTime);
            agentConfig.setLoadAgent(true);
        } catch (Exception e) {
            agentConfig.setLoadAgent(false);
            e.printStackTrace();
            log.info("agent bootstrap error", e.getMessage());
        } catch (NoClassDefFoundError error) {
            agentConfig.setLoadAgent(false);
            log.info("1、war 类型且非idea环境中，pom添加下列plugin; 2、war 类型，idea 环境中，mvn clean package");
            log.error("agent bootstrap error", error);
        } finally {
            log.info(JSONObject.toJSONString(agentConfig));
            System.err.println(JSONObject.toJSONString(agentConfig, true));
        }
    }
}



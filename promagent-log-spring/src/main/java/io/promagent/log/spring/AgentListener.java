package io.promagent.log.spring;


import com.alibaba.fastjson.JSONObject;
import com.sun.tools.attach.VirtualMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class AgentListener implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private DownloadUtils downloadUtils;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        try {
            if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
                upJarTask();
                if (StringUtils.isEmpty(agentConfig.getJarFile())) {
                    downloadUtils.updateJar();
                }
                log.info(JSONObject.toJSONString(agentConfig, true));
                VirtualMachine.attach(agentConfig.getPid()).loadAgent(agentConfig.getJarFile());
                System.err.println("agent is ok");
            } else {
                log.info("已加载");
            }
        } catch (Exception e) {
            log.error("AgentListener error:", e);
        }
    }

    public void upJarTask() {
        if (!agentConfig.getUpJarTask()) {
            return;
        }
        long delay = TimeUnit.MINUTES.toMillis(1);
        long period = TimeUnit.MINUTES.toMillis(10);

        new Timer("upJarTask").schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    downloadUtils.updateJar();
                } catch (Exception error) {
                    log.error("upJarTask", error);
                }
            }
        }, delay, period);
    }
}

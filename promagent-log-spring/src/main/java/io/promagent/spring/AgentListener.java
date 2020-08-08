package io.promagent.spring;


import com.alibaba.fastjson.JSONObject;
import com.sun.tools.attach.VirtualMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


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
                if (StringUtils.isEmpty(agentConfig.getJarFile())){
                    downloadUtils.updateJar();
                }
                log.info(JSONObject.toJSONString(agentConfig, true));
                VirtualMachine.attach(agentConfig.getPid()).loadAgent(agentConfig.getJarFile());
            } else {
                log.info("已加载");
            }
        } catch (Exception e) {
            log.error("AgentListener error:", e);
        }
    }
}

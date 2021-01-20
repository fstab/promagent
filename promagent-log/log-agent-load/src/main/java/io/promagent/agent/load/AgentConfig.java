package io.promagent.agent.load;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentConfig {

    private Hooks hooks = new Hooks();
    private FastHooks fastHooks = new FastHooks();

    private Agent agent = new Agent();
    private Load load = new Load();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Load {
        private long time = -1;
        private boolean result = false;
        private File agentJar = null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Hooks {
        private Map<String, List<String>> annMethodHook = new HashMap<>();
        private Map<String, List<String>> annClassHook = new HashMap<>();
        private Map<String, List<String>> regHook = new HashMap<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Agent {

        private String appName = "appName";
        private String appEvn = "prod";

        private boolean debug = false;
        private int retMaxLength = 20480;

        private String mdcLogId = "access_id";
        private String traceId = "X-REQUEST-ID";

        private String callClass = "io.promagent.agent.core.Logger";

        private List<String> headers = Arrays.asList("none");
        private List<String> ignoreSignatures = new ArrayList<>();
        private List<String> skipRetSignatures = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FastHooks {
        private String controllerPackage;
        private String scheduledPackage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgentConfigPrefix {
        private AgentConfig promagent;
    }
}



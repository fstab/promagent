package io.promagent.agent;

import com.sun.tools.attach.VirtualMachine;


public class AgentBootstrap {
    public static void main(String[] args) throws Exception {
        VirtualMachine.attach(args[0]).loadAgent(args[1]);
    }
}

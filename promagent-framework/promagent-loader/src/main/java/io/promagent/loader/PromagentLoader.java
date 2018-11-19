package io.promagent.loader;

import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class PromagentLoader {

    public static void main(String[] args) throws Exception {
        int pid = getIntArg(args, "-pid");
        int port = getIntArg(args, "-port");
        String agentJar = getStringArg(args, "-agent");
        PromagentLoader.loadPromagent(agentJar, pid, port);
    }

    private static void loadPromagent(String agentJar, int pid, int port) throws Exception {
        VirtualMachineDescriptor vmd = findVirtualMachine(Integer.toString(pid));
        if (vmd == null) {
            System.err.println("No Java process found with PID " + pid);
            System.exit(-1);
        }
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(vmd);
            vm.loadAgent(agentJar, "port=" + port);
        } catch (AgentLoadException e) {
            System.err.println("Failed to attach agent: " + getMessage(e));
        } finally {
            if (vm != null) {
                vm.detach();
            }
        }
    }

    private static VirtualMachineDescriptor findVirtualMachine(String pid) {
        for (VirtualMachineDescriptor vmd : VirtualMachine.list()) {
            if (vmd.id().equalsIgnoreCase(pid)) {
                return vmd;
            }
        }
        return null;
    }

    private static String getMessage(AgentLoadException e) {
        switch (e.getMessage()) {
            case "-4":
                return "Insuffient memory";
            case "100":
                return "Agent JAR not found or no Agent-Class attribute";
            case "101":
                return "Unable to add JAR file to system class path";
            case "102":
                return "Agent JAR loaded but agent failed to initialize";
            default:
                return e.getMessage();
        }
    }

    private static int getIntArg(String[] args, String option) {
        String stringArg = getStringArg(args, option);
        try {
            return Integer.parseInt(stringArg);
        } catch (NumberFormatException e) {
            System.err.println(option + " " + stringArg + ": invalid argument");
            System.exit(-1);
            return 0; // will never happen
        }
    }

    private static String getStringArg(String[] args, String option) {
        for (int pos : new int[]{0, 2, 4}) {
            if (args.length < pos + 2) {
                printUsageAndExit();
            }
            if (option.equals(args[pos])) {
                return args[pos+1];
            }
        }
        printUsageAndExit();
        return null; // will never happen
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: java -cp $JAVA_HOME/lib/tools.jar:/path/to/promagent-loader.jar io.promagent.loader.PromagentLoader -agent /path/to/promagent.jar -port 9300 -pid <pid>");
        System.exit(-1);
    }
}

package io.promagent.log.spring;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
public class ProcessUtils {

    private static String FOUND_JAVA_HOME = null;

    public static String findJavaHome() {
        String javaHome = System.getProperty("java.home");

        if (JavaVersionUtils.isLessThanJava9()) {
            File toolsJar = new File(javaHome, "lib/tools.jar");
            if (!toolsJar.exists()) {
                toolsJar = new File(javaHome, "../lib/tools.jar");
            }
            if (!toolsJar.exists()) {
                // maybe jre
                toolsJar = new File(javaHome, "../../lib/tools.jar");
            }

            if (toolsJar.exists()) {
                FOUND_JAVA_HOME = javaHome;
                return FOUND_JAVA_HOME;
            }

            if (!toolsJar.exists()) {
                log.debug("Can not find tools.jar under java.home: " + javaHome);
                String javaHomeEnv = System.getenv("JAVA_HOME");
                if (javaHomeEnv != null && !javaHomeEnv.isEmpty()) {
                    log.debug("Try to find tools.jar in System Env JAVA_HOME: " + javaHomeEnv);
                    // $JAVA_HOME/lib/tools.jar
                    toolsJar = new File(javaHomeEnv, "lib/tools.jar");
                    if (!toolsJar.exists()) {
                        // maybe jre
                        toolsJar = new File(javaHomeEnv, "../lib/tools.jar");
                    }
                }

                if (toolsJar.exists()) {
                    log.info("Found java home from System Env JAVA_HOME: " + javaHomeEnv);
                    FOUND_JAVA_HOME = javaHomeEnv;
                    return FOUND_JAVA_HOME;
                }

                throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome
                        + ", please try to start arthas-boot with full path java. Such as /opt/jdk/bin/java -jar arthas-boot.jar");
            }
        } else {
            FOUND_JAVA_HOME = javaHome;
        }
        return FOUND_JAVA_HOME;
    }

    public static void startAgent(List<String> attachArgs) {
        // find java/java.exe, then try to find tools.jar
        String javaHome = findJavaHome();

        // find java/java.exe
        File javaPath = findJava();
        if (javaPath == null) {
            throw new IllegalArgumentException("Can not find java/java.exe executable file under java home: " + javaHome);
        }

        File toolsJar = findToolsJar();

        if (JavaVersionUtils.isLessThanJava9()) {
            if (toolsJar == null || !toolsJar.exists()) {
                throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome);
            }
        }

        List<String> command = new ArrayList<>();
        command.add(javaPath.getAbsolutePath());

        if (toolsJar != null && toolsJar.exists()) {
            command.add("-Xbootclasspath/a:" + toolsJar.getAbsolutePath());
        }

        command.addAll(attachArgs);
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            final Process proc = pb.start();
            Thread redirectStdout = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    InputStream inputStream = proc.getInputStream();
                    try {
                        IOUtils.copy(inputStream, System.out);
                    } catch (IOException e) {
                        inputStream.close();
                    }

                }
            });

            Thread redirectStderr = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    InputStream inputStream = proc.getErrorStream();
                    try {
                        IOUtils.copy(inputStream, System.err);
                    } catch (IOException e) {
                        inputStream.close();
                    }

                }
            });
            redirectStdout.start();
            redirectStderr.start();
            redirectStdout.join();
            redirectStderr.join();

            int exitValue = proc.exitValue();
            if (exitValue != 0) {
                log.error("attach fail" + Arrays.toString(command.toArray()));
                System.exit(1);
            }
        } catch (Throwable e) {
            // ignore
        }

    }

    private static File findJava() {
        String javaHome = findJavaHome();
        String[] paths = {"bin/java", "bin/java.exe", "../bin/java", "../bin/java.exe"};

        List<File> javaList = new ArrayList<>();
        for (String path : paths) {
            File javaFile = new File(javaHome, path);
            if (javaFile.exists()) {
                log.debug("Found java: " + javaFile.getAbsolutePath());
                javaList.add(javaFile);
            }
        }

        if (javaList.isEmpty()) {
            log.debug("Can not find java/java.exe under current java home: " + javaHome);
            return null;
        }

        // find the shortest path, jre path longer than jdk path
        if (javaList.size() > 1) {
            Collections.sort(javaList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    try {
                        return file1.getCanonicalPath().length() - file2.getCanonicalPath().length();
                    } catch (IOException e) {
                        // ignore
                    }
                    return -1;
                }
            });
        }
        return javaList.get(0);
    }

    private static File findToolsJar() {
        if (JavaVersionUtils.isGreaterThanJava8()) {
            return null;
        }

        String javaHome = findJavaHome();
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(javaHome, "../lib/tools.jar");
        }
        if (!toolsJar.exists()) {
            // maybe jre
            toolsJar = new File(javaHome, "../../lib/tools.jar");
        }

        if (!toolsJar.exists()) {
            throw new IllegalArgumentException("Can not find tools.jar under java home: " + javaHome);
        }

        log.debug("Found tools.jar: " + toolsJar.getAbsolutePath());
        return toolsJar;
    }
}

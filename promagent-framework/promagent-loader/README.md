promagent-loader
----------------

Experimental tool for loading the promagent into an existing JVM. Usage:

```bash
java -cp $JAVA_HOME/lib/tools.jar:/path/to/promagent-loader.jar io.promagent.loader.PromagentLoader -agent /path/to/promagent.jar -port 9300 -pid <pid>
```

The JAR file `$JAVA_HOME/lib/tools.jar` must exist. It is part of JDK 8. With JDK 9 or higher, it might be possible to use the promagent-loader without this external dependency, because the classes were moved into the runtime.

The `/path/to/promagent.jar` must be an absolute path, not a relative path. 

The port is the TCP port for the exporter, like when promagent is attached on JVM startup with parameter `-javaagent:/path/to/promagent.jar=port=9300`.

The `<pid>` is the PID of the Java process that the agent should attach to. It can be found with the `jps` command.

See [https://github.com/raphw/byte-buddy/tree/master/byte-buddy-agent](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-agent) for a more portable implementation of an agent loader.

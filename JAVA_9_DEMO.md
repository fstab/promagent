Java 9
------

The following is a Java 9 proof of concept. It shows how to run the [Wildfly Demo](README.md) with Wildfly 11 using JDK9.

## Downloading and Compiling the Example Agent

The `promagent-example` is built the same way as described in [README.md](README.md):

Clone [Promagent](https://github.com/fstab/promagent) from GitHub:

```bash
git clone https://github.com/fstab/promagent.git
cd promagent
```

The `promagent-api` and `promagent-maven-plugin` are not on Maven Central yet. Run the following commands to make them available locally (in `~/.m2/repository/`):

```bash
cd promagent-framework
mvn clean install
cd ..
```

Compile the example agent. This should create the file `./promagent-example/target/promagent.jar`:

```bash
cd promagent-example
mvn clean verify
cd ..
```

## Running the Example with a Java EE Demo Application on Wildfly

Download and unzip the [Wildfly Quickstart](https://github.com/wildfly/quickstart) applications.

```bash
curl -LO https://github.com/wildfly/quickstart/files/1257979/wildfly-11.0.0.CR1-quickstarts.zip
unzip wildfly-11.0.0.CR1-quickstarts.zip
cd wildfly-11.0.0.CR1-quickstarts/kitchensink/
```

Two changes to kitchensink's `pom.xml` are needed to make it compile with JDK9:

1. `kitchensink` uses the `maven-enforcer-plugin` to ensure Java 1.7 or greater is used. This check fails with Java 9. Add the following to the `<build>` section in kitchensink's `pom.xml` to disable the `enforce-java-version` execution:
   
   ```xml
   <plugins>
       <plugin>
           <artifactId>maven-enforcer-plugin</artifactId>
           <executions>
               <execution>
                   <id>enforce-java-version</id>
                   <phase>none</phase>
               </execution>
           </executions>
       </plugin>
   </plugins>
   ```
   
2. The JAXB APIs are no longer contained on the default class path in Java 9. Add it explicitly to the `<dependencies>` section in kitchensink's `pom.xml`:
   
   ```xml
   <dependency>
       <groupId>javax.xml.bind</groupId>
       <artifactId>jaxb-api</artifactId>
       <version>2.3.0</version>
   </dependency>
   ```

Now `kitchensink` should build successfully with JDK9.

```bash
mvn clean package
cd ../..
```

Download and extract the [Wildfly application server](http://wildfly.org/).

```bash
curl -O http://download.jboss.org/wildfly/11.0.0.CR1/wildfly-11.0.0.CR1.tar.gz
tar xfz wildfly-11.0.0.CR1.tar.gz
cd wildfly-11.0.0.CR1
```

Run the Wildfly application server with the Promagent attached.

```bash
LOGMANAGER_JAR=$(find $(pwd) -name 'jboss-logmanager-*.jar')
export JAVA_OPTS="
    -Xbootclasspath/a:${LOGMANAGER_JAR}
    -Dsun.util.logging.disableCallerCheck=true
    -Djboss.modules.system.pkgs=org.jboss.logmanager,io.promagent.agent
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager
    -javaagent:../promagent/promagent-dist/target/promagent.jar=port=9300
    ${JAVA_OPTS}
"
./bin/standalone.sh
```

In a new Shell window, deploy the quickstart application.

```bash
cd wildfly-11.0.0.CR1
./bin/jboss-cli.sh --connect --command="deploy ../wildfly-11.0.0.CR1-quickstarts/kitchensink/target/kitchensink.war"
```

Go to [http://localhost:8080/kitchensink](http://localhost:8080/kitchensink) to view the quickstart application,
go to [http://localhost:9300/metrics](http://localhost:9300/metrics) to view the Prometheus metrics.

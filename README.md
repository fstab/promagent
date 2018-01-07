[![Build Status](https://travis-ci.org/fstab/promagent.svg?branch=master)](https://travis-ci.org/fstab/promagent)

Promagent
=========

_Prometheus Monitoring for Java Web Applications without Modifying their Source Code._

Promagent is a framework for creating Java agents providing [Prometheus](https://prometheus.io/) metrics for Java Web applications without modifying the applications' source code.
Promagent uses the [Byte Buddy](http://bytebuddy.net/) bytecode manipulation library to insert Prometheus metrics during application startup.

The Promagent code repository contains two projects:

- `promagent-framework`: Provides, among other things, the `promagent-api` and the `promagent-maven-plugin` helping you to create your own agents.
- `promagent-example`: An agent providing example metrics for Spring and Java EE applications:
  - HTTP: Number and duration of web requests.
  - SQL: Number and duration of database queries.

The example agent was tested with [Spring Boot](https://projects.spring.io/spring-boot/) and with the [Wildfly application server](http://wildfly.org/).

Example
-------

![screenshot](screenshot.png)

Some example Prometheus queries evaluating Promagent metrics:

* Top five most frequent SQL queries: `topk(5, sql_queries_total)`.
* Top five longest-running HTTP requests (median): `topk(5, http_request_duration{quantile="0.5"})`.
* If a database query is triggered within the context of an HTTP request, the HTTP `path` is added as a label to the `sql_*` metrics. That way, we can relate database queries to the corresponding HTTP requests:
  * Time ratio spent on the database when processing an HTTP request: `sum(sql_query_duration{quantile="0.5"}) by (query,method,path) / on(method,path) group_left sum(http_request_duration{quantile="0.5"}) by (method,path)`.
  * Which database queries are triggered how often for which HTTP request: `sum(sql_queries_total) by (method, path, query) / on(method, path) group_left sum(http_requests_total) by (method, path)`.

### Downloading and Compiling the Example Agent

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

### Running the Example with a Spring Boot Demo Application

_The following runs with Java 8 and was not tested with Java 9 yet._

Download and compile a [Spring Boot Getting Started](https://spring.io/guides/gs/accessing-data-rest/) application.

```bash
git clone https://github.com/spring-guides/gs-accessing-data-rest.git
cd gs-accessing-data-rest/complete
mvn clean package
cd ../..
```

Run the Spring Boot application with the Promagent attached.

```bash
java \
    -javaagent:promagent/promagent-example/target/promagent.jar=port=9300 \
    -jar gs-accessing-data-rest/complete/target/gs-accessing-data-rest-0.1.0.jar
```

Go to [http://localhost:8080](http://localhost:8080) to view the Spring Boot application,
go to [http://localhost:9300/metrics](http://localhost:9300/metrics) to view the Prometheus metrics.

### Running the Example with a Java EE Demo Application on Wildfly

_This demo runs with Java 8. For a Java 9 version, see [JAVA_9_DEMO.md](JAVA_9_DEMO.md)._

Download and compile a [Wildfly Quickstart](https://github.com/wildfly/quickstart) application.

```bash
git clone https://github.com/wildfly/quickstart.git
cd quickstart/kitchensink
mvn clean package
cd ../..
```

Download and extract the [Wildfly application server](http://wildfly.org/).

```bash
curl -O http://download.jboss.org/wildfly/10.1.0.Final/wildfly-10.1.0.Final.tar.gz
tar xfz wildfly-10.1.0.Final.tar.gz
```

Run the Wildfly application server with the Promagent attached.

```bash
cd wildfly-10.1.0.Final
LOGMANAGER_JAR=$(find $(pwd) -name 'jboss-logmanager-*.jar')
export JAVA_OPTS="
    -Xbootclasspath/p:${LOGMANAGER_JAR}
    -Djboss.modules.system.pkgs=org.jboss.logmanager,io.promagent.agent
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager
    -javaagent:../promagent/promagent-example/target/promagent.jar=port=9300
"
./bin/standalone.sh
```

In a new Shell window, deploy the quickstart application.

```bash
cd wildfly-10.1.0.Final
./bin/jboss-cli.sh --connect --command="deploy ../quickstart/kitchensink/target/kitchensink.war"
```

Go to [http://localhost:8080/kitchensink](http://localhost:8080/kitchensink) to view the quickstart application,
go to [http://localhost:9300/metrics](http://localhost:9300/metrics) to view the Prometheus metrics.

Creating your Own Agent
-----------------------

A Promagent is implemented as a set of Hooks. A Hook is a Java class meeting the following requirements:

* The class is annotated with `@Hook`.
* The class has a public constructor taking a single parameter of type `MetricsStore`.
* The class provides methods annotated with `@Before` or `@After`. Those methods must take exactly the same parameters as the method you want to intercept.

The best way to get started is to have a look at the `ServletHook` and `JdbcHook` in the `promagent-example`.

A simple Hook counting the number of Servlet requests looks as follows:

```java
@Hook(instruments = "javax.servlet.Servlet")
public class ServletHook {

    private final Counter servletRequestsTotal;

        public ServletHook(MetricsStore metricsStore) {
    
            servletRequestsTotal = metricsStore.createOrGet(new MetricDef<>(
                    "servlet_requests_total",
                    (name, registry) -> Counter.build()
                        .name(name)
                        .help("Total number of Servlet requests.")
                        .register(registry)
            ));
        }

        @After(method = "service")
        public void after(ServletRequest request, ServletResponse response) {
            httpRequestsTotal.inc();
        }
}
```

A Promagent needs two entries in the `pom.xml`. First, the `promagent-api` must be included as a dependency:

```xml
<dependency>
    <groupId>io.promagent</groupId>
    <artifactId>promagent-api</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope> <!-- will be made available by the promagent-maven-plugin -->
</dependency>
```

Second, the `promagent-maven-plugin` that creates an agent JAR:

```xml
<build>
    <finalName>promagent</finalName>
    <plugins>
        <plugin>
            <groupId>io.promagent</groupId>
            <artifactId>promagent-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <id>promagent</id>
                    <phase>package</phase>
                    <goals>
                        <goal>build</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

With these two things included, `mvn clean package` should produce a working Java agent in `target/promagent.jar`.

### A Hook's Life Cycle

The Promagent framework creates a new Hook instance for each HTTP request. While this sounds simple, it's a bit tricky in the details, because multiple Servlet calls may be involved fulfilling a single HTTP request (one Servlet calling another Servlet). The exact mechanism is as follows:

* When an instrumented method is called for the first time, a new Hook instance is created. The Hook instance is stored in a [ThreadLocal](https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html).
* For nested calls within the same thread, the instance from the ThreadLocal is re-used.
* When the outermost call is done (leaving the `@After` method of the outermost call), the Hook instance is removed from the ThreadLocal and discarded. For the next call, a new instance will be created.

The simple `ServletHook` above would count nested Servlet calls, the value of `servlet_requests_total` might be higher than the actual number of HTTP requests.
The `ServletHook` in the `promagent-example` project prevents this by tracking the current stack depth, so the `http_requests_total` metric counts the actual number of HTTP requests.
Future Promagent versions will provide a more flexible `@Hook` annotation so users can configure if the Hook should be invoked for nested calls or not.

### The Hook's Constructor Parameter

Most applications use static variables to maintain Prometheus metrics, as described in the [Prometheus Client Library for Java](https://github.com/prometheus/client_java) documentation:

```java
# Doesn't work with Promagent
static final Counter counter = Counter.build()
    .name("requests_total")
    .help("Total requests.")
    .register();
```

Unfortunately, static variables are maintained per deployment in an application server. When an application is re-deployed, a new instance of the same `Counter` is created, which causes conflicts in the Prometheus registry. It is also impossible to instrument a mix of internal modules (like an internal Servlet in the JAX-RS implementation) and deployments (like Servlets in a WAR file) that way.

To prevent this, Promagent requires Hooks to use the `MetricsStore` to maintain metrics:

```java
# This is the correct way with Promagent
Counter counter = metricsStore.createOrGet(new MetricDef<>(
                    "requests_total",
                    (name, registry) -> Counter.build()
                        .name(name)
                        .help("Total requests.")
                        .register(registry)
            ));
```

The Promagent library will take care that the `Counter` is created only once, and that the `Counter` instance is re-used across multiple deployments and internal modules in an application server.

### Hook Annotations

* `@Hook`: Hooks are annotated with the `@Hook(instruments = {...})` annotations. The `instruments` parameter takes a list of Strings specifying the names of the classes or interfaces to be instrumented, like `{"javax.servlet.Servlet", "javax.servlet.Filter"}`. The Hook instruments not only the classes or interfaces themselves, but all sub-classes or implementations of these classes or interfaces.
* `@Before`: Hook methods annotated with `@Before(method = {...})` are invoked when an instrumented method is entered. The `methods` parameter takes a list of Strings specifying the names of the intercepted methods, like `{"service", "doFilter"}`. The number and types of arguments are derived from the method itself, i.e. the Hook method annotated with `@Before` must take the exact same parameters as the methods it wants to instrument.
* `@After`: Hook methods annotated with `@After(method = {...})` are invoked when an instrumented method is left. `@After` methods are always called, even if the instrumented method terminates with an Exception. The semantics is the same as with the `@Before` annotation. If a Hook is only interested in the number of calls but not in the duration, it is sufficient to implement either only a `@Before` method or only an `@After` method and omit the other.
* `@Returned`: It might be useful to learn the return value of an instrumented method. In order to do so, methods annotated with `@After` may have an additional parameter annotated with `@Returned`, where the type corresponds to the return type of the intercepted method. If the instrumented method returns regularly, the return value is provided. If the method returns exceptionally, `null` (or the default type for primitive types, like `0` for `int`) is provided. `@Returned` parameters are only allowed in `@After` methods, not in `@Before` methods.
* `@Thrown`: This is like `@Returned`, but to learn an Exception thrown out of an instrumented method. The type should be `Throwable` to avoid class cast errors on unexpected `RuntimeException` or `Error`. If the instrumented method does not throw an exception, the parameter annotated with `@Thrown` will be `null`.

### Using Labels

The Prometheus server internally stores one time series for each observed set of label values. The time series database in the Prometheus server can easily handle thousands of different time series, but millions of different time series could be a problem.
Therefore, it is important to keep the number of different label values relatively small. Unique user IDs, timestamps, or session keys should not be used as label values.

The `promagent-example` strips HTTP URLs and SQL queries to make sure that there are not too many different label values:

* For HTTP requests, path parameters are replaced with placeholders. The goal is to use REST resources like `/item/{id}` as labels, not in actual paths like `/item/123`.
* For SQL queries, values are stripped. The goal is to use the structure of the query like `INSERT INTO ITEMS (ID, NAME, PRICE) VALUES (...)` as a label, not in its values like `INSERT INTO ITEMS (ID, NAME, PRICE) VALUES (23, 'abc', 17.5)`.

Of course, replacing path parameters and SQL values is application specific. The `promagent-example` implements a very simple replacement in `ServletHook.stripPathParameters()` and `JdbcHook.stripValues()`, but you probably need to customize these methods for your application.

### Running Docker Tests

The `promagent-example` project contains an alternative Maven configuration in `pom-with-docker-tests.xml`.
This configuration uses the `docker-maven-plugin` to create Docker images and run integration tests against Docker containers.

The Wildfly tests can be run as follows:

```bash
cd promagent-example
mvn -f pom-with-docker-tests.xml clean verify -Pwildfly
cd ..
```

The Spring Boot tests can be run as follows:

```bash
cd promagent-example
mvn -f pom-with-docker-tests.xml clean verify -Pspring
cd ..
```

The first run takes a while, because the Docker images need to be built. Once the images are available on the local systems, runs are significantly faster.

Exposing Metrics
----------------

Promagent supports three different ways of exposing metrics to the Prometheus server:

* The agent has a built-in HTTP server. This is used in the examples above. The server is started when the
  command line argument `port` is used, as for example `-javaagent:agent.jar=host=localhost,port=9300`.
  The `host` argument is optional, it defaults to the wildcard IP address.
  If `port` is omitted the built-in server is not started.
* The [promagent-exporter](https://github.com/fstab/promagent/tree/master/promagent-framework/promagent-exporter) module implements
  a simple Web application in WAR file format. If you deploy the _promagent-framework/promagent-exporter/target/promagent.war_ on your
  server, it will collect Promagent metrics via JMX and expose them under its deployment URL,
  like [http://localhost:8080/promagent](http://localhost:8080/promagent).
* All metrics are made available via JMX, so any JMX client can be used to access the metrics.

Status
------

This is a demo project. The main goal is to learn the internals of bytecode manipulation and class loading in Java application servers. I am planning to work on the following:

* Try it with more application servers ([Payara](http://www.payara.fish/), [TomEE](http://tomee.apache.org/)) and adjust the code if necessary.
* Write documentation about the internal implementation, mainly the bytecode manipulation and class loading aspects.
* Generalize the concept so that users can not only write Hooks, but also other collectors. A proof-of-concept is includes in the class `JmxCollector` in `promagent-example`.
* Generalize the concept so we don't only support monitoring with Prometheus, but also tracing with OpenTracing API compatible tools.

The `promagent-api` and `promagent-maven-plugin` are not yet available on Maven Central, but they will be uploaded when the API becomes a bit more stable.

If you want to write your own agent and are looking for examples of methods you might want to instrument, look at related projects, like [inspectIT](http://www.inspectit.rocks/) (hooks are configured [here](https://github.com/inspectIT/inspectIT/tree/master/inspectit.server/src/main/external-resources/ci/profiles/common)) or [stagemonitor](http://www.stagemonitor.org/).

Resources
---------

* [Prometheus Monitoring for Java Web Applications w o Modifying Source Code ](https://www.youtube.com/watch?v=BjyI93c8ltA) (youtube.com), video from my Devoxx 2017 talk.
* [Instrumenting Java Web Applications without Modifying their Source Code](https://www.javacodegeeks.com/2017/07/instrumenting-java-web-applications-without-modifying-source-code.html) (Java Code Geeks).

Thank You ConSol
----------------

This project is supported as part of the R&D activities at [ConSol Software GmbH](https://www.consol.de/). See the [ConSol Labs Blog](https://labs.consol.de/) for more info.

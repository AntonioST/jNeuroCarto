jNeuroCarto Application packaging
=================================

This document teach you how to include your module into `jneurocarto` application.

## Prepare

### Prepare environment.

Require `Java` and `maven`.

### Create module

Your library should include `module-info.java`. For example:

```java
module your.library {

    requires java.desktop;
    
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.javafx;
    requires io.ast.jneurocarto.javafx.chart;

    exports your.library;
    provides ProbeProvider with MyProbeProvider;
    provides PluginProvider with MyPluginProvider;
}
```

Examples:

* [jNeuroCarto-probe-npx](../jNeuroCarto-probe-npx) Neuropixels extension
* [jNeuroCarto-probe-npx-javafx](../jNeuroCarto-probe-npx-javafx) Neuropixels plugins for application

#### Used commandline framework

- `org.jspecify` null annotations
- `org.slf4j` logging framework
- `info.picocli` command line interface
- `com.fasterxml.jackson.core` json read/write

Commandline Examples:

* [Main.java](../jNeuroCarto-probe-npx/src/main/java/io/ast/jneurocarto/probe_npx/cli/Main.java)

### install

Install your library into local repository

```shell
mvn clean install
```

## Packaging

You have to modify the [pom.xml](pom.xml).

### dependency

Add your library as dependency.

```xml

<dependencies>
    <!-- add below -->
    <dependency>
        <groupId>...</groupId>
        <artifactId>your.library</artifactId>
        <version>...</version>
    </dependency>
    <!-- add above -->
</dependencies>
```

### launcher

If you have some commandline utilities, and you want it to be packaged together,
you have to add a launcher.

Prepare a `launcher-your-command.properties`,

```properties
module=your.library/your.library.cli.Main
description="your utilities"
```

and add it into [pom.xml](pom.xml).

```xml

<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <executions>
                <id>jpackage</id>
                <configuration>
                    <executable>jpackage</executable>
                    <arguments>
                        <!-- add below -->
                        <argument>--add-launcher</argument>
                        <argument>your-command=launcher-your-command.properties</argument>
                        <!-- add above -->
                    </arguments>
                </configuration>
            </executions>
        </plugin>
    </plugins>
</build>
```

Examples:

* [launcher-probe-npx.properties](launcher-probe-npx.properties)

## Install

```shell
mvn clean install
```

If you create your module under `jNeuroCarto` root pom, you have to add your project, likes

```shell
mvn -pl your-project clean install
```

The application will be packaged into `target/dist/jneurocarto/`.
You can copy this directory to anywhere.


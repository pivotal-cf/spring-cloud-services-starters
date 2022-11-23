= Spring Cloud Services Starters

Spring Cloud Services Starters are a curated set of dependencies for use with link:https://docs.pivotal.io/spring-cloud-services/index.html[Spring Cloud Services] in a link:https://pivotal.io/platform[Pivotal Cloud Foundry] environment.

:toc:
:toc-placement!:

toc::[]

== Features

Easily take advantage of the various services by including the BOM and corresponding starter. See the example Maven POM and Gradle build files below.

* spring-cloud-services-starter-config-client
* spring-cloud-services-starter-service-registry

== Build and Deploy

The release artifacts are available from Maven Central. You may also build and install the starter POMs to your local Maven repository:

----
./gradlew clean build publishToMavenLocal
----

== Usage

Include the BOM and starter dependencies in your project using Maven or Gradle.

== Compatibility Matrix

[cols="1,1,1"]
|===
| SCS Starters | Spring Cloud | Spring Boot

| 3.5.x | 2021.0.x | 2.7.x
| 3.4.x | 2021.0.x | 2.6.x
| 3.3.x | 2020.0.x | 2.5.x
|===

=== Maven BOM

`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="https://maven.apache.org/POM/4.0.0" xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="https://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.demo</groupId>
    <artifactId>spring-cloud-services-demo</artifactId>
    <version>0.1.0</version>
    <packaging>jar</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>${spring-boot.version}</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.pivotal.spring.cloud</groupId>
            <artifactId>spring-cloud-services-starter-config-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.pivotal.spring.cloud</groupId>
            <artifactId>spring-cloud-services-starter-service-registry</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.pivotal.spring.cloud</groupId>
                <artifactId>spring-cloud-services-dependencies</artifactId>
                <version>${spring-cloud-services.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-repository</id>
            <url>https://repo.spring.io/libs-release</url>
        </repository>
    </repositories>

    <pluginRepositories>
        <pluginRepository>
            <id>spring-plugins</id>
            <url>https://repo.spring.io/plugins-release</url>
        </pluginRepository>
        <pluginRepository>
            <id>spring-repository</id>
            <url>https://repo.spring.io/libs-release</url>
        </pluginRepository>
    </pluginRepositories>

</project>
```

=== Gradle

`build.gradle`

```groovy
buildscript {
    repositories {
        mavenCentral()
        maven { url "https://repo.spring.io/plugins-release/" }
        maven { url "https://repo.spring.io/libs-release/" }
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
        classpath("io.spring.gradle:dependency-management-plugin:0.6.0.RELEASE")
    }
}

apply plugin: "java"
apply plugin: "eclipse"
apply plugin: "idea"
apply plugin: "spring-boot"
apply plugin: "io.spring.dependency-management"

jar {
    baseName = "spring-cloud-services-demo"
    version =  "0.1.0"
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        mavenBom "io.pivotal.spring.cloud:spring-cloud-services-dependencies:${springCloudServicesVersion}"
    }
}

dependencies {
    compile("io.pivotal.spring.cloud:spring-cloud-services-starter-config-client")
    compile("io.pivotal.spring.cloud:spring-cloud-services-starter-service-registry")
}

repositories {
    mavenCentral()
    maven { url "https://repo.spring.io/libs-release/" }
}
```

== License

Spring Cloud Services Starters is Open Source software released under the
https://www.apache.org/licenses/LICENSE-2.0.html[Apache 2.0 license].

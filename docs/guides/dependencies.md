# Managing Dependencies

---

## Overview

In your Helidon Talon application you will use three BOM files to manage the versions of direct and
transitive dependencies. These BOM files are:

| BOM File                 | Description                                                                                     |
|--------------------------|-------------------------------------------------------------------------------------------------|
| helidon-oci-internal-bom | Talon's version of `oci-internal-bom`. Manages versions of com.oracle.pic dependencies          |
| helidon-oci-dependencies | Manages versions of io.helidon, com.oracle.helidon.oci, and associated third party dependencies |
| dropwizard-service-bom   | Manages versions of third party dependencies not covered by the first two BOMs                  |

## Using The BOM Files

You use the BOM files by importing them into your application's `dependencyManagement` section. The order of the
imports is important. 

``` xml
    <dependencyManagement>
        <dependencies>
            <!-- The order of these imports is important. First has precedence over later. -->
            <!-- Manage versions of com.oracle.pic artifacts -->
            <dependency>
                <groupId>com.oracle.helidon.oci</groupId>
                <artifactId>helidon-oci-internal-bom</artifactId>
                <version>${helidon.oci.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Manage versions of io.helidon, com.oracle.helidon and associated third party artifacts -->
            <dependency>
                <groupId>com.oracle.helidon.oci</groupId>
                <artifactId>helidon-oci-dependencies</artifactId>
                <version>${helidon.oci.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Manage versions of additional OCI third party artifacts -->
            <dependency>
                <groupId>com.oracle.pic.commons</groupId>
                <artifactId>dropwizard-service-bom</artifactId>
                <version>${version.oci.dropwizard.bom}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

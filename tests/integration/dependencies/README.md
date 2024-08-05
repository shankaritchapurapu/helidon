# Dependencies Tests

## Overview

Ensures the integrity of the dependencies from dependency management.

## Usage

From the top level of the project:

```agsl
cat dependencies/pom.xml | etc/scripts/depm2dep.sh
```

Then update [pom.xml](pom.xml) with the output.

Also, be sure to update this if a new versioned property is being added to dependency management:

```xml
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>versions-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>install</phase>
                        <goals>
                            <goal>display-property-updates</goal>
                        </goals>
                        <configuration>
                            <!-- TODO: the properties that are commented out cause the versions plugin to crash - research this -->
                            <includeProperties>
                                helidon.version,
                                oci-java-sdk-version,
                                oracle-internal-authproxy-filter-version,
                                oracle-internal-commons-metrics-lib-version,
                                oracle-internal-commons-metrics-module-version,
                                oracle-internal-commons-metrics-reporter-version,
                                oracle-internal-commons-metrics-version,
                                oracle-internal-commons-request-id-version,
                                oracle-internal-exception-mapping-version,
                                oracle-internal-identity-common-version,
                                oracle-internal-identity-version,
                                oracle-internal-logging-version,
                                oracle-internal-splat-sdk-version,
                                oracle-internal-vault-java-sdk-version,
                                oracle-internal-vault-user-api-spec-version,
                                oracle-internal-vault-user-java-client-version,
                                oracle-internal-secret-service-cp-java-client-version,
```

## Build

```shell
mvn clean package
```
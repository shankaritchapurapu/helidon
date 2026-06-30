# OCI OpenAPI Generation With Splat Validation

---

## Overview

The OCI OpenAPI generation guide documents the supported build-time flow for OCI services that need both
Splat/RQS API specification validation and generated Helidon resources. It composes the Splat-owned
`com.oracle.pic.platform.splat:splat-swagger-maven-plugin` with the Helidon Extensions OpenAPI generator through
`openapi-generator-maven-plugin` and `generatorName` `helidon-declarative`.

OCI services that onboard to Splat/RQS still need a Splat-facing Swagger 2 API specification with the required
`x-obmcs-splat` and RQS metadata. The same Swagger 2 file can be used as the source for Helidon resource generation;
OpenAPI Generator parses the Swagger 2 source into the OpenAPI model used by the Helidon generator.

There is no separate custom OCI OpenAPI generator module for this path, and this page does not describe a runtime
Helidon Talon service integration.

---

## Boundary

The Splat Maven plugin validates the Splat-facing API specification and
downstream specification during the build. It does not generate Helidon Java
resources.

The Helidon Extensions OpenAPI generator generates the Helidon resources. It
does not perform Splat validation, does not submit the spec to Splat, does not
publish an RQS schema, and does not make runtime calls to Splat or RQS.

For valid specs, Splat/RQS metadata affects the generated OpenAPI artifact, not
the generated Java resource behavior. The runtime `/openapi` endpoint is served
by Helidon OpenAPI when the generated `META-INF/openapi.yaml` is on the
application classpath.

---

## Maven Usage

Manage plugin versions in parent plugin management. The example parent manages
both `openapi-generator-maven-plugin` and `splat-swagger-maven-plugin`.

Use the Splat plugin to validate the Splat-facing Swagger 2 file and downstream
specification:

The example filters `src/main/splat/splat-validation.conf` into
`target/generated-splat` before the Splat plugin runs so the config contains
absolute module paths and can be run from the repository root.

```xml
<plugin>
    <groupId>com.oracle.pic.platform.splat</groupId>
    <artifactId>splat-swagger-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>validate-splat-template</id>
            <phase>compile</phase>
            <goals>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <disable>false</disable>
        <outputFileLocation>${project.build.directory}/splat-spec-validation-output.txt</outputFileLocation>
        <configs>
            <param>${project.build.directory}/generated-splat/splat-validation.conf</param>
        </configs>
    </configuration>
</plugin>
```

Use the Helidon Extensions generator to generate Helidon resources from the same
Swagger 2 file:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generate-helidon-resources</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>helidon-declarative</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/swagger/robot-api.yaml</inputSpec>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <configOptions>
                    <apiPackage>com.example.generated.api</apiPackage>
                    <modelPackage>com.example.generated.model</modelPackage>
                    <invokerPackage>com.example.generated</invokerPackage>
                    <helidonVersion>${helidon.version}</helidonVersion>
                    <generateClient>false</generateClient>
                    <generateErrorHandler>false</generateErrorHandler>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.helidon.extensions.openapi-generator</groupId>
            <artifactId>helidon-extensions-openapi-generator</artifactId>
            <version>${version.helidon.extensions.openapi-generator}</version>
        </dependency>
    </dependencies>
</plugin>
```

---

## Example

See [examples/oci-openapi-generator](../../examples/oci-openapi-generator/README.md)
for a smoke-test example that:

* Keeps `robot-api.yaml` as the Splat-facing Swagger 2 source.
* Provides example Splat validation config and downstream spec files.
* Generates Helidon resources with `helidon-declarative`.
* Starts the generated application and verifies the generated routes with HTTP
  requests.
* Verifies that the generated `/openapi` document still contains the Splat/RQS
  metadata.

Run the generation and HTTP tests from the repository root:

```bash
mvn -pl examples/oci-openapi-generator -am test
```

The same command also runs the Splat validation plugin before the HTTP tests.
To run only generation, compilation, and Splat validation:

```bash
mvn -pl examples/oci-openapi-generator -am compile
```

---

## References

* [Helidon Extensions OpenAPI Generator](https://github.com/helidon-io/helidon-extensions/tree/main/extensions/openapi-generator)
* [Splat Maven Plugin for Specification Validation](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/splat/reference/splat-maven-plugin-for-spec-validation.htm)
* [Splat Swagger API Specification Extensions](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/splat/reference/splat-swagger-api-spec-extns.htm)
* [Splat Downstream Specification](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/splat/reference/splat-downstr-spec.htm)
* [Enabling RQS Automation](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/splat/features/how-to-enable-rqs-automation.htm?Highlight=RQS)
* [Author RQS Schema](https://confluence.oraclecorp.com/confluence/display/OCIQP/Author+RQS+schema)
* [Splat Platform - Support for RQS Integration](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/Splat+Platform+-+Support+for+RQS+integration)
* [Task Template for Onboarding to RQS Automation](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/Task+template+for+onboarding+to+RQS+automation)

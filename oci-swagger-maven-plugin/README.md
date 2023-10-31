# Oracle Swagger Code Generation Maven Plugin

This Maven plugin can be used to generate Helidon MP service and model code based on Swagger 2.0 Specifications.

The model classes are based entirely on the Public SDK code generator, and will look like what our public Java SDK users would see.

The Swagger parser and generator use the Public SDK generator and add a few extra things necessary for service stub generation.

# How do I generate code?

Here's a high level diagram of how Swagger code generation works:

    +-------------------+     +---------------+     +---------------+
    | Swagger Spec File +---> | YAML Jackson  +---> | Swagger Model |
    +-------------------+     | Object Mapper |     +------+--------+
                              +---------------+            |
                                                           |
                                                           v
                                                 1. Convert spec model
       +----------------------------------+      to codegen input   +------------------+
       |                                  | <-----------------------+                  |
       | OracleCodegenOrchestratorInput   |                         |   Public SDK     |
       |                                  |                         |    Generator     |
       |                                  | <-----------------------+                  |
       +----------------------------------+   2. Get API and Model  +--+---------------+
                                              templates to send        |
                                              to template engine       | 3. Pass Codegen model objects
                                                                       |    to templating engine with
                                                                       |    templates to process
                                                                       v
                            +-------------+             +-------------------+
                            | Model Files | <-----------+                   |
                            +-------------+             | Mustache template |
                              +-----------+             |      engine       |
                              | API Files | <-----------+                   |
                              +-----------+             +-------------------+


The Swagger (OpenAPI) generation can be configured by using the `helidon-oci-swagger-maven-plugin` with `oracle-java-helidon-service` set as the language.

```xml
            <plugin>
                <groupId>com.oracle.helidon.oci</groupId>
                <artifactId>helidon-oci-swagger-maven-plugin</artifactId>
                <version>${helidon-oci-swagger-maven-plugin.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <basePackage>com.oracle.test</basePackage>
                            <language>oracle-java-helidon-service</language>
                            <specPath>reference-spec.yaml</specPath>
                            <additionalProperties>
                                <!-- Telling SDK and generator to use Jakarta namespace or not -->
                                <useJakartaAnnotations>true</useJakartaAnnotations>
                                <!-- This is shared between SDK model and our service gen -->
                                <enableValidation>true</enableValidation>
                                <!-- Same as default set in code but still setting here as reference -->
                                <contextsIdentity>false</contextsIdentity>
                                <contextsJaxRsHttpHeaders>false</contextsJaxRsHttpHeaders>
                                <contextsJaxRsUriInfo>false</contextsJaxRsUriInfo>
                                <contextsJaxRsRequest>false</contextsJaxRsRequest>
                                <contextsJaxRsSecurityContext>false</contextsJaxRsSecurityContext>
                                <contextsAuthProxyIdentity>false</contextsAuthProxyIdentity>
                                <contextsAuditPayloadAppender>false</contextsAuditPayloadAppender>
                                <contextsContainerRequest>false</contextsContainerRequest>
                                <contextsContainerRequest>false</contextsContainerRequest>
                                <useJaxRsServiceResponse>false</useJaxRsServiceResponse>
                            </additionalProperties>
                            <project/>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

# Config Options

### Oracle Swagger Maven Plugin Configuration
The `helidon-oci-swagger-maven-plugin` has the following configuration options:

- language (defaults to oracle-java-helidon-service)- Generator to use. Must be set to `oracle-java-helidon-service` to generate Helidon MP sources.
- specPath (defaults to src/main/resources/swagger.yaml) - The path to the swagger specification file.
- outputDir (defaults to `${project.build.directory}/generated-sources`) - The directory to which the sources will be written.
- basePackage (Required) - The base package for all generated sources. The api files will be written to the `${basePackage}.api` and models to the `${basePackage}.model` packages.
- additionalProperties - Helidon generator-specific properties. See below for available values.
- importMappings - The swagger import mappings parameter. It is highly unlikely that you need to modify this parameter.

### Model Configuration options
Models are generated by the public SDK. You can find out what options there are available by looking at their options:
https://bitbucket.oci.oraclecorp.com/projects/SDK/repos/bmc-sdk-swagger/browse/bmc-sdk-swagger/src/main/java/com/oracle/bmc/sdk/swagger/codegen/OracleJavaSdkCodegen.java#60

By default, Helidon service codegen:
- Enables 'enableDefaultDiscriminatorOnlyElseNone' - this is used to prevent defaulting of polymorphic types to a specific class type unless the spec explicitly says there's a default (for backwards compat).
- Disables 'useExplicitlySetFilter' - this is an annotation for how to process serialization.
- Sets 'loggerName' to "log" - this is used by lombok to generate the logger name.
- Enables 'enableValidation' - this adds all the validation annotations to the models and service stubs.
- Enables 'enableEnumFromString' - this allows to deserialize enums with underscores correctly.

### Helidon Service Configuration options

Service options can be set by specifying them as additional properties in the configuration, for example:

```xml
<additionalProperties>
    <enableValidation>false</enableValidation>
</additionalProperties>
```
The generator supports these additional properties:

- 'useJakartaAnnotations' (Required) - Tells SDK and generator to use jakarta namespaces or not. Set to false for Helidon 2.x and true for Helidon 3.x onwards.
- 'enableValidation' (default true) - same as models, it will enable all the validation annotations
- 'contextsIdentity' (default false) - adds the OCI Identity args (auth context and principal) to every api
- 'contexts*' (default false) - these are various other flags to inject other different types of contexts, see OracleJavaHelidonServiceCodegen.ConfigOption enum for more info.
- 'useJaxRsServiceResponse' (default false) - whether to wrap all return types of all operations in `jakarta.ws.rs.core.Response` objects.

The generated service stubs currently result in an abstract classes you can extend from.  It is recommened you extend from 'Abstract<yourservice>BaseResource'.  This is the latest version that allows you the most ability to customize the injected contexts.

# Helidon MP Service Generation with Pegasus

This module demonstrates a Helidon MP based application and is composed of the following:

1. Server-side source code that will be generated using Oracle BMC Swagger tool based on an OpenApi specification document in the
   form of [reference-spec.yaml](./reference-spec.yaml) as an input. The generated source code will be located
   in
   target.generated-sources.oracle-java-helidon-service-sources.com.oracle.tests.integration and will have the following
   components:
    * A JAX-RS base abstract class
    * A model source code that will represent a JSON object
2. A server application that will implement the generated JAX-RS base abstract class to provide a simple implementation.
3. It also showcases how to use a WebClient to test this server application.

## Swagger Generator

The Swagger (OpenAPI) generation can be configured by using the `helidon-oci-swagger-maven-plugin`. For detailed usage information
about this plug-in, please see the [README](../../../docs/swagger.md)

## Application Configuration:

The application configuration can be customized in
[src/main/resources/META-INF/microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties).
and currently has the following parameters:

1. `server.host` - Host IP address of the server application and currently set to accept all at `0.0.0.0`
2. `server.port` - Host port of the server application and currently set to `8080`
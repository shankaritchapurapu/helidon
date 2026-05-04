# OCI OpenAPI Generator Example

This example shows how an OCI service can use one Swagger 2 API specification
for two build-time steps:

* Validate the Splat-facing API and downstream specifications with the
  Splat-owned `splat-swagger-maven-plugin`.
* Generate Helidon resources with the Helidon Extensions
  `helidon-declarative` OpenAPI generator.

The Splat/RQS integration shown here is build-time only. It does not submit the
API specification to Splat, publish or change an RQS schema, or make runtime
calls to Splat or RQS.

A real service still needs to follow the Splat/RQS onboarding workflow with the
reviewed service resource type, schema version, scope, permissions, and rollout
settings.

## Contents

* [`robot-api.yaml`](src/main/resources/swagger/robot-api.yaml) is the annotated
  Swagger 2 spec used as the Splat-facing source artifact and as the Helidon
  generator input.
* [`splat-validation.conf`](src/main/splat/splat-validation.conf) and
  [`downstream.conf`](src/main/splat/downstream.conf) illustrate the Splat
  validation plugin input files.
* [`pom.xml`](pom.xml) wires `openapi-generator-maven-plugin` to generate
  Helidon resources during `generate-sources` and runs Splat validation during
  `compile`. The build filters the Splat config into `target/generated-splat`
  before validation so it can be run from the repository root.
* `SplatRqsOpenApiGeneratorExampleTest` starts the generated Helidon
  application, sends HTTP requests to generated resource routes, and verifies
  that the generated OpenAPI endpoint still contains the Splat/RQS metadata.

## Run

From the repository root:

```bash
mvn -pl examples/oci-openapi-generator -am test
```

Generated source files are written under
`target/generated-sources/openapi/src/main/java` and compiled as part of the
example build. The generated endpoint methods are stubs; a real service must
replace the generated method bodies or delegate from them to service-owned
business logic.

The same command also runs the Splat validation plugin before the HTTP tests.
To run only generation, compilation, and Splat validation:

```bash
mvn -pl examples/oci-openapi-generator -am compile
```

The example keeps Swagger 2 as the source because current Splat-facing
validation/onboarding guidance is Swagger 2 oriented. OpenAPI Generator parses
that source before the Helidon generator produces resources.

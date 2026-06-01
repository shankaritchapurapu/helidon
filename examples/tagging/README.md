# Helidon Tagging Example

## Overview

This example shows how to use Helidon together with:

* `helidon-oci-tagging` for binary tag slug creation
* `helidon-oci-identity` for Auth SDK authentication, authorization, and request data injection
* `helidon-oci-identity-client` for the OCI Java SDK Identity client

The example exposes separate paths for creating an OCI Identity tag definition and for creating a tagged resource. Both
paths are protected with `@AuthorizationPermission`. The resource path also injects the Auth SDK `AuthorizationRequest`
directly, converts existing request tags into a tag slug, sends that slug to Authorization Service with
`AuthorizationRequestFactory.setNewTags(...)`, and returns a resource representation containing the authorized tag slug.

Metrics emission is disabled in [`src/main/resources/application.yaml`](./src/main/resources/application.yaml).

For background and the tag slug workflow, see [Tagging](../../docs/services/tagging.md) under the
[Helidon-OCI Native Services Integration Guide](../../docs/README.md).

## Prerequisites

* JDK 25
* Maven

## Build

From `examples/tagging`:

```shell
mvn package
```

## Run

From `examples/tagging`:

```shell
java -jar ./target/helidon-oci-examples-tagging.jar
```

The service starts on `http://localhost:8080/tagging`.

## Endpoints

Both endpoints are intercepted by the identity integration through `@AuthorizationPermission`. The `curl` snippets show the
request body shape; live calls must use the signed request or development authentication setup for the target
environment.

Create the OCI Identity tag definition first:

```shell
curl -X POST http://localhost:8080/tagging/tag-definitions \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
        "tagNamespaceId": "ocid1.tagnamespace.oc1..example",
        "tagName": "CostCenter",
        "description": "Cost center tag",
        "costTracking": false
      }'
```

After the tag definition exists, create a tagged resource:

```shell
curl -X POST http://localhost:8080/tagging/resources \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
        "resourceId": "ocid1.exampletaggedresource.oc1..example",
        "compartmentId": "ocid1.compartment.oc1..example",
        "tags": {
          "freeformTags": {
            "owner": "platform",
            "environment": "dev"
          },
          "definedTags": {
            "Operations": {
              "CostCenter": "42"
            }
          },
          "systemTags": {
            "orcl-cloud": {
              "free-tier-retained": "true"
            }
          }
        }
      }'
```

The resource response is a tagged resource view. The `tagSlug` value is the authorized slug returned by Authorization
Service, and the response tag maps are decoded from that authorized slug.

## Configuration

The example uses instance principal authentication for the OCI Java SDK Identity client:

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  identity-client:
    region: us-ashburn-1
```

The Auth SDK configuration is under `oci.identity`. Region and physical AD are
omitted there because the identity integration defaults them from `oci-env`.
The example's [`oci-config.yaml`](./src/main/resources/oci-config.yaml) supplies
a local `helidon.oci-env.location-override` for deterministic local runs; replace
or remove that override in deployments that should use OCI runtime location
files. Keep `oci.identity-client.region` explicit unless the OCI SDK client is
configured another way, such as with an explicit endpoint or region-bearing
authentication provider.

## Test

Run the example tests:

From `examples/tagging`:

```shell
mvn test
```

The tests start the example application, mock `IAuthorizationClient` and OCI SDK `Identity` through test-only service
registry bindings, and exercise the HTTP tag definition and resource creation paths without making real OCI calls.

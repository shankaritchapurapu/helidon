# Helidon Tagging Example

## Overview

This example shows how to use Helidon together with the OCI internal tagging client.

It supports:

* converting `freeformTags`, `definedTags`, and optional `systemTags` into a tag slug
* decoding a tag slug back into tag maps
* creating an empty tag slug for write paths that do not carry tags

The example uses the reusable `helidon-oci-tagging` module. The endpoints do not call OCI; they use the
module-created internal tagging client to convert resource tag maps to and from tag slugs.
Metrics emission is disabled in [`src/main/resources/application.yaml`](./src/main/resources/application.yaml).

For background and the tag slug workflow, see [Tagging](../../docs/tagging.md) under the
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

The service exposes:

* `POST /tagging/slugs`
* `POST /tagging/tag-sets`
* `GET /tagging/slugs/empty`

Create a tag slug from an existing resource with freeform, defined, and system tags:

```shell
curl -X POST http://localhost:8080/tagging/slugs \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
        "resourceId": "ocid1.instance.oc1..example",
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

Use `POST /tagging/tag-sets` with `resourceId` and `tagSlug` to decode a slug back into a tagged resource.

## Test

Run the example tests:

From `examples/tagging`:

```shell
mvn test
```

The tests start the example application and use the tagging client supplied by `helidon-oci-tagging`.

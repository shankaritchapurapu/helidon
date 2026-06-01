# Helidon Data-Plane Reference Example

This example is the SE-oriented replacement for the older standalone reference service.
It keeps the same intent, a small runnable service that demonstrates the OCI Helidon
cross-cutting integrations, but it follows the current `oci-helidon` example style:
Helidon 4 SE, service registry bootstrapping, and lightweight annotated endpoints instead
of the old MP and OpenAPI-generated application structure.

## What It Demonstrates

The example combines the OCI Helidon pieces that fit naturally in a single SE app:

- `helidon-oci-request-id-webserver`: every request gets an `opc-request-id`, and the
  status endpoint exposes the current request id through a direct `OciRequestId`
  resource-method parameter
- `helidon-oci-errorcode`: business failures are reported with OCI-style error codes
  and JSON payloads
- `helidon-oci-identity`: write operations require a signed request via method-level
  `@AuthorizationPermission`, and the endpoint injects `Principal` directly into secured
  resource methods to access the authenticated principal
- `helidon-oci-kiev`: robots are stored through Kiev, with the example
  configuration using the `IN_MEMORY` backend so the service stays runnable without
  external infrastructure
- `helidon-oci-audit`: mutating operations enrich the current audit payload when audit is
  enabled

The repository already has dedicated examples for service-specific integrations:

- [Limits](../limits/README.md)
- [Kiev store](../store/README.md)
- [Workflow](../workflow/README.md)

This module is intended to be the reference app for the data-plane style,
cross-cutting service setup around those integrations.

## Build

From `examples/data-plane`:

```shell
mvn package
```

From the repository root:

```shell
mvn -pl examples/data-plane -am package
```

## Run

From `examples/data-plane`:

```shell
java -jar ./target/helidon-oci-examples-dataplane.jar
```

The service starts on `http://localhost:8080/data-plane`.

## Endpoints

Open endpoints:

- `GET /data-plane`
  Returns service status, the current `opc-request-id`, and audit state.
- `GET /data-plane/probe`
  Small operational probe kept for continuity with the old reference service.
  It is not a Helidon health endpoint.
- `GET /data-plane/robots`
  Lists the sample robots stored in Kiev. Supports `compartmentId` and `displayName`.
- `GET /data-plane/robots/{id}`
  Fetches a robot. Robot lookup and mutation responses use a small envelope with
  `responseStatus`, `payload`, and `error`.

Signed endpoints:

- `POST /data-plane/robots`
  Creates a robot.
- `PUT /data-plane/robots/{id}`
  Updates a robot display name.
- `DELETE /data-plane/robots/{id}`
  Deletes a robot.

Example open requests:

```shell
curl -s http://localhost:8080/data-plane | jq
curl -s http://localhost:8080/data-plane/robots | jq
curl -s http://localhost:8080/data-plane/robots/robot-1 | jq
curl -s http://localhost:8080/data-plane/probe
```

## Authentication

Write operations are protected by `@AuthorizationPermission` annotations. The generated identity
interceptor performs authentication, authorizes the declared permission, and makes the
authenticated `Principal` available as a direct resource-method parameter. The test suite uses
the hard-coded key supplier mode, already used by `examples/identity`, to exercise the secured
routes without a live OCI environment.

For a real environment you can switch to the same patterns used by `examples/identity`:

- hard-coded key supplier for local/dev signing
- OCI API key signing through `~/.oci/config`
- instance principal material through the tunneled IMDS setup

## Configuration Notes

The runnable configuration lives in [`application.yaml`](src/main/resources/application.yaml).
It enables:

- Kiev persistence through `oci.kiev`
- direct `OciRequestId` method-parameter injection on the status endpoint
- request-id support through the dependency
- audit via `oci.auditv2`
- identity authn/authz settings via `oci.identity`

Identity region and physical-AD settings are intentionally omitted. The
identity integration defaults them from `oci-env`; the example's
[`oci-config.yaml`](src/main/resources/oci-config.yaml) supplies a local
`helidon.oci-env.location-override` so the app and tests stay deterministic
outside OCI. Replace or remove that override in deployments that should use OCI
runtime location files.

The configured Kiev data store uses the `IN_MEMORY` backend, which keeps the example
self-contained. Switching to `DIRECT_DB` or
`SERVICE` is a configuration change: update the `oci.kiev.data-stores` entry and
provide the backend-specific settings shown in the same file.

The same file also includes commented examples for:

- Secret Service V2 TLS rotation (`manager.oci-ssv2`)
- environment-derived endpoints once env-config values are available
- future metrics/T2 settings

## Test

Run the example tests:

```shell
mvn test
```

From the repository root:

```shell
mvn -pl examples/data-plane -am test
```

The tests cover:

- request-id propagation on open routes
- OCI error JSON on missing resources
- signed create/update/delete flows using the hard-coded key supplier

# Helidon Identity Example

## Overview

This example shows how to protect Helidon SE endpoints with the OCI Identity
integration, including the SPLAT-aware request path used when traffic is
forwarded by SPLAT.

It demonstrates:

* `helidon-oci-identity` Auth SDK authentication and authorization
* `helidon-oci-envconfig` environment-derived region and availability domain values
* method-level `@AuthorizationPermission` permission checks
* `@Identity.Authenticated` for endpoints that need authentication without authorization
* direct `Principal` injection into protected resource methods
* SPLAT-forwarded requests through `oci.identity.splat-aware`
* local testing for SPLAT behavior without a live SPLAT deployment
* service registry bootstrapping with generated Helidon bindings

The example exposes one open endpoint and two signed endpoints. The signed
endpoints use `@AuthorizationPermission`, and the generated identity interceptor
authenticates the request, authorizes the declared permission, registers request
identity data, and supplies the authenticated `Principal` directly to the
endpoint method.

Use `@Identity.Authenticated` when an endpoint needs an authenticated caller but
does not need an authorization permission check. It can be applied at method
level or type level. Use Auth SDK authorization annotations, such as
`@AuthorizationPermission`, when the endpoint must also authorize one or more
permissions. Authorization implies authentication, because authorization cannot
be performed without first authenticating the caller.

Auth SDK annotations in
`com.oracle.pic.identity.authorization.permissions.annotations` trigger the
generated identity interceptor for authorized endpoints.

Helidon requires either `@Identity.Authenticated` or a supported Auth SDK
authorization annotation to trigger the code generation that creates and applies
the identity interceptor. Without one of those annotations, identity request data
such as `Principal` is not registered for direct method-parameter injection.

For the full identity integration guide, see
[Identity](../../docs/services/identity.md) under the
[Helidon-OCI Native Services Integration Guide](../../docs/README.md).

## Prerequisites

* JDK 25
* Maven

For live signed requests, configure OCI Auth SDK trust material and request
signing for the target environment. Local tests use either API key signing from
`~/.oci/config` or explicit test certificate material.

## Build

From `examples/identity`:

```shell
mvn package
```

From the repository root:

```shell
mvn -pl examples/identity -am package
```

## Run

From `examples/identity`:

```shell
java -jar ./target/helidon-oci-examples-identity.jar
```

The service starts on `http://localhost:8080/identity`.

## Endpoints

Open endpoint:

* `GET /identity`
  Returns `pong`.

Signed endpoints:

* `POST /identity/once`
  Requires permission `IDENTITY_ONCE` and returns the submitted plain-text body.
* `POST /identity/twice`
  Requires permission `IDENTITY_TWICE` and returns the submitted plain-text body twice.

Example open request:

```shell
curl -s http://localhost:8080/identity
```

The signed endpoints require a valid signed request for the configured
environment:

```shell
curl -X POST http://localhost:8080/identity/once \
  -H 'Content-Type: text/plain' \
  -H 'Accept: text/plain' \
  -d 'Hello World'
```

Those endpoints can also accept a SPLAT-forwarded request that satisfies the
SPLAT-aware filter checks described below.

## Configuration

The runtime configuration lives in
[`application.yaml`](src/main/resources/application.yaml).

The relevant settings are under `oci.identity`:

* `oci.identity.authentication` configures the Auth SDK authenticator, region,
  trust material, application metadata, and instance principal URI.
* `oci.identity.authorization` configures the authorization client region,
  service name, physical AD, and trust material.
* `oci.identity.splat-aware` configures how the identity filter identifies and
  handles SPLAT-forwarded requests.

The example also enables Helidon request scope because the generated identity
interceptor registers per-request identity data into the request context.

Region and availability-domain settings are intentionally omitted from the
identity configuration. The identity integration defaults them from `oci-env`:

```yaml
oci:
  identity:
    authentication:
      global-business-unit: my-business-unit
      team-name: my-team
      application-name: my-application
    authorization:
      service-name: my-service
    splat-aware:
      splat-request-port: 8443
```

The example includes
[`oci-config.yaml`](src/main/resources/oci-config.yaml) with a local
`helidon.oci-env.location-override` so the app and tests have deterministic
values outside OCI. In a deployed OCI environment, `oci-env` can resolve the
same values from runtime files such as `/etc/region` and
`/etc/availability-domain`; replace or remove the local override for that
deployment shape.

## SPLAT-Aware Configuration

The runtime configuration includes a commented production-shape mTLS listener and
active SPLAT-aware identity settings:

```yaml
server:
  port: 8080
  # Uncomment this block in a real SPLAT deployment.
  # sockets:
  #   splat:
  #     port: 8443
  #     tls:
  #       client-auth: "REQUIRED"
  #       endpoint-identification-algorithm: "NONE"
  #       trust:
  #         pem:
  #           cert-chain:
  #             resource.path: /path/to/trusted-client-ca.pem
  #       private-key:
  #         pem:
  #           key:
  #             resource.path: /path/to/server-key.pem
  #           cert-chain:
  #             resource.path: /path/to/server-cert-chain.pem

oci:
  identity:
    splat-aware:
      splat-request-port: 8443
      additional-splat-request-ports: []
      skip-authorization-for-splat: true
      validate-splat-cert: true
      disable-tag-only-request-check: true
      reject-x-region-calls: false
```

In a deployed service, `splat-request-port` should be the mTLS-only listener
that receives traffic from SPLAT. The example keeps that listener commented so
the default app still starts with only the normal `8080` listener, but the
SPLAT-aware config points at the intended `8443` SPLAT port.

Keep `validate-splat-cert: true` in real deployments. The local SPLAT test
profile disables certificate validation only because the test simulates SPLAT
headers without standing up an mTLS SPLAT listener.

This complements [`examples/splat`](../splat/README.md): that example shows the
standalone SPLAT mTLS interceptor from `helidon-oci-splat`; this identity
example shows how the Auth SDK identity filter handles SPLAT-forwarded
principals and falls back to normal Auth SDK/authproxy behavior for non-SPLAT
traffic.

## When The SPLAT Path Is Used

The generated identity interceptor asks `AuthContextRequestFilterFactory` for an
Auth SDK request filter. That factory creates a provenance-aware
`SplatAwareAuthContextRequestFilter` subclass; the request itself decides which
branch is used.

The request is treated as SPLAT-aware when:

* the request arrives on `oci.identity.splat-aware.splat-request-port`, or one of
  `additional-splat-request-ports`
* `validate-splat-cert` is `false`, or the mTLS client certificate passes SPLAT
  certificate validation
* the request carries SPLAT-forwarded identity data in the `opc-principal` header

When `skip-authorization-for-splat: true`, the service-side authorization call
is skipped only if the request is identified as SPLAT and it also includes:

```text
oci-skip-authorization-for-splat: true
```

That models the production path where SPLAT has already handled authorization
and forwards the authenticated principal to the service.

## When It Defaults To Authproxy

The same filter falls back to the normal Auth SDK/authproxy behavior when the
request is not identified as a SPLAT request. Common cases are:

* direct client calls to the normal service listener, such as `8080` in this
  example
* signed local tests that do not use the SPLAT headers
* requests arriving on a port not configured as a SPLAT request port
* requests on a SPLAT port that do not pass required SPLAT certificate checks

In this path, the service does not trust `opc-principal` as a SPLAT-forwarded
principal. The request must authenticate normally, for example with OCI request
signing, and authorization annotations are evaluated through the normal Auth
SDK/authproxy flow.

## Test

Run the example tests:

```shell
mvn test
```

From the repository root:

```shell
mvn -pl examples/identity -am test
```

The signed request test uses the `API_KEY` profile from `~/.oci/config` when
available; if request signing cannot be configured locally, that test path logs
a warning and skips the signed call. The SPLAT-aware test uses explicit local
test certificate material from `src/test/resources`.

Run only the SPLAT-aware example test from the repository root:

```shell
mvn -pl examples/identity -am -Dtest=IdentityEndpointSplatAwareTest -Dsurefire.failIfNoSpecifiedTests=false test
```

`IdentityEndpointSplatAwareTest` uses
[`src/test/resources/application-splat.yaml`](src/test/resources/application-splat.yaml).
The test binds the server to a local available port, configures that same port as
the SPLAT request port, sends an unsigned request with `opc-principal`, and adds
`oci-skip-authorization-for-splat: true`. That proves the identity context is
hydrated from the SPLAT-forwarded principal instead of from a normal signed
request. It is not an mTLS certificate validation test; the runtime config keeps
the commented mTLS listener as the production shape for that part of the setup.

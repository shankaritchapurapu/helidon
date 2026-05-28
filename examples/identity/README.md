# Helidon Identity Example

This example shows how to secure generated Helidon endpoints with OCI Identity
authentication and authorization, including the SPLAT-aware request path used
when traffic is forwarded by SPLAT.

## What It Demonstrates

- `@AuthorizationPermission` on generated `@RestServer.Endpoint` methods
- `IdentityContext` injection into secured resource methods
- direct signed requests through the normal Auth SDK/authproxy path
- SPLAT-forwarded requests through `oci.identity.splat-aware`
- local testing for SPLAT behavior without a live SPLAT deployment

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

The service starts on:

```text
http://localhost:8080/identity
```

Open endpoint:

```shell
curl -s http://localhost:8080/identity
```

Secured endpoints:

- `POST /identity/once`
- `POST /identity/twice`

Those endpoints require either a normal signed request or a SPLAT-forwarded
request that satisfies the SPLAT-aware filter checks described below.

## SPLAT-Aware Configuration

The runtime configuration lives in [`src/main/resources/application.yaml`](src/main/resources/application.yaml):

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
      region: us-ashburn-1
```

In a deployed service, `splat-request-port` should be the mTLS-only listener
that receives traffic from SPLAT. The example keeps that listener commented so
the default app still starts with only the normal `8080` listener, but the
SPLAT-aware config points at the intended `8443` SPLAT port.

Keep `validate-splat-cert: true` in real deployments. The local SPLAT test
profile disables certificate validation only because the test simulates SPLAT
headers without standing up an mTLS SPLAT listener.

This complements [`examples/splat`](../splat/README.md): that example shows the
standalone SPLAT mTLS interceptor from `helidon-oci-splat`; this identity example
shows how the Auth SDK identity filter handles SPLAT-forwarded principals and
falls back to normal Auth SDK/authproxy behavior for non-SPLAT traffic.

## When The SPLAT Path Is Used

The generated identity interceptor asks `AuthContextRequestFilterFactory` for an
Auth SDK request filter. That factory always creates a
`SplatAwareAuthContextRequestFilter`; the request itself decides which branch is
used.

The request is treated as SPLAT-aware when:

- the request arrives on `oci.identity.splat-aware.splat-request-port`, or one of
  `additional-splat-request-ports`
- `validate-splat-cert` is `false`, or the mTLS client certificate passes SPLAT
  certificate validation
- the request carries SPLAT-forwarded identity data in the `opc-principal` header

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

- direct client calls to the normal service listener, such as `8080` in this
  example
- signed local tests that do not use the SPLAT headers
- requests arriving on a port not configured as a SPLAT request port
- requests on a SPLAT port that do not pass required SPLAT certificate checks

In this path, the service does not trust `opc-principal` as a SPLAT-forwarded
principal. The request must authenticate normally, for example with OCI request
signing, and authorization annotations are evaluated through the normal
Auth SDK/authproxy flow.

## Test

Run all identity example tests from the repository root:

```shell
mvn -pl examples/identity -am test
```

Run only the SPLAT-aware example test:

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

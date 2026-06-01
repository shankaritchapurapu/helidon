# Helidon SPLAT Example

---

This example shows the current SPLAT integration path for Helidon generated `@RestServer.Endpoint` handlers.

## What It Demonstrates

- generated `@RestServer.Endpoint` 
- service-registry discovery of the SPLAT endpoint interceptor from the `splat` module
- the listener-side TLS contract needed for SPLAT to see client certificates on the request

## Configure

This example is meant to run on an mTLS listener. That means you should expose this example only on a listener that is already configured for:

- TLS enabled
- `client-auth: "REQUIRED"`
- trust material that validates the client certificate chain you expect SPLAT to inspect
- server certificate and key material for the listener itself

The sample `application.yaml` includes a commented listener block showing where that server-side TLS configuration lives.
The SPLAT region override is omitted from `oci.splat`; the SPLAT integration
defaults it from `oci-env`. The example's `oci-config.yaml` provides a local
`helidon.oci-env.location-override` for deterministic local runs.

## Build

From `examples/splat`:

```shell
mvn package
```

From the repository root:

```shell
mvn -pl examples/splat -am package
```

## Run

From `examples/splat`:

```shell
java -jar ./target/helidon-oci-examples-splat.jar
```

Then call the endpoint with a client certificate and key that your listener trusts and that upstream SPLAT accepts:

```shell
curl --cacert /path/to/ca.pem \
  --cert /path/to/client-cert.pem \
  --key /path/to/client-key.pem \
  https://localhost:8443/splat
```

If the request reaches the handler, the response is:

```text
splat-ok
```

## Test Scope

This example is compiled as part of the build, but it does not include a fake local end-to-end certificate test.
A real SPLAT end-to-end flow depends on certificate material and trust relationships that match the upstream SPLAT
environment, so the example focuses on the concrete application wiring and this README instead of a misleading self-signed test.

# Splat

---

## Overview

The Splat module integrates the upstream SPLAT JAX-RS mTLS filter into generated Helidon `@RestServer.Endpoint`
request handling. It delegates certificate and authorization validation to the upstream `SplatMtlsFilter` implementation directly.

---

## Maven Coordinates

To enable SPLAT mTLS validation, add the following dependency to your project’s pom.xml:

```xml

<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-splat</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

When the Helidon service registry is enabled, this module enables discovery of the SPLAT endpoint interceptor and SPLAT filter
factory automatically. This sets request flow as:

- [SplatMtlsEndpointInterceptor](../../splat/src/main/java/com/oracle/helidon/oci/splat/SplatMtlsEndpointInterceptor.java) is a
  SPLAT-owned `HttpEntryPoint.Interceptor`
  that runs for generated `@RestServer.Endpoint` handlers, which Helidon wires through `HttpEntryPoint.EntryPoints.handler(...)`.
- [SplatMtlsRequestHandler](../../splat/src/main/java/com/oracle/helidon/oci/splat/SplatMtlsRequestHandler.java) creates the
  upstream SPLAT JAX-RS filter and bridges Helidon request/response objects to JAX-RS filter execution.

### Listener And Certificate Requirements

It is important to note that mTLS must be configured on the Helidon WebServer listener itself with the appropriate trust material
and client-certificate requirements. The upstream SPLAT filter reads peer certificates from the request context. In practice that
means:

- the Helidon listener must terminate TLS and require client certificates
- the listener trust configuration must validate the client certificate chain before SPLAT runs
- SPLAT does not choose which socket to attach; the generated endpoint interceptor runs wherever that endpoint is exposed

If the same generated endpoint is reachable on a non-mTLS listener, SPLAT still executes there, but the request will not carry the
peer-certificate chain that upstream SPLAT expects. The recommended deployment shape is therefore to expose SPLAT-protected
generated endpoints only on listeners where mTLS is already required.

---

## Configuration

Configure SPLAT validation behavior using the `application.yaml` file.

| Config Key                            | Default Value | Description                                                                                    | Notes                                                                                                                                                                                                                  |
|---------------------------------------|---------------|------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| oci.splat.enabled                     | true          | Whether to enable SPLAT mTLS validation.                                                       |                                                                                                                                                                                                                        |
| oci.splat.skip-authz-validation-check | false         | Whether to bypass SPLAT authorization validation.                                              | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.reject-x-region-calls       | false         | Whether upstream SPLAT validation should reject cross-region client certificates.              | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.region                      | none          | The OCI region name. If not specified, the value is resolved from the OCI runtime environment. |                                                                                                                                                                                                                        |

---

## References

* [Splat User Guide](https://confluence.oci.oraclecorp.com/display/PLAT/Splat+-+User+Guide)
* [Splat Concepts](https://confluence.oci.oraclecorp.com/display/PLAT/1.+Splat+Concepts)
* [Splat Onboarding](https://confluence.oci.oraclecorp.com/display/PLAT/2.+Splat+Onboarding)
* [Splat Features](https://confluence.oci.oraclecorp.com/display/PLAT/3.+Splat+Features)
* [Splat Example](../../examples/splat/README.md)

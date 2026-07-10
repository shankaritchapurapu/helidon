# Splat

---

## Overview

The Splat integration wires upstream Splat mTLS validation into generated Helidon
`@RestServer.Endpoint` request handling. When the module is on the classpath, Helidon discovers
the endpoint interceptor through the service registry and runs the upstream `SplatMtlsFilter`
before protected endpoint methods are invoked.

The integration delegates certificate and authorization validation to upstream Splat and uses
`oci.splat` configuration for validation behavior such as region selection, cross-region rejection,
and authorization validation checks.

After all validation succeeds, the integration records trusted SPLAT provenance under a shared key in the server-side
request context. SplatAware Identity records the same value after its own port and certificate checks.
Other integrations, such as Audit V2, use that shared provenance instead of trusting caller-controlled SPLAT headers
alone.

---

## Maven Coordinates

To enable Splat mTLS validation, add the following dependency to your project’s pom.xml:

```xml

<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-splat</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

When the Helidon service registry is enabled, this module discovers and wires the Splat endpoint interceptor automatically.
Applications do not call the Splat integration classes directly. For generated `@RestServer.Endpoint` handlers, Helidon runs
`SplatMtlsEndpointInterceptor`, and that interceptor delegates to `SplatMtlsRequestHandler`, which creates the upstream
Splat JAX-RS filter and adapts the Helidon request and response objects for Splat validation.

### Listener And Certificate Requirements

It is important to note that mTLS must be configured on the Helidon WebServer listener itself with the appropriate trust material
and client-certificate requirements. The upstream Splat filter reads peer certificates from the request context. In practice that
means:

- the Helidon listener must terminate TLS and require client certificates
- the listener trust configuration must validate the client certificate chain before Splat runs
- Splat does not choose which socket to attach; the generated endpoint interceptor runs wherever that endpoint is exposed

If the same generated endpoint is reachable on a non-mTLS listener, Splat still executes there, but the request will not carry the
peer-certificate chain that upstream Splat expects. The recommended deployment shape is therefore to expose Splat-protected
generated endpoints only on listeners where mTLS is already required.

---

## Configuration

Configure Splat validation behavior using the `application.yaml` file.

The `skip-authz-validation-check` name intentionally mirrors the upstream Splat mTLS filter setting
`skipAuthzValidationCheck`. Keeping the Helidon key aligned with the upstream setting makes it easier to compare
configuration with Splat guidance and generated metadata.

| Config Key                            | Default Value | Description                                                                                    | Notes                                                                                                                                                                                                                  |
|---------------------------------------|---------------|------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| oci.splat.enabled                     | true          | Whether to enable Splat mTLS validation.                                                       |                                                                                                                                                                                                                        |
| oci.splat.skip-authz-validation-check | false         | Whether to bypass Splat authorization validation.                                              | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.reject-x-region-calls       | false         | Whether upstream Splat validation should reject cross-region client certificates.              | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.region                      | none          | The OCI region name. If not specified, the value is resolved from the OCI runtime environment. |                                                                                                                                                                                                                        |

---

## References

* [Splat User Guide](https://confluence.oci.oraclecorp.com/display/PLAT/Splat+-+User+Guide)
* [Splat Concepts](https://confluence.oci.oraclecorp.com/display/PLAT/1.+Splat+Concepts)
* [Splat Onboarding](https://confluence.oci.oraclecorp.com/display/PLAT/2.+Splat+Onboarding)
* [Splat Features](https://confluence.oci.oraclecorp.com/display/PLAT/3.+Splat+Features)
* [Splat Example](../../examples/splat/README.md)

# Secret Service V2

---

## Overview

The Secret Service V2 (SSv2) integration provides Helidon configuration and TLS support for SSv2.
It includes:

* Secret Service Config Source for SSv2 secret retrieval through Helidon Config
* Secret Service TLS Manager for server and client mTLS rotation

---

## Maven Coordinates

Add the dependency for the SSv2 feature you use:

Config source:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.secret-service</groupId>
    <artifactId>helidon-oci-secret-service-config-source</artifactId>
    <scope>runtime</scope>
</dependency>
```

TLS manager:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.secret-service</groupId>
    <artifactId>helidon-oci-secret-service-tls-manager</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

### Secret Service Config Source

SSv2 config source is a lazy Helidon config source backed by SSv2 and maps configuration properties to SSv2 secret paths.

Properties with configurable prefix `oci.ssv2` by default are being resolved as following:
* `oci.ssv2/secret/helidon/test-secret/latest` is being resolved from SSv2 as `/secret/helidon/test-secret/latest`

Keys outside the configured prefix are ignored by this source.

```java
import io.helidon.config.Config;
import io.helidon.service.registry.Service;

@Service.Singleton
final class SecretAwareService {
    private final String testSecret;

    @Service.Inject
    SecretAwareService(Config config) {
        this.testSecret = config.get("oci.ssv2/secret/helidon/test-secret/latest")
                .asString()
                .orElseThrow();
    }
}
```

Primary declarative configuration uses `oci-config.yaml`. When no explicit
`meta-config.*` bootstrap file is present and the application uses Helidon's default
declarative bootstrap, the SSv2 config source is auto-registered and reads
`helidon.oci-secret-service` from `oci-config.yaml`. The same file can also configure
`oci-env`, which publishes the region and IaaS domain values used by the default SSv2
endpoint:

```yaml
helidon:
  oci-env:
    prefix: "oci.env"
    location-override:
      region: "sol-mars-1"
      availability-domain: "sol-mars-1-ad-1"
      fault-domain: 5

  oci-secret-service:
    prefix: "oci.ssv2"
    cache-ttl: "PT5M"
    poll-interval: "PT30M"
    client:
      endpoint: "https://secret-service-ce.${oci.env.iaas-domain-name}/v1"
      tls-config:
        ca-bundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"

  oci:
    authentication-method: "auto"
    allowed-authentication-methods: ["config", "config-file"]
    authentication:
      config-file:
        profile: "DEFAULT"
        path: "/custom/path/.oci/config"
      config:
        region: "${oci.env.region-name}"
        fingerprint: "0A0B..."
        tenant-id: "ocid"
        user-id: "ocid"
        private-key:
          resource-path: "/on/classpath/private-key.pem"
      session-token:
        session-token: "token"
        session-lifetime-hours: 8
        region: "${oci.env.region-name}"
```

Optional explicit config source configuration in `meta-config.yaml`. This example includes
`oci-env` because explicit meta-config disables the automatic `oci-env` source registration
path, and SSv2 uses env-config to resolve the current region's IaaS domain:
```yaml
sources:
  - type: "oci-env"
  - type: "oci-secret-service"
    properties:
      prefix: "oci.ssv2"
      cache-ttl: "PT5M"
      poll-interval: "PT30M"
      client:
        endpoint: "https://secret-service-ce.${oci.env.iaas-domain-name}/v1"
        tls-config:
          ca-bundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
        retry-config:
          max-retries: 3
          min-retry-delay-in-ms: 100
```

The SSv2 client also accepts `client.retry` as an alias for `client.retry-config`. Aliases are provided for user
convenience, either to align with native OCI parameter names or with similar settings in other Helidon OCI modules.
Specify at most one name for an aliased setting, not both.

Endpoint region and domain resolution is delegated to
[Environment Configuration](../utilities/environment-config.md). The config-source module brings
`helidon-oci-envconfig` at runtime, and the default SSv2 endpoint uses
`${oci.env.iaas-domain-name}`:

```yaml
client:
  endpoint: "https://secret-service-ce.${oci.env.iaas-domain-name}/v1"
```

`oci-env` resolves the active region from its normal location sources, imports dynamic
core-regions metadata when needed, and publishes domain keys such as
`oci.env.iaas-domain-name`. For example, `us-phoenix-1` resolves the IaaS domain to
`r2.oracleiaas.com`, so the endpoint becomes
`https://secret-service-ce.r2.oracleiaas.com/v1`. `us-seattle-1` resolves to
`r1.oracleiaas.com`, and other realms use their realm-specific IaaS domain suffix.
If runtime location files are unavailable, the default service-registry bootstrap path can also
derive the same values from IMDS metadata.

When explicit `meta-config.*` is used, include `type: "oci-env"` before
`type: "oci-secret-service"` so the endpoint placeholder can be resolved during config
bootstrap. If no explicit `meta-config.*` is present and the application uses Helidon's
default declarative bootstrap, `oci-env` is auto-registered when `helidon-oci-envconfig`
is on the classpath, and SSv2 reads `helidon.oci-secret-service` from `oci-config.yaml`.
If the placeholder is still unresolved, SSv2 client initialization fails and the endpoint
must be configured explicitly.

SSv2 client configuration can be provided either under `client` or directly in the source definition. If both styles are used, values from `client` override duplicate root-level client settings.

If `cache-ttl` is not configured, the direct-read secret-value cache defaults to
`PT5M`. If `poll-interval` is not configured, background polling defaults to
`PT30M`. `cache-ttl` does not change the default polling cadence.

The SSv2 source uses the same keys in `oci-config.yaml` and `meta-config.*`; only the
parent path differs. `oci-config.yaml` uses `helidon.oci-secret-service`, while
`meta-config.*` uses the `properties` block of a source entry with
`type: "oci-secret-service"`. When both inputs are present for an explicit
`oci-secret-service` source, provider properties override `helidon.oci-secret-service` from
`oci-config.yaml` per key. Values from `oci-config.yaml` fill only keys missing from provider
properties.

Runtime behavior:
* Reads are lazy and each requested key becomes tracked on first access.
* Tracked values are cached for configurable `cache-ttl`.
* If the first resolver call for a tracked key throws, the source treats that key as absent instead of exposing the resolver exception.
* That initial resolver failure starts a one-`cache-ttl` source-level backoff window, so repeated source lookups keep returning absent until the TTL expires and SSv2 is retried.
* Existing Helidon `Config` nodes that already resolved the key as absent do not retry by themselves; use change listeners or rebuild `Config` to observe recovery on that same logical key.
* If a refresh fails after a value was already cached, callers keep seeing the cached value and direct reads again wait one `cache-ttl` before retrying SSv2.
* Background polling starts only when config change listeners are active.
* Only tracked keys are polled and included in emitted root snapshots.
* Listener-driven polling forces fresh SSv2 reads regardless of the current source-level cache/backoff window, so a recovered key can be published before the next TTL-based source retry.
* Direct lazy reads can publish an updated tracked snapshot immediately when they refresh a value while listeners are active.
* Root snapshot publication is serialized internally to keep poll-driven and on-demand refreshes consistent.
* The config source uses an internal SSv2 vault client backed by OCI SDK request signing, retry support, and the generated SSv2 response model.
* The source-level `cache-ttl` is the effective secret-value cache for this feature.

Runnable sample code is available in
[`examples/secret-service`](../../examples/secret-service/README.md). The sample
starts a Helidon SE endpoint, configures the SSv2 source from `oci-config.yaml`,
and reads a sample SSv2-backed value through Helidon `Config`.

### Secret Service TLS Manager

TLS manager `oci-ssv2` is capable of mTLS rotation with keys and certificates produced by PKI service and stored in SSv2 in JSON format.

PKI material can be loaded by one of the available options exclusively:
* From SSv2 by providing SSv2 path over `pki.secret-path` property.
* By loading the mTls material in JSON format through Helidon `Resource`, from file system - `pki.resource.path`,
  classpath - `pki.resource.resource-path`, or inline content - `pki.resource.content-plain`.

When `pki.secret-path` is used, the TLS manager reads the PKI JSON directly from SSv2. The reload cron performs a fresh
SSv2 read on each reload attempt, so Secret Service config source change notifications and `cache-ttl` do not affect
TLS rotation detection.

Trust CA bundle can be loaded from file system - `trust.path` or classpath - `trust.resource-path`.

### Server mTLS rotation

Helidon server side mTls can be configured to use our `oci-ssv2` Tls manager. The example below demonstrates rotation every 30 minutes of certificates delivered by PKI to an SSV2 secret:
```yaml
server:
  port: 8080
  host: localhost
  tls:
    endpoint-identification-algorithm: "NONE"
    client-auth: "REQUIRED"
    manager:
      oci-ssv2:
        # Download mTls context every 30 minutes
        reload.expression: "0 0/30 * * * ? *"
        # Fetch PKI mTls material from SSv2
        pki.secret-path: /secret/xxx/test-mtls-server/latest
        # Use trust CA bundle from local filesystem
        trust.path: /etc/oci-pki/ca-bundle.pem
```

### Client mTLS rotation

The following clients can use `oci-ssv2` Tls manager in Helidon:

* Helidon WebClient
* Any client able to use `javax.net.ssl.SSLContext` produced by `oci-ssv2` Tls manager

Consider following example:
```yaml
# Client configuration
acme-client:
  tls:
    manager:
      oci-ssv2:
        # Download mTls context every 30 minutes
        reload.expression: "0 0/30 * * * ? *"
        # Fetch PKI mTls material from SSv2
        pki.secret-path: /secret/xxx/acme-mtls-client/latest
        # Use trust CA bundle from local filesystem
        trust.path: /etc/oci-pki/ca-bundle.pem
        
# Another client configuration
another-client:
  tls:
    manager:
      oci-ssv2:
        reload.expression: "0/50 * * * * ? *"
        pki.secret-path: /secret/xxx/other-mtls-custom-client/latest
        trust.path: /etc/oci-pki/ca-bundle.pem
```

#### JAX-RS clients

  ```java
  Config acmeTlsConfig = Services.get(Config.class).get("acme-client.tls");
  ClientBuilder.newBuilder()
              .sslContext(Tls.create(acmeTlsConfig).sslContext())
              .build()
              .target(uri);
  ```

#### Helidon WebClient

  ```java
  WebClient.builder()
           .baseUri(uri)
           .config(Services.get(Config.class).get("acme-client"))
           .build();
  ```

#### MicroProfile Rest Client

  ```java
  RestClientBuilder.newBuilder()
           .baseUri(uri)
           .sslContext(Tls.create(Services.get(Config.class).get("acme-client.tls")).sslContext())
           .build(GreetRestClient.class);
  ```

#### Other HTTP clients

Any Java HTTP Client can use `javax.net.ssl.SSLContext` created by `oci-ssv2` Tls manager.

```java
  Config custTlsConfig = Services.get(Config.class).get("client.tls");
  javax.net.ssl.SSLContext sslContext = Tls.create(custTlsConfig).sslContext();
```

---

## Configuration

| Key                          | Example Value                             | Default value                     | Description                                        |
|------------------------------|-------------------------------------------|-----------------------------------|----------------------------------------------------|
| `reload.expression`         | `0 0/30 * * * ? *`                        | `0 0/30 * * * ? *` - every 30 min | Cron expression for reload interval configuration. |
| `reload.enabled`             | `false`                                   | `true`                            | When `false`, only initial load happens.           |
| `pki.secret-path`            | `/secret/helidon/test-mtls-server/latest` |                                   | SSv2 secret path to load PKI JSON material.        |
| `pki.resource.path`          | `/opt/mtls/client-pki.json`               |                                   | File path to load PKI JSON material from file.     |
| `pki.resource.resource-path` | `mtls/client-pki.json`                    |                                   | Class path to load PKI JSON material from file.    |
| `pki.resource.content-plain` | `{...}`                                   |                                   | Inline PKI JSON material.                          |
| `pki.password`               | `password123`                             |                                   | Password used in case private key is encrypted.    |
| `trust.path`                 | `/etc/oci-pki/ca-bundle.pem`              |                                   | File path to load trust CA bundle.                 |
| `trust.resource-path`        | `mtls/ca-bundle.pem`                      |                                   | Class path to load trust CA bundle.                |

---

## References

* [Secret Service V2](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/secret/landing-secrets.htm)

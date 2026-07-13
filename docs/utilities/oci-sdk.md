# OCI SDK

---

## Overview

The OCI SDK integration provides shared OCI SDK authentication support for Helidon applications.

When `helidon-oci-sdk-oci` and a matching authentication module are on the classpath, the integration exposes the
selected OCI SDK `BasicAuthenticationDetailsProvider` as an injectable service. Other modules can then build OCI SDK
clients without implementing their own authentication selection logic.

The Identity integration is an exception; it uses the identity auth SDK directly and does not build clients
from the shared OCI SDK authentication provider.

Authentication is configured under `helidon.oci`. Applications choose an authentication method by
configuration and add the matching authentication module to the classpath.

---

## Maven Coordinates

The common OCI SDK integration is provided by:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-oci</artifactId>
</dependency>
```

Add one or more authentication modules depending on the methods the application should support:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-authentication-instance</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-authentication-resource</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-authentication-oke-workload</artifactId>
</dependency>
```

For ODO service-principal authentication:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-authentication-service-principal</artifactId>
</dependency>
```

The service-principal module depends on OCI SDK internal add-ons and is intended for OCI-internal
deployments where those artifacts are available.

---

## Authentication

The selected authentication provider is exposed through the Helidon service registry as
`BasicAuthenticationDetailsProvider`. A service can inject it directly:

```java
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

@Service.Singleton
class OciClientFactory {
    private final BasicAuthenticationDetailsProvider authProvider;

    @Service.Inject
    OciClientFactory(BasicAuthenticationDetailsProvider authProvider) {
        this.authProvider = authProvider;
    }
}
```

Use `authentication-method` to select a specific method:

```yaml
helidon:
  oci:
    authentication-method: instance-principal
```

When `authentication-method` is `auto`, Helidon tries the available authentication methods from the
service registry and uses the first method that can provide a `BasicAuthenticationDetailsProvider`.
The available methods are determined by the authentication modules on the classpath.

---

## Service Principal Authentication

Service-principal authentication adds the `service-principal` authentication method and an
`OciServicePrincipalProvider` capability. On ODO it builds an OCI SDK
`S2SAuthenticationDetailsProvider` using instance-principal material. When an
`OciResourcePrincipalProvider` is available, such as OKE workload identity on OMK or standard OCI resource-principal
authentication, it builds an `RpS2SAuthenticationDetailsProvider` instead. Both are exposed through the common
`BasicAuthenticationDetailsProvider` contract.

Service-principal configuration is part of the shared `OciConfig`. For the normal declarative configuration path,
put the complete `helidon.oci` configuration in `oci-config.yaml`, including
`authentication.service-principal`. The service-principal module reads this subtree from the configuration retained
by `OciConfig`; it does not read service-principal settings from the application `Config` assembled by
`meta-config.*`.

Enable it by adding the service-principal authentication dependency and selecting the method:

```yaml
helidon:
  oci:
    authentication-method: service-principal
```

For local testing through an IMDS tunnel:

```yaml
helidon:
  oci:
    authentication-method: service-principal
    imds-base-uri: http://localhost:8000/opc/v2/
    imds-timeout: PT3S
    imds-detect-retries: 1
```

For explicit service-principal certificates instead of IMDS:

```yaml
helidon:
  oci:
    authentication-method: service-principal
    federation-endpoint: https://auth.us-phoenix-1.oraclecloud.com
    tenant-id: ocid1.tenancy.oc1...
    authentication:
      service-principal:
        use-instance-principal: false
        certificates:
          - certificate: /path/to/sp_cert.pem
            private-key: /path/to/sp_key.pem
            passphrase: ""
          - certificate: /path/to/intermediate.pem
```

Optional service-principal settings:

```yaml
helidon:
  oci:
    authentication-method: service-principal
    region: us-phoenix-1
    federation-endpoint: https://auth.example
    imds-base-uri: http://169.254.169.254/opc/v2/
    tenant-id: ocid1.tenancy.oc1...
```

On OMK, `federation-endpoint` and `tenant-id` are required for the RP-to-SP exchange. The OKE
workload authentication module supplies the resource principal automatically; service consumers do
not need their own ODO/OMK switch.

Modules that need a service principal in addition to the globally selected authentication method
can inject the capability directly:

```java
import io.helidon.integrations.oci.OciServicePrincipalProvider;
import io.helidon.service.registry.Service;

@Service.Singleton
class DownstreamClientFactory {
    private final OciServicePrincipalProvider servicePrincipalProvider;

    @Service.Inject
    DownstreamClientFactory(OciServicePrincipalProvider servicePrincipalProvider) {
        this.servicePrincipalProvider = servicePrincipalProvider;
    }
}
```

When no resource principal is available, the provider requires an available instance metadata
service by default. If `authentication-method` is set explicitly to `service-principal`,
`authentication.service-principal.use-instance-principal` is left at its default of `true`, and IMDS
is not available, startup fails because the requested authentication method cannot provide an auth
provider.

When `authentication.service-principal.use-instance-principal=false`, IMDS is not required. In this mode
`federation-endpoint`, `tenant-id`, and at least one certificate entry must be configured. The first certificate
entry is the leaf certificate and must include `private-key`; subsequent entries are treated as intermediate
certificates.

---

## Usage

Any module or application code that consumes `BasicAuthenticationDetailsProvider` can use service-principal
authentication without additional code changes.

For example, an OCI SDK client factory can remain authentication-method neutral:

```java
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.bmc.monitoring.MonitoringClient;

@Service.Singleton
class MonitoringFactory implements Supplier<Monitoring> {
    private final BasicAuthenticationDetailsProvider authProvider;

    @Service.Inject
    MonitoringFactory(BasicAuthenticationDetailsProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public Monitoring get() {
        return new MonitoringClient(authProvider);
    }
}
```

The selected provider is controlled only by configuration and classpath contents.

---

## Configuration

Common `helidon.oci` authentication keys:

These keys are loaded together as `OciConfig`. In particular, do not split the
`helidon.oci.authentication.service-principal` subtree into an application source configured through
`meta-config.*`; keep it with the rest of `helidon.oci` in `oci-config.yaml`.

| Key | Default value | Description |
|-----|---------------|-------------|
| `helidon.oci.authentication-method` | `auto` | Authentication method to use. Set to `service-principal` to require service-principal auth. |
| `helidon.oci.allowed-authentication-methods` | | Optional method allow-list used when `authentication-method` is `auto`. |
| `helidon.oci.region` | | Optional OCI region used by authentication providers that support a region override. |
| `helidon.oci.imds-base-uri` | | Optional IMDS base URI override. Useful for local tunnel testing. |
| `helidon.oci.imds-timeout` | | Optional IMDS availability check timeout. |
| `helidon.oci.imds-detect-retries` | | Optional number of IMDS availability detection retries. |
| `helidon.oci.federation-endpoint` | | Optional federation endpoint override used by providers that support it. |
| `helidon.oci.tenant-id` | | Optional tenancy OCID used by providers that support it. |
| `helidon.oci.authentication.service-principal.use-instance-principal` | `true` | Whether service-principal auth loads certificate material from IMDS. |
| `helidon.oci.authentication.service-principal.certificates` | `[]` | Explicit service-principal certificate chain used when `use-instance-principal=false`. |
| `helidon.oci.authentication.service-principal.certificates[].certificate` | | Required certificate resource path. The first entry is the leaf certificate. |
| `helidon.oci.authentication.service-principal.certificates[].private-key` | | Private key resource path. Required on the first certificate entry. |
| `helidon.oci.authentication.service-principal.certificates[].passphrase` | `""` | Optional private key passphrase. |

Service-principal support uses `region`, `federation-endpoint`, `imds-base-uri`, and `tenant-id` when
they are configured. Explicit certificate mode also uses `authentication.service-principal`. These settings must
come from the same `OciConfig` source.

---

## Common Module Contents

The `helidon-oci-sdk-oci` module is the common OCI SDK integration layer. It provides:

* `OciConfig` configuration under `helidon.oci`
* `OciConfigProvider`, which loads OCI SDK configuration from `oci-config.yaml`, environment variables,
  system properties, or an application-provided `OciConfig`
* `AdpProvider`, which selects an `OciAuthenticationMethod` and exposes the resulting
  `BasicAuthenticationDetailsProvider` through the Helidon service registry
* built-in authentication methods for `config`, `config-file`, and `session-token`
* `RegionProvider`, which exposes an OCI SDK `Region` through the service registry
* IMDS utilities used to detect the instance metadata service and retrieve instance metadata
* the `io.helidon.integrations.oci.spi.OciAuthenticationMethod` SPI used by separate authentication
  modules such as instance-principal, resource-principal, OKE workload identity, and service-principal

The common module does not contain every OCI authentication provider. Authentication methods that require
additional OCI SDK or environment-specific dependencies live in separate modules so applications can choose
only the methods they need.

---

## Client Integrations

The following modules already use the shared `BasicAuthenticationDetailsProvider`, so they can use
`service-principal` by adding the authentication module and setting `helidon.oci.authentication-method`:

* [Kiev](../services/kiev.md), when the Kiev service backend uses `service.auth.type: "OVERRIDDEN"`
* [Limits](../services/limits.md)
* [Metrics](../services/metrics.md)
* [Secret Service V2 config source](../services/secret-service.md)
* [Workflow](../services/workflow.md), when workflow auth details are enabled
* [Metering control plane direct mode](../services/metering.md)

Kiev's native `service.auth.type: "S2S"` is separate from this OCI SDK provider. Use `OVERRIDDEN`
when the Kiev client should receive the Helidon-managed `BasicAuthenticationDetailsProvider`.
Kiev `OVERRIDDEN` auth and Workflow can also reference reusable dynamic SSL context providers configured under
`oci.dynamic-ssl-context-providers`, or custom named `DynamicSslContextProviderConfig` services supplied by the
application.

The [Identity](../services/identity.md) integration does not use the OCI SDK `BasicAuthenticationDetailsProvider`.
It builds identity authentication and authorization clients through the identity auth SDK, so the
service-principal authentication method described here does not change identity client construction.

The [Tagging](../services/tagging.md) integration and metering data plane log-file recorder do not currently use
OCI SDK authentication providers.

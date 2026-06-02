# Limits

---

## Overview

The Limits integration registers a configured OCI Limits data plane client,
`com.oracle.oci.limits.LimitsDPClient`, with the Helidon service registry.

Applications can inject the client and use it to read service limits, evaluate limits for a
region or availability domain, and perform other operations supported by the Limits data plane
Java client. The client is built from:

* the shared OCI SDK `BasicAuthenticationDetailsProvider`
* optional `oci.limits` endpoint and client settings
* OCI SDK client defaults when no explicit client settings are provided

---

## Maven Coordinates

To enable limits, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-limits</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

Configure OCI SDK authentication separately. For example, to use instance principal
authentication:

```yaml
helidon:
  oci:
    authentication-method: instance-principal
```

You can inject `LimitsDPClient` directly:

```java
import io.helidon.service.registry.Service;

import com.oracle.oci.limits.LimitsDPClient;

@Service.Singleton
class LimitsService {
    private final LimitsDPClient limitsClient;

    @Service.Inject
    LimitsService(LimitsDPClient limitsClient) {
        this.limitsClient = limitsClient;
    }
}
```

You can also retrieve the client from the service registry:

```java
LimitsDPClient client = Services.get(LimitsDPClient.class);
```

Example call:

```java
import com.oracle.oci.limits.requests.GetServiceLimitsRequest;
import com.oracle.oci.limits.responses.GetServiceLimitsResponse;

GetServiceLimitsRequest request = GetServiceLimitsRequest.builder()
        .group("compute")
        .tag("standard")
        .build();

GetServiceLimitsResponse response = client.getServiceLimits(request);
```

---

## Example Application

The repository includes a runnable [Limits example](../../examples/limits/README.md).

The example shows:

* service-registry lookup for `LimitsDPClient`
* default OCI SDK config file loading
* `oci.limits.client` timeout configuration
* a sample `getServiceLimits` call

See the [Limits example configuration](../../examples/limits/src/main/resources/application.yaml)
for the YAML used by the example.

---

## Configuration

The `oci.limits` configuration can be customized using YAML configuration. OCI SDK client configuration settings are
grouped under `oci.limits.client`.

| Config Key | Default Value | Description |
|------------|---------------|-------------|
| `oci.limits.endpoint` | unset | Optional explicit OCI Limits endpoint override. |
| `oci.limits.client.connection-timeout` | `PT10S` | OCI SDK connection timeout. |
| `oci.limits.client.read-timeout` | `PT1M` | OCI SDK read timeout. |
| `oci.limits.client.max-async-threads` | `50` | Maximum async worker threads used by OCI SDK asynchronous helpers and waiters. |

If `client` is absent, the module uses `ClientConfiguration.builder().build()` from the OCI SDK. That provides the
OCI SDK defaults: 10 seconds connection timeout, 60 seconds read timeout, and 50 max async threads.

Example configuration in `application.yaml`:

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  limits:
    endpoint: https://limits-dp.example.oraclecloud.com
    client:
      connection-timeout: PT5S
      read-timeout: PT30S
      max-async-threads: 20
```

When `endpoint` is unset, the Limits client uses the endpoint behavior provided by the underlying
Limits data plane Java client. Set `endpoint` for explicit routing, local tests, or environments
that require a non-default Limits endpoint.

---

## References

* [Limits User Guide](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/Limits)
* [Limits Repository](https://bitbucket.oci.oraclecorp.com/projects/LIM/repos/limits-dp-client/browse)

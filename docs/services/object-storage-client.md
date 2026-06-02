# OCI Object Storage Client

---

## Contents

* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Configuration](#configuration)
* [Usage](#usage)

---

## Overview

The Object Storage Client integration registers the OCI Java SDK
`com.oracle.bmc.objectstorage.ObjectStorage` client with the Helidon service registry.

The module wires the synchronous OCI Java SDK Object Storage client using the shared
OCI SDK authentication provider. It supports explicit endpoint routing, configured
regions, and the shared OCI SDK client configuration options.

---

## Maven Coordinates

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-object-storage-client</artifactId>
</dependency>
```

The module uses the shared OCI SDK authentication provider. Add one authentication
method module to the application, for example instance principals:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-sdk-authentication-instance</artifactId>
</dependency>
```

---

## Configuration

Client settings live under `oci.object-storage-client`:

The `region` and `region-id` settings are both available for user convenience:
`region-id` aligns with the OCI Object Storage API parameter name, while `region`
aligns with the Helidon OCI configuration convention. Configure only one of them;
setting both is rejected as invalid configuration.

| Key | Default Value | Description |
|-----|---------------|-------------|
| `oci.object-storage-client.endpoint` | unset | Explicit Object Storage endpoint. When set, this overrides `region` and `region-id`. |
| `oci.object-storage-client.region` | unset | OCI region used to derive the Object Storage endpoint when `endpoint` is not set. Region values can use public region name, internal name, or airport code. |
| `oci.object-storage-client.region-id` | unset | Alias for `region` using the common OCI region ID name. |
| `oci.object-storage-client.client.connection-timeout` | unset | OCI SDK connection timeout. |
| `oci.object-storage-client.client.read-timeout` | unset | OCI SDK read timeout. |
| `oci.object-storage-client.client.max-async-threads` | unset | Maximum async worker threads used by OCI SDK asynchronous helpers and waiters. |
| `oci.object-storage-client.client.disable-data-buffering-on-upload` | unset | Whether upload buffering should be disabled. |
| `oci.object-storage-client.client.retry` | unset | OCI SDK retry configuration. |
| `oci.object-storage-client.client.circuit-breaker` | unset | OCI SDK circuit breaker configuration. |

When `endpoint` is unset, the module resolves the region from `region` or its
`region-id` alias; if neither is set, it uses the injected region supplier.

Example:

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  object-storage-client:
    region-id: us-ashburn-1
    client:
      connection-timeout: PT10S
      read-timeout: PT1M
      disable-data-buffering-on-upload: true
```

Use `endpoint` instead of `region` for explicit endpoint routing:

```yaml
oci:
  object-storage-client:
    endpoint: https://objectstorage.us-ashburn-1.oraclecloud.com
```

For local tests, point `endpoint` at the test service, for example `http://localhost:8081`.

---

## Usage

Inject the SDK interface from the Helidon service registry:

```java
import io.helidon.service.registry.Service;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;

@Service.Singleton
class Buckets {
    private final ObjectStorage objectStorage;

    @Service.Inject
    Buckets(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    void listBuckets(String namespaceName, String compartmentId) {
        objectStorage.listBuckets(ListBucketsRequest.builder()
                .namespaceName(namespaceName)
                .compartmentId(compartmentId)
                .build());
    }
}
```

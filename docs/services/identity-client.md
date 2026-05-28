# OCI Identity Client

---

## Contents

* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Configuration](#configuration)
* [Usage](#usage)

---

## Overview

The `helidon-oci-identity-client` module contributes the OCI Java SDK
`com.oracle.bmc.identity.Identity` client to the Helidon service registry.

This module is separate from [`helidon-oci-identity`](./identity.md). The existing
`helidon-oci-identity` module wires the internal Auth SDK for request authentication,
authorization, and `IdentityContext`; this module wires the public OCI Java SDK Identity
client used for Identity APIs such as `createTag`, `createTagNamespace`, and tag defaults.

---

## Maven Coordinates

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-identity-client</artifactId>
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

Client settings live under `oci.identity-client`. OCI SDK client configuration settings are grouped under
`oci.identity-client.client`:

| Key | Default Value | Description |
|-----|---------------|-------------|
| `oci.identity-client.client.connection-timeout` | `PT10S` | OCI SDK connection timeout. |
| `oci.identity-client.client.read-timeout` | `PT1M` | OCI SDK read timeout. |
| `oci.identity-client.client.max-async-threads` | `50` | Maximum async worker threads used by OCI SDK asynchronous helpers and waiters. Synchronous Identity calls do not use this pool. |
| `oci.identity-client.endpoint` | unset | Explicit Identity endpoint. When set, this overrides `region` and realm-specific endpoint templates. |
| `oci.identity-client.region` | unset | OCI region used to derive the Identity endpoint when `endpoint` is not set. |
| `oci.identity-client.realm-specific-endpoint-template-enabled` | `false` | Enables OCI SDK realm-specific endpoint templates when `endpoint` is not set. |

If `client` is absent, the module uses `ClientConfiguration.builder().build()` from the OCI SDK. That provides the
OCI SDK defaults: 10 seconds connection timeout, 60 seconds read timeout, and 50 max async threads.

The module registers the synchronous OCI SDK `Identity` client. Synchronous OCI SDK
calls execute on the thread that calls the client. In Helidon applications that means
the usual request-flow code can call the synchronous API directly and let Helidon's
virtual threads handle blocking I/O.

Example:

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  identity-client:
    region: us-ashburn-1
    client:
      connection-timeout: PT10S
      read-timeout: PT1M
      max-async-threads: 50
```

Use `endpoint` instead of `region` for tests or explicit endpoint routing:

```yaml
oci:
  identity-client:
    endpoint: http://localhost:8081
```

---

## Usage

Inject the SDK interface from the Helidon service registry:

```java
import io.helidon.service.registry.Service;

import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.model.CreateTagDetails;
import com.oracle.bmc.identity.requests.CreateTagRequest;

@Service.Singleton
class Tags {
    private final Identity identity;

    @Service.Inject
    Tags(Identity identity) {
        this.identity = identity;
    }

    void createTag(String tagNamespaceId, String tagName, String description) {
        identity.createTag(CreateTagRequest.builder()
                .tagNamespaceId(tagNamespaceId)
                .createTagDetails(CreateTagDetails.builder()
                                          .name(tagName)
                                          .description(description)
                                          .build())
                .build());
    }
}
```

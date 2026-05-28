# Limits

---

## Overview

The limits module provides support for OCI limits integration.

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

The limits module provides a Helidon service that can be used to interact with OCI Limits. You can obtain a `LimitsDPClient` instance using the `Services` class:

```java
LimitsDPClient client = Services.get(LimitsDPClient.class);
```

You can then use the `client` instance to perform operations such as checking limits and quotas.

Make sure to configure the `oci.limits` properties in your YAML configuration file to customize the behavior of the limits module. Refer to the [Configuration](#configuration) section for more details.

---

## Configuration

The `oci.limits` configuration can be customized using YAML configuration. OCI SDK client configuration settings are
grouped under `oci.limits.client`. The following properties are supported:

* `client.connection-timeout`: The OCI SDK connection timeout duration. Default is PT10S (10 seconds).
* `client.read-timeout`: The OCI SDK read timeout duration. Default is PT1M (one minute).
* `client.max-async-threads`: The maximum number of asynchronous threads. Default is 50.
* `endpoint`: Optional explicit OCI Limits endpoint override, directly under `oci.limits`.

Example configuration in `application.yaml`:

```yaml
oci:
  limits:
    client:
      connection-timeout: PT5S
      read-timeout: PT30S
      max-async-threads: 20
    endpoint: https://limits.example.oraclecloud.com
```

---

## References

* [Limits User Guide](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/Limits)
* [Limits Repository](https://bitbucket.oci.oraclecorp.com/projects/LIM/repos/limits-dp-client/browse)

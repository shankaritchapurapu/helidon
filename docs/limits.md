# Limits

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

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

The `oci.limits` configuration can be customized using YAML configuration. The following properties are supported:

* `connection-timeout`: The connection timeout duration. Default is PT10S (10 seconds).
* `read-timeout`: The read timeout duration. Default is PT1M (one minute).
* `max-async-threads`: The maximum number of asynchronous threads. Default is 10.

Example configuration in `application.yaml`:

```yaml
oci:
  limits:
    connection-timeout: PT5S
    read-timeout: PT30S
    max-async-threads: 20
```

---

## References

* [Limits User Guide](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/Limits)
* [Limits Repository](https://bitbucket.oci.oraclecorp.com/projects/LIM/repos/limits-dp-client/browse)

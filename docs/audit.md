# Audit

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

---

## Overview

The Audit module provides support for OCI AuditV2Filter integration, which is required for setting up OCI Audit. The AuditV2Filter is used to automatically capture and log HTTP requests and responses according to configurable audit rules, ensuring relevant user and resource activity is recorded for compliance, security, and troubleshooting purposes..

---

## Maven Coordinates

To enable AuditV2Filter, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-project</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

The AuditV2Filter will be registered on the WebServer using [AuditV2Feature](../audit/src/main/java/com/oracle/helidon/oci/audit/AuditV2Feature.java), which is a [Helidon Server Feature](https://helidon.io/docs/v4/se/webserver/webserver#_server_features). When the [Service Registry](https://helidon.io/docs/v4/se/injection/injection#generate-binding) is started on an application as shown below:
```java
ServiceRegistryManager.start(ApplicationBinding.create());
```
the AuditV2Feature will be configured into the WebServer automatically. Otherwise, you need to configure it explicitly in code by registering it with the WebServer:
```java
WebServer.builder()
         .addFeature(Services.get(AuditV2Feature.class))
         .build();
```

---

## Configuration

Configure AuditV2Filter behavior using the `application.yaml` file.

| Config Key                             | Default Value               | Description                                                                                           |
|----------------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------|
| oci.audit.enabled                      | true                        | Whether to enable the AuditV2Filter.                                                                  |
| oci.audit.event-source                 | EventSourceNotConfigured    | Identifies the event source to be set in audit events.                                                |
| oci.audit.respect-splat-audited-flag   | true                        | Whether to respect the `oci-splat-audited` request header to conditionally disable auditing.          |
| oci.audit.request-parameter-rules      | []                          | List of rules for filtering/auditing HTTP request parameters.                                         |
| oci.audit.request-header-rules         | []                          | List of rules for filtering/auditing HTTP request headers.                                            |
| oci.audit.response-header-rules        | []                          | List of rules for filtering/auditing HTTP response headers.                                           |

**Rule configuration format** (`request-parameter-rules`, `request-header-rules`, `response-header-rules`):  
Each rule should have the following fields:
- `resources`: String for matching resource paths
- `actions`: String for matching HTTP methods/actions
- `values`: String for matching parameter/header names

**Example configuration:**
```yaml
oci:
  audit:
    enabled: true
    event-source: MyService
    respect-splat-audited-flag: true
    request-parameter-rules:
      - resources: "/orders"
        actions: "POST"
        values: "orderId"
    request-header-rules:
      - resources: "/admin"
        actions: "GET"
        values: "Authorization"
    response-header-rules:
      - resources: "/public"
        actions: "GET"
        values: "Content-Type"

---

## References

* [Audit v2 User Guide](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/MON-2%3A+Events+and+Audit+v2)
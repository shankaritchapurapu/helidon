# Audit

---

## Overview

The Audit integration registers OCI `AuditV2Filter` support with Helidon WebServer. The filter
captures and logs HTTP requests and responses according to configurable audit rules so relevant
user and resource activity can be recorded for compliance, security, and troubleshooting.

---

## Maven Coordinates

To enable audit support, add the following dependency to your project’s `pom.xml`:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-audit</artifactId>
</dependency>
```

Direct `AuditPayloadAppender` endpoint parameter injection also requires the Helidon OCI codegen processor at
application compile time. Add `helidon-oci-codegen` to the Maven compiler annotation processor path alongside the
normal Helidon annotation processors, using the same Helidon OCI version as `helidon-oci-audit`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.helidon.bundles</groupId>
                <artifactId>helidon-bundles-apt</artifactId>
                <version>${helidon.version}</version>
            </path>
            <path>
                <groupId>com.oracle.helidon.oci</groupId>
                <artifactId>helidon-oci-codegen</artifactId>
                <version>${helidon.oci.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## Usage

`AuditV2Filter` is registered on the WebServer using `AuditV2Feature`, which is a
[Helidon Server Feature](https://helidon.io/docs/v4/se/webserver/webserver#_server_features). When the
[Service Registry](https://helidon.io/docs/v4/se/injection/injection#generate-binding) is started on an
application as shown below:
```java
ServiceRegistryManager.start(ApplicationBinding.create());
```
the Audit feature is configured into the WebServer automatically. Otherwise, you can register it explicitly:

```java
WebServer.builder()
         .addFeature(Services.get(AuditV2Feature.class))
         .build();
```

To verify the filter ran during local testing, send the `oci-splat-audit-verify: true` request header. The
response will include `oci-splat-audit-event-summary` as a JSON array. Each entry contains `eventId`.
If `respect-splat-audited-flag` is enabled, sending `oci-splat-audited: true`
skips audit emission and verification summary creation for that request, while leaving the request appender available
to endpoint code.

The feature is registered on the default WebServer socket and on every configured named socket.

With the codegen processor configured, application code can enrich the current request's event by injecting
Sherlock's request-scoped `AuditPayloadAppender` into an endpoint method:

```java
@Http.POST
@Http.Path("/orders")
OrderView create(@Http.Entity CreateOrder request, AuditPayloadAppender audit) {
    audit.setEventName("CreateOrder", OperationSynchronousType.None);
    audit.overridePrincipalTenantId(request.tenantId());
    audit.overrideCompartmentId(request.compartmentId());
    audit.setResourceId(request.orderId());
    audit.appendToAuditRios(List.of(new AuditRIO(request.compartmentId(), request.orderId())));
    return service.create(request);
}
```

Direct endpoint injection fails fast if the request appender is not present.

---

## Example Application

The repository includes a runnable [audit example](../../examples/audit/README.md).

The example provides:

* a small Helidon service with `GET` and `POST` routes
* `oci.auditv2` YAML configuration with request-parameter, request-header, and response-header rules
* a verification flow using `oci-splat-audit-verify`
* an opt-out flow using `oci-splat-audited`

The example README includes build, run, and test commands.

---

## Configuration

Configure `AuditV2Filter` behavior using the `application.yaml` file.

| Config Key                             | Default Value               | Description                                                                                           |
|----------------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------|
| oci.auditv2.enabled                    | true                        | Whether to enable the AuditV2Filter.                                                                  |
| oci.auditv2.event-source               | EventSourceNotConfigured    | Identifies the event source to be set in audit events.                                                |
| oci.auditv2.event-name                 | HttpRequest                 | Event name used when application code does not set a more specific event.                             |
| oci.auditv2.tenant-id                  |                             | Tenant OCID used when application code does not set the principal tenant.                             |
| oci.auditv2.compartment-id             |                             | Compartment OCID used when application code does not set a target compartment.                        |
| oci.auditv2.resource-id                |                             | Resource identifier used when application code does not set one.                                      |
| oci.auditv2.resource-name              |                             | Resource name used when application code does not set one.                                            |
| oci.auditv2.respect-splat-audited-flag | true                        | Whether to respect the `oci-splat-audited` request header to conditionally disable auditing.          |
| oci.auditv2.request-parameter-rules    | []                          | List of rules for filtering or auditing HTTP request parameters.                                      |
| oci.auditv2.request-header-rules       | []                          | List of rules for filtering or auditing HTTP request headers.                                         |
| oci.auditv2.response-header-rules      | []                          | List of rules for filtering or auditing HTTP response headers.                                        |

**Rule configuration format** (`request-parameter-rules`, `request-header-rules`, `response-header-rules`):  
Each rule should have the following fields:
- `resources`: String for matching resource paths
- `actions`: String for matching HTTP methods/actions
- `values`: String for matching parameter/header names

**Example configuration:**

```yaml
oci:
  auditv2:
    enabled: true
    event-source: MyService
    event-name: HttpRequest
    tenant-id: ocid1.tenancy.oc1..example
    compartment-id: ocid1.compartment.oc1..example
    resource-id: my-service
    resource-name: My Service
    respect-splat-audited-flag: true
    request-parameter-rules:
      - resources: "/orders"
        actions: "POST"
        values: "orderId"
      - resources: "/orders/{orderId}"
        actions: "GET"
        values: "includeDetails"
    request-header-rules:
      - resources: "/admin"
        actions: "GET"
        values: "Authorization"
      - resources: "/orders"
        actions: "POST"
        values: "opc-request-id"
    response-header-rules:
      - resources: "/public"
        actions: "GET"
        values: "Content-Type"
      - resources: "/orders"
        actions: "POST"
        values: "etag"
```

For a complete runnable configuration, see the
[audit example configuration](../../examples/audit/src/main/resources/application.yaml).

---

## References

* [Audit example](../../examples/audit/README.md)
* [Audit example configuration](../../examples/audit/src/main/resources/application.yaml)
* [Audit v2 User Guide](https://confluence.oraclecorp.com/confluence/display/OCIPLAT/MON-2%3A+Events+and+Audit+v2)

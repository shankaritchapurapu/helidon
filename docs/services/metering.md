# Metering

---

## Overview

The metering integrations provide Helidon-friendly configuration, service registry bindings, and declarative
annotations for Helidon OCI applications that need to report OCI metering.

There are two metering modules. Applications should use one of them, based on the OCI service style they are building:

1. `helidon-oci-metering-dp` for data plane services using emitter-dp.
2. `helidon-oci-metering-cp` for control plane services using metering-agent.

Both modules use the same application configuration root, `oci.metering`, and both provide a `Metering` class
with annotations for:

* recording a single metering point
* timing one method invocation
* marking the start and end of a bounded metering region
* adding static and parameter-derived tags

The modules intentionally do not expose a shared DP/CP application API. The native emitter libraries support different
use cases and configuration, so each module keeps its own package and service bindings.

---

## Maven Coordinates

Add exactly one metering dependency to your application.

For a data plane service:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.metering</groupId>
    <artifactId>helidon-oci-metering-dp</artifactId>
</dependency>
```

For a control plane service:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.metering</groupId>
    <artifactId>helidon-oci-metering-cp</artifactId>
</dependency>
```

The metering annotations have source retention and are processed at application compile time. Use the normal Helidon
service/codegen annotation processing setup for applications that use declarative metering annotations.

---

## Usage

### Choose DP or CP

Use the DP module when your service should record through emitter-dp's `LogFileUsageRecorder`.

Use the CP module when your service should record through metering-agent. CP supports two recording styles:

1. `direct`, which sends bounded events through metering-agent's authenticated direct client.
2. `agent`, which records bounded events through metering-agent's Kiev-backed agent path.

Only one CP style should be configured in an application.

### Configure DP

At minimum, configure the DP endpoint:

```yaml
oci:
  metering:
    endpoint: "https://bling.example.internal"
```

A fuller DP configuration can provide the values needed by emitter-dp:

```yaml
oci:
  metering:
    endpoint: "https://bling.example.internal"
    metering-period: "PT1M"
    archiving-duration: "PT1H"
    metering-dir: "/var/opt/oracle/metering"
    client-id: "orders-dp"
    service: "orders"
    os-enabled: false
    k8s-based-deployment: false
    host-name: "orders-host-1"
```

When the DP module is on the classpath, Helidon can create and inject:

* `com.oracle.helidon.oci.metering.dp.MeteringConfig`
* `com.oracle.pic.bling.config.MeteringAgentConfig`
* `com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder`

### Configure CP direct

The direct CP style sends metering events through metering-agent's direct client. It also needs a
`com.oracle.bmc.auth.BasicAuthenticationDetailsProvider` in the Helidon service registry.

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  metering:
    direct:
      endpoint: "https://bling.example.internal"
      client-id: "orders-cp"
```

When the CP direct config is present, Helidon can create and inject:

* `com.oracle.helidon.oci.metering.cp.MeteringRecorder`
* `com.oracle.pic.bling.emit.client.MeteringClient`

### Configure CP agent

The agent CP style uses `metering-agent`'s Kiev-backed metering log store path. Application code must run the metered call in
a Kiev transaction when recording through this style.

```yaml
oci:
  metering:
    agent:
      endpoint: "https://bling.example.internal"
      client-id: "orders-cp"
      metering-period: "PT1M"
      bucket-configs:
        - bucket-name: "orders_metering"
          service-name: "orders"
          meter-name: "orders.requests"
```

When the CP agent config is present, Helidon can create and inject:

* `com.oracle.helidon.oci.metering.cp.MeteringRecorder`
* `com.oracle.pic.bling.emit.config.MeteringAgentConfig`
* `com.oracle.pic.bling.emit.MeteringLogStores`

The CP module creates `MeteringLogStores` from a registry-provided `com.oracle.pic.kiev.mapping.MappedDataStore`.
Applications can get that from the Helidon OCI Kiev module by adding and configuring `helidon-oci-kiev`, or they can
provide their own `MappedDataStore` supplier.

### Use declarative metering

Both DP and CP expose a module-specific `Metering` class. Import the one for the module your application uses:

```java
import com.oracle.helidon.oci.metering.dp.Metering;
```

or:

```java
import com.oracle.helidon.oci.metering.cp.Metering;
```

Record a single point after a method succeeds:

```java
@Metering.Point(value = "orders.created",
                tags = @Metering.Tag(key = "source", value = "api"))
void createOrder(@Metering.CompartmentId String compartmentId,
                 @Metering.ResourceId String orderId,
                 @Metering.Amount float amount,
                 @Metering.TagValue("operation") String operation) {
    // application work
}
```

Record elapsed time for one method invocation:

```java
@Metering.Timed(value = "orders.lookup")
Order lookup(@Metering.CompartmentId String compartmentId,
             @Metering.ResourceId String orderId,
             @Metering.TagValue("operation") String operation) {
    return findOrder(orderId);
}
```

Record an explicitly bounded region using `@Metering.Start` and `@Metering.End`:

```java
@Metering.Start("orders.workflow")
void beginWorkflow(@Metering.CompartmentId String compartmentId,
                   @Metering.ResourceId String orderId,
                   @Metering.TagValue("phase") String phase) {
}

@Metering.End(tags = @Metering.Tag(key = "completed", value = "true"))
void endWorkflow(@Metering.TagValue("result") String result) {
}
```

Metering regions use the current Helidon `Context`. If no current context exists, a method intercepted by
`@Metering.Start` or `@Metering.End` can declare an `io.helidon.common.context.Context` parameter. Regions cannot be
nested in the same context.

### Use CP programmatic recording

CP applications can inject `MeteringRecorder` and record a bounded event directly:

```java
class OrderProcessing {
    private final MeteringRecorder recorder;

    OrderMetering(MeteringRecorder recorder) {
        this.recorder = Services.get(MeteringRecorder.class);
    }

    void recordOrderCreated(String compartmentId, String resourceId) throws Exception {
        Instant now = Instant.now();
        MeteringEvent event = MeteringEvent.builder()
                .meterName("orders.created")
                .compartmentId(compartmentId)
                .resourceId(resourceId)
                .from(now)
                .to(now)
                .amount(1.0D)
                .tags(Map.of("operation", "create"))
                .build();

        recorder.record(event);
    }
}
```

The CP recorder also supports `unwrap(Class<T>)` for code that needs direct access to the native metering-agent delegate.

Services can also retrieve the `MappedDataStore` if they want to work directly with it:

```java
    var mappedDataStore = Services.get(MappedDataStore.class);
```

---

## Configuration

When a Helidon blueprint maps to a native emitter configuration object, the metering modules avoid duplicating native
defaults. Required values are enforced by Helidon only when the integration itself must require them; optional values
are passed to the native builder only when present.

### DP configuration

| Key                                      | Default value  | Description |
|------------------------------------------|----------------|-------------|
| `oci.metering.endpoint`                  |                | Bling ingest endpoint. |
| `oci.metering.metering-period`           | native default | Period used by emitter-dp for metering work. |
| `oci.metering.archiving-duration`        | native default | Duration for retaining archived metering data. |
| `oci.metering.metering-dir`              | native default | Local directory used by emitter-dp. |
| `oci.metering.client-id`                 | native default | Metering client ID. |
| `oci.metering.service`                   | native default | Service name reported to Bling. |
| `oci.metering.os-enabled`                | native default | Whether Object Storage reporting is enabled. |
| `oci.metering.k8s-based-deployment`      | native default | Whether the deployment is Kubernetes-based. |
| `oci.metering.bucket-name`               | native default | Object Storage bucket name used by emitter-dp. |
| `oci.metering.namespace`                 | native default | Object Storage namespace used by emitter-dp. |
| `oci.metering.report-to-bling-frequency` | native default | Frequency for reporting archived usage to Bling. |
| `oci.metering.host-name`                 | local host     | Host name passed to `LogFileUsageRecorder`. |

### CP direct configuration

Required when `oci.metering.direct` is present.

| Key                              | Default value | Description |
|----------------------------------|---------------|-------------|
| `oci.metering.direct.endpoint`   |               | Bling ingest endpoint. |
| `oci.metering.direct.client-id`  |               | Metering client ID. |

### CP agent configuration

Required when `oci.metering.agent` is present.

| Key                                                     | Default value  | Description |
|---------------------------------------------------------|----------------|-------------|
| `oci.metering.agent.endpoint`                           |                | Bling ingest endpoint. |
| `oci.metering.agent.client-id`                          |                | Metering client ID. |
| `oci.metering.agent.bucket-configs[].bucket-name`       |                | Metering bucket name. |
| `oci.metering.agent.bucket-configs[].service-name`      |                | Service name associated with the bucket. |
| `oci.metering.agent.bucket-configs[].meter-name`        |                | Meter name associated with the bucket. |
| `oci.metering.agent.max-workers`                        | native default | Maximum workers for the full control plane agent path. |
| `oci.metering.agent.metering-period`                    | native default | Period used by metering-agent for metering work. |
| `oci.metering.agent.canary-disabled`                    | native default | Whether canary behavior is disabled. |
| `oci.metering.agent.max-archive-workers`                | native default | Maximum archive workers. |
| `oci.metering.agent.scan-page-size`                     | native default | Scan page size. |
| `oci.metering.agent.max-writes-per-transaction`         | native default | Maximum writes per transaction. |
| `oci.metering.agent.retention-period`                   | native default | Retention period for archived data. |
| `oci.metering.agent.skip-archive-lease-check`           | native default | Whether archive lease checking should be skipped. |
| `oci.metering.agent.lease-dao-scan-page-size`           | native default | Lease DAO scan page size. |
| `oci.metering.agent.fast-catchup-mode-enabled`          | native default | Whether fast catchup mode is enabled. |

### Annotation values

| Annotation | Description |
|------------|-------------|
| `@Metering.Point` | Records one metering point after the method invocation succeeds. |
| `@Metering.Timed` | Records elapsed seconds for one method invocation. |
| `@Metering.Start` | Starts an explicitly bounded metering region in the current Helidon context. |
| `@Metering.End` | Ends the active metering region and records elapsed seconds. |
| `@Metering.Tag` | Adds a static tag to a type or method. Method tags override type tags with the same key. |
| `@Metering.CompartmentId` | Marks a method parameter containing the compartment ID. |
| `@Metering.ResourceId` | Marks a method parameter containing the metered resource ID. |
| `@Metering.Amount` | Marks a method parameter containing the point measurement amount. |
| `@Metering.TagValue` | Marks a method parameter whose value should be recorded as a tag. |

`@Metering.Point`, `@Metering.Timed`, and `@Metering.Start` can also declare static `compartmentId` and `resourceId`
values. If both static values and annotated parameters are absent, the recorded value is left unset and the native
emitter library performs its own validation.

---

## References

* [OCI metering overview](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/landing-metering.htm)
* [OCI metering endpoints](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/metering-service-integration/metering-endpoints.htm)
* [Control plane metering](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/metering-service-integration/metering-agent.htm)
* [Data plane metering](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/metering-service-integration/data-plane-metering-agent.htm)
* [Helidon OCI Kiev integration](kiev.md)

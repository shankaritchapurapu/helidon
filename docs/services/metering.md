# Metering

---

## Overview

The Metering integrations register native OCI metering runtime support with the Helidon service
registry. When the selected metering module is on the classpath and the service registry starts,
Helidon creates the native metering agent from `oci.metering` configuration and starts or stops it
with the application lifecycle.

There are two metering modules. Applications should use one of them, based on the OCI service style
they are building:

1. `helidon-oci-metering-dp` for data plane services using emitter-dp.
2. `helidon-oci-metering-cp` for control plane services using emitter-cp.

Both modules use the same application configuration root, `oci.metering`.

The CP module also registers the support needed by the `Metering` annotations for:

* recording a single metering point
* timing one method invocation
* marking the start and end of a bounded metering region
* adding static and parameter-derived tags

The modules intentionally do not expose a shared DP/CP application API. The native emitter libraries support different
use cases and configuration, so each module keeps its own package and service bindings.

---

## Maven Coordinates

Add exactly one Helidon Talon metering dependency to your service.

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

The metering annotations in CP are processed at application compile time. Use the normal Helidon
service/codegen annotation processing setup for applications that use declarative metering annotations.

An OCI service using the Helidon Talon Metering CP library must make sure that a `@Service.Singleton Supplier<MappedDataStore>` Helidon service factory class is on the classpath. You can do this in any of the following ways:
* Add a dependency on the Helidon Talon Kiev integration library:
  
  ```xml
  <dependency>
    <groupId>com.oracle.helidon.oci.kiev</groupId>
    <artifactId>helidon-oci-kiev</artifactId>
  </dependency>
  ```
* Write your own factory:
  ```java
  @Services.Singleton
  class MyMappedDataStoreFactory implements Supplier<MappedDataStore> {
     ...
  }
  ```
* Add a dependency to some other library that contains such a factory.

---

## Usage

### Choose DP or CP

Use the DP module when your service should record through emitter-dp's `LogFileUsageRecorder`.

Use the CP module when your service should record through metering-agent's Kiev-backed agent path.

### Start The Service Registry

Both metering modules use Helidon service run levels to start and stop their native metering agents. Services must start the
Helidon service registry during application startup so the metering runtime reaches the startup run level:

```java
import io.helidon.service.registry.ApplicationBinding;
import io.helidon.service.registry.ServiceRegistryManager;

ServiceRegistryManager.start(ApplicationBinding.create());
```

Without this startup call, Helidon Talon cannot start or stop the DP
`MeteringReportingAgent` or CP `MeteringAgent`  automatically. A service does not need to prime the `ServiceRegistryManager` this way if it wants to prepare and build the configuration explicitly and start and stop the OCI metering agent itself.

### Configure DP

The DP config blueprint requires the native emitter-dp values that the native builder requires:

```yaml
oci:
  metering:
    enabled: true
    endpoint: https://bling.example.internal
    metering-dir: "/var/opt/oracle/metering"
    client-id: "orders-dp"
    service: "orders"
    os-enabled: false
    k8s-based-deployment: false
```

The two Object Storage settings above intentionally disable the native emitter-dp Object Storage backup path for a
minimal no-backup setup. If you omit them, Helidon delegates to emitter-dp's native defaults. In emitter-dp 1.0.0.9
those defaults enable Object Storage backup and Kubernetes deployment mode, so the native config validation also
requires `bucket-name` and `namespace`.

When the service relies on Helidon to create the native DP clients, provide a
`BasicAuthenticationDetailsProvider` through the Helidon service registry and add client-specific config for the
native Bling publisher client. DP metering uses the `ObjectStorageClient` supplied by the shared Object Storage module
from `oci.object-storage-client` configuration. See the
[Object Storage client](object-storage-client.md) documentation for the full set of supported Object Storage settings.

```yaml
oci:
  metering:
    bling-publisher-client:
      endpoint: https://bling.example.internal
      client-id: "orders-dp"
  object-storage-client:
    endpoint: https://objectstorage.us-phoenix-1.oraclecloud.com
    region: "us-phoenix-1"
```

Service code can also provide a prebuilt native `BlingPublisherClient` programmatically on the `MeteringConfig`
builder. DP metering receives its `ObjectStorageClient` from the Helidon service registry; applications that need a
custom Object Storage client can provide their own service binding instead of using the shared Object Storage module.
`oci.metering.region` is needed when Helidon constructs the native `LogFileUsageReporter`; it does not configure the
Object Storage client.

A fuller DP configuration can provide the values needed by emitter-dp:

```yaml
oci:
  metering:
    enabled: true
    endpoint: https://bling.example.internal
    metering-period: "PT1M"
    archiving-duration: "PT1H"
    metering-dir: "/var/opt/oracle/metering"
    client-id: "orders-dp"
    service: "orders"
    region: "us-phoenix-1"
    report-interval: "PT5M"
    bling-publisher-client:
      endpoint: https://bling.example.internal
      client-id: "orders-dp"
    os-enabled: false
    k8s-based-deployment: false
    host-name: "orders-host-1"
  object-storage-client:
    endpoint: https://objectstorage.us-phoenix-1.oraclecloud.com
    region: "us-phoenix-1"
```

### Configure CP

CP uses `metering-agent`'s Kiev-backed metering log store path. Service code must run the metered call in
a Kiev transaction when recording through this style.

```yaml
oci:
  metering:
    enabled: true
    endpoint: https://bling.example.internal
    client-id: "orders-cp"
    region: "us-phoenix-1"
    metering-period: "PT1M"
    bucket-configs:
      - bucket-name: "orders_metering"
        service-name: "orders"
        meter-name: "orders.requests"
```

When the CP config is present, Helidon can create and inject:

* `com.oracle.pic.bling.emit.MeteringLogStores`
* `com.oracle.pic.bling.emit.config.MeteringAgentConfig`

NOTE: For CP metering, Helidon Talon for CP metering requires a Helidon service factory for the OCI type 
`com.oracle.pic.kiev.mapping.MappedDataStore` but does not provide one itself. Services can get a `MappedDataStore` factory from the [Helidon Talon Kiev module](kiev.md) by
adding and configuring `helidon-oci-kiev`, or service developers can provide their own factory instead.

Service code can inject or look up `MeteringLogStores`, select the specific native `MeteringLogStore` for a meter name, and
update that store in the current Kiev transaction. 

### Use CP declarative metering

The CP module exposes a `Metering` class for declarative metering:

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
`@Metering.Start` or `@Metering.End` can declare an `io.helidon.common.context.Context` parameter. Regions can overlap
in the same context; `@Metering.End` ends the most recent matching meter name, or the most recent region if no meter
name is specified.


### Use DP programmatic recording

Note that DP does not expose metering annotations. The native data plane API is batch-oriented, so DP applications should gather
appropriate batches and record them programmatically.

DP services can inject `LogFileUsageRecorder` and pass native DP `Meters` batches to emitter-dp:

```java
import com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder;

class OrderMetering {
    private final LogFileUsageRecorder recorder;

    OrderMetering(LogFileUsageRecorder recorder) {
        this.recorder = recorder;
    }

    void recordBatch(List<Meters> meters) throws Exception {
        recorder.recordMetersAsync(meters).call();
    }
}
```

### Use CP programmatic recording

CP services can inject `MeteringLogStores`, select the `MeteringLogStore` for the meter they need to update, and
invoke the native metering-agent API directly:

```java
import java.time.Instant;
import java.util.Map;

import com.oracle.helidon.oci.metering.cp.Tags;
import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.bling.emit.store.MeteringLogStore;
import com.oracle.pic.kiev.Transaction;

class OrderProcessing {
    private final MeteringLogStores logStores;

    OrderProcessing(MeteringLogStores logStores) {
        this.logStores = logStores;
    }

    void recordOrderCreated(Transaction transaction,
                            String compartmentId,
                            String resourceId) throws Exception {
        Instant now = Instant.now();
        MeteringLogStore logStore = logStores.getByMeterName("orders.created");

        logStore.addMeter(transaction,
                          resourceId,
                          compartmentId,
                          now,
                          now,
                          1.0D,
                          Tags.toJson(Map.of("operation", "create")));
    }
}
```

Services can also retrieve the `MappedDataStore` if they want to work directly with it:

```java
    var mappedDataStore = Services.get(MappedDataStore.class);
```

---

## Configuration

When a Helidon blueprint maps to a native emitter configuration object, the metering modules avoid duplicating native
defaults. Required values are enforced by Helidon only when the integration itself must require them; optional values
are passed to the native builder only when present.

In the tables below, `native default` means Helidon does not set that value when the property is
omitted. The default comes from the underlying emitter library: emitter-dp for DP configuration
and metering-agent for CP configuration.

### DP configuration

| Key                                      | Default value  | Description |
|------------------------------------------|----------------|-------------|
| `oci.metering.enabled`                   | `true`         | Whether Helidon starts and stops the native reporting agent. |
| `oci.metering.endpoint`                  | required       | Bling ingest endpoint. |
| `oci.metering.metering-period`           | native default | Period used by emitter-dp for metering work. |
| `oci.metering.archiving-duration`        | native default | Duration for retaining archived metering data. |
| `oci.metering.metering-dir`              | required       | Local directory used by emitter-dp. |
| `oci.metering.client-id`                 | required       | Metering client ID for the native emitter-dp config. |
| `oci.metering.service`                   | required       | Service name reported to Bling. |
| `oci.metering.region`                    |                | OCI public region name used when constructing the native DP reporter. |
| `oci.metering.os-enabled`                | native default | Whether Object Storage reporting is enabled. |
| `oci.metering.k8s-based-deployment`      | native default | Whether the deployment is Kubernetes-based. |
| `oci.metering.bucket-name`               | native default | Object Storage bucket name used by emitter-dp; required by native validation when Object Storage backup remains enabled. |
| `oci.metering.namespace`                 | native default | Object Storage namespace used by emitter-dp; required by native validation when Object Storage backup remains enabled. |
| `oci.metering.report-interval`           | native default | Duration interval for reporting archived usage to Bling; must be at least `PT1S` and is converted to seconds for emitter-dp. |
| `oci.metering.host-name`                 | local host     | Host name passed to `LogFileUsageRecorder`. |
| `oci.metering.bling-publisher-client.endpoint` | required when using generated client | Bling endpoint for the native publisher client. |
| `oci.metering.bling-publisher-client.client-id` | required when using generated client | Client ID for the native publisher client. |
| `oci.object-storage-client.*`            |                | Object Storage client configuration used by the shared Object Storage module that supplies DP metering's injected client. |

### CP configuration

Required when `helidon-oci-metering-cp` is present.

| Key                                                     | Default value  | Description |
|---------------------------------------------------------|----------------|-------------|
| `oci.metering.enabled`                                  | `true`         | Whether Helidon starts and stops the native metering agent. |
| `oci.metering.endpoint`                                 |                | Bling ingest endpoint. |
| `oci.metering.client-id`                                |                | Metering client ID. |
| `oci.metering.region`                                   | required       | OCI public region name used by the native metering agent. |
| `oci.metering.host-name`                                | local host     | Host name passed to the native metering agent. |
| `oci.metering.bucket-configs[].bucket-name`             |                | Metering bucket name. |
| `oci.metering.bucket-configs[].service-name`            |                | Service name associated with the bucket. |
| `oci.metering.bucket-configs[].meter-name`              |                | Meter name associated with the bucket. |
| `oci.metering.max-workers`                              | native default | Maximum workers for the full control plane agent path. |
| `oci.metering.metering-period`                          | native default | Period used by metering-agent for metering work. |
| `oci.metering.canary-disabled`                          | native default | Whether canary behavior is disabled. |
| `oci.metering.max-archive-workers`                      | native default | Maximum archive workers. |
| `oci.metering.scan-page-size`                           | native default | Scan page size. |
| `oci.metering.max-writes-per-transaction`               | native default | Maximum writes per transaction. |
| `oci.metering.retention-period`                         | native default | Retention period for archived data. |
| `oci.metering.skip-archive-lease-check`                 | native default | Whether archive lease checking should be skipped. |
| `oci.metering.lease-dao-scan-page-size`                 | native default | Lease DAO scan page size. |
| `oci.metering.fast-catchup-mode-enabled`                | native default | Whether fast catchup mode is enabled. |

### CP annotation values

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

`@Metering.Point`, `@Metering.Timed`, `@Metering.Start`, and `@Metering.End` support `measureOnFailure`, which defaults
to `false`. When it is `true`, the generated interceptor records or preserves the metered region even if the annotated
method fails. Recording failures from the metering libraries are logged and do not interrupt the annotated method.

---

## References

* [OCI metering overview](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/landing-metering.htm)
* [OCI metering endpoints](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/metering-service-integration/metering-endpoints.htm)
* [Control plane metering](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/metering-service-integration/metering-agent.htm)
* [Data plane metering](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/metering/metering-service-integration/data-plane-metering-agent.htm)
* [Helidon Talon Kiev integration](kiev.md)

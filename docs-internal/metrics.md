# OCI Metrics Integration Notes

## Basic Functionality
The Helidon Talon metrics integration library automatically sends the following metrics to the telemetry backend:
* Any Helidon metric registered by the service or its dependencies, whether imperatively using the neutral Helidon metrics API or declaratively using the Helidon `@Metrics.Timed` and `@Metrics.Counted` annotations.
* Several JVM-related gauges.
* A family of service-core-compatible automatic HTTP metrics covering all incoming HTTP requests:
  * `<scope>.Time`
  * `<scope>.ResourceTime`
  * `<scope>.WireReadTime`
  * `<scope>.WireWriteTime`
  * `<scope>.ResponseOut.StatusCode.<statusCode>.Count`
  * `<scope>.ResponseOut.StatusFamily.<n>XX.Count`
  * `<scope>.ResponseOut.Count`
  * `<scope>.SuccessRate`

  These metrics intentionally do not use automatic `method`, `route`, or `status.family` tags. The endpoint identity is
  encoded in the metric scope so migrated services can continue to use service-core-style dashboards and alerts.

## Overall design
### Overview
OCI Helidon metrics integration provides a new implementation of the Helidon neutral metrics API, with these key aspects:
* Delegates to some other underlying provider (Micrometer is the only one as of this writing) to store data for each meter.
* Augments each update to a meter with a call to the OCI metrics library to record the meter's updated value.
* Implements a new Helidon metrics publisher type, `oci`, which captures the configurable aspects of the connection to the backend.

### Handling Gauges
Gauges are wrappers around values which are updated _outside_ the Helidon metrics API. As a result, there is no way we can intercept updates to the values which underlie gauges. 

Instead, this library does the following:
* Implements gauges so they retain the previous value reported. 
* Periodically gathers all known gauges and calls the OCI metrics library to record gauge values which have changed. 
  
  Users can configure the gauge sampling behavior:
  * whether to sample gauges at all, and
  * what sampling interval to use.

### General Configuration
There are many configurable settings related to connecting to the backend and retrying failed transmissions, all exposed as attributes of the OCI metrics publisher. These are implemented as Helidon config blueprints and so are settable through configuration files or programmatically.

### Reporter-Style Configuration
The OCI metrics publisher exposes settings which correspond to DropWizard's `BaseReporterFactory` reporter controls:
## Automatic HTTP metrics

The automatic HTTP metrics use the service-core naming convention, but the endpoint identity comes from OCI Helidon
code generation instead of Jersey runtime metadata.

For a generated `@RestServer.Endpoint` method, the default scope is:

```text
ClassSimpleName.methodName
```

For example, `StoreEndpoint.listItems` emits:

```text
StoreEndpoint.listItems.Time
StoreEndpoint.listItems.ResourceTime
StoreEndpoint.listItems.WireReadTime
StoreEndpoint.listItems.WireWriteTime
StoreEndpoint.listItems.ResponseOut.StatusCode.200.Count
StoreEndpoint.listItems.ResponseOut.StatusFamily.2XX.Count
StoreEndpoint.listItems.ResponseOut.Count
StoreEndpoint.listItems.SuccessRate
```

`Time` covers the complete request as observed by the webserver metrics filter. `ResourceTime` covers the REST endpoint
method execution as observed by the generated interceptor and is emitted only when endpoint metadata is present.
`WireReadTime` covers request body stream reads and is emitted only when the request body is read. `WireWriteTime` covers
response body stream writes and is emitted only when a non-empty response body is written.
`SuccessRate` records `1` for response statuses below `500` and `0` for `500` or higher. Client errors therefore count
as successful from this metric's perspective, matching the service-core convention.

If a request is not matched to generated endpoint metadata, metrics fall back to the `UnknownMethod` scope. The fallback
still emits response count, status code count, status family count, and `SuccessRate`. If detailed timing is enabled, it
also emits `Time` and any observed wire timers under `UnknownMethod`, but it does not emit `ResourceTime`.

The OCI publisher setting `enable-detailed-timing-auto-metrics` defaults to `true`, matching service-core's
`MetricsConfiguration.enableDetailedTimingAutoMetrics`. When set to `false`, OCI Helidon suppresses `Time`,
`ResourceTime`, `WireReadTime`, and `WireWriteTime`; response counters and `SuccessRate` still emit.

### Endpoint scope annotations

OCI Helidon provides two annotations for controlling automatic HTTP metric scopes:

```java
import com.oracle.helidon.oci.metrics.MetricPrefix;
import com.oracle.helidon.oci.metrics.SecondaryMetricPrefix;
```

`@MetricPrefix` applies to endpoint classes and replaces the default `ClassSimpleName.methodName` primary scope:

```java
@RestServer.Endpoint
@MetricPrefix("Inventory")
class StoreEndpoint {
    @Http.GET
    List<Item> listItems() {
        ...
    }
}
```

This emits primary metrics such as:

```text
Inventory.Time
Inventory.ResponseOut.Count
```

Set `appendMethodName=true` when one endpoint class has multiple methods that should keep separate primary scopes:

```java
@RestServer.Endpoint
@MetricPrefix(value = "Inventory", appendMethodName = true)
class StoreEndpoint {
    @Http.GET
    List<Item> listItems() {
        ...
    }

    @Http.POST
    Item createItem(Item item) {
        ...
    }
}
```

This emits `Inventory.listItems.*` and `Inventory.createItem.*`.

`@SecondaryMetricPrefix` applies to endpoint classes or methods and emits the same automatic HTTP metrics under an
additional scope:

```java
@RestServer.Endpoint
@MetricPrefix(value = "Inventory", appendMethodName = true)
@SecondaryMetricPrefix("InventoryAll")
class StoreEndpoint {
    @Http.GET
    @SecondaryMetricPrefix("InventoryReads")
    List<Item> listItems() {
        ...
    }
}
```

For `listItems`, this emits metrics under all three scopes: `Inventory.listItems`, `InventoryAll`, and
`InventoryReads`.

### Heliport migration hints

Heliport migrations should replace service-core automatic HTTP metric annotations with the OCI Helidon annotations:

```java
// Service-core
import com.oracle.pic.commons.service.metrics.jersey.MetricPrefix;
import com.oracle.pic.commons.service.metrics.jersey.SecondaryMetricPrefix;

// OCI Helidon
import com.oracle.helidon.oci.metrics.MetricPrefix;
import com.oracle.helidon.oci.metrics.SecondaryMetricPrefix;
```

The annotation semantics are intentionally aligned:

* `@MetricPrefix("Scope")` becomes `@MetricPrefix("Scope")`.
* `@MetricPrefix(value = "Scope", appendMethodName = true)` keeps the same shape and appends the Java endpoint method
  name to the configured scope.
* `@SecondaryMetricPrefix("Scope")` can remain on endpoint classes or individual endpoint methods.
* If service-core code relied on the default name, omit `@MetricPrefix`; OCI Helidon uses `ClassSimpleName.methodName`.

The code generator also recognizes the service-core annotation type names when they are still present on migrated code,
which helps during incremental migration. Prefer switching imports to `com.oracle.helidon.oci.metrics` in Heliport output
so new services do not need service-core annotation dependencies.

Heliport should also verify that generated REST endpoints compile with `helidon-oci-codegen` on the annotation processor
path. Without that processor, the runtime filter cannot know the Java endpoint method and falls back to `UnknownMethod`.

If the service-core `MetricsConfiguration` has `resourcePackagePrefix` configured, Heliport should migrate it to the
OCI publisher setting `resource-package-prefix`. This preserves the service-core behavior in which only resources whose
fully-qualified class name starts with the configured prefix are tracked by automatic HTTP metrics. If the setting is
absent, OCI Helidon tracks HTTP metrics for every generated endpoint.

Avoid migrating service-core-only metrics behavior that is intentionally out of scope for this integration:

* user-agent metrics
* runtime dimensions
* service-log-only annotations

### Reporter Configuration
The OCI metrics publisher now has a nested `reporter` configuration object. It collects settings which correspond to service-core scheduled reporter controls:
* `metrics-scope-name`: The root name segment used as the prefix for built-in JVM metric names. The default is `service`, so JVM metrics use names such as `service.jvm.memory.heap.used`.
* `includes`: Metric names to include.
* `excludes`: Metric names to exclude.
* `filter-matching-mode`: How to treat `includes` and `excludes` entries: `exact`, `regex`, or `substring`. The default is `exact`; regex matching uses full-pattern semantics.
* `includes-attributes`: Metric attribute names to include when reporting derived values.
* `excludes-attributes`: Metric attribute names to exclude when reporting derived values.

The DropWizard `BaseReporterFactory` also exposes `durationUnit` and `rateUnit` settings, but Helidon Talon does not
expose them. The service-core scheduled metrics reporter emits counters, gauges, and one-minute rates; it does not emit
timer duration values, and its reported rates are events per second.

Reporter include and exclude decisions apply to metric names only. Excludes take precedence over includes, and an empty `includes` list means all non-excluded metrics are eligible for publishing. The programmatic `filter` setting is intentionally not configurable from YAML; it exists only for code which builds the config directly.

#### Heliport implications
When Heliport migrates service-core `scheduled-metrics-reporter` configuration to Helidon Talon metrics configuration, it should map the legacy reporter settings into the single Helidon Talon publisher object at `metrics.publishers.oci`:

| service-core `scheduled-metrics-reporter` setting | Helidon Talon setting |
| --- | --- |
| `metricsScopeName` | `metrics-scope-name` |
| `includes` | `includes` |
| `excludes` | `excludes` |
| legacy regex/substring matching flags | `filter-matching-mode` |
| `includesAttributes` | `includes-attributes` |
| `excludesAttributes` | `excludes-attributes` |
| `frequency` | `sample-interval` |

Helidon Talon samples metrics on the configured interval and does not report metric mutations to the OCI metrics layer as they happen. Heliport should map the legacy scheduled reporter interval/frequency setting to `sample-interval`. Counters use the configured metric name and emit interval deltas. Timers use the configured metric name and emit a single one-minute EWMA rate in events per second. Distribution summaries use the configured metric name and emit a single interval mean. This preserves service-core-style `SuccessRate` because it records `0.0` or `1.0`, so the interval mean is the success rate.

Heliport should not migrate legacy `durationUnit`; there is no Helidon Talon equivalent because no emitted timer metric
contains timer duration values. Attribute filters still apply to sampled values: counters, gauges, and functional counters
use `value`; timer one-minute EWMA rates use `m1_rate`; distribution summary interval means use `mean`.

Roughly speaking, a DropWizard reporter corresponds to a Helidon metrics publisher. The legacy OCI metrics configuration can specify multiple reporters, but Helidon Talon supports only one set of reporter-style controls in the `oci` metrics publisher. This is intentional: both the legacy DropWizard-based approach and Helidon Talon periodically report metrics through configured reporter-style controls. If a legacy config contains multiple scheduled metrics reporters, Heliport should choose or consolidate to one Helidon Talon publisher configuration and flag any ambiguous cases for review. If future requirements emerge to support multiple DW reporters, we can look at adding additional OCI-related metrics publishers to match.

Once Heliport identifies which `reporter` to migrate, it should map the `useRegExFilters` and `useSubstringMatching` booleans to the corresponding `FilterMatchingMode` enum value:

| Original `useRegexFilters` | Original `useSubstringMatching` | Migrated `FilterMatchingMode` |
| --- | --- | --- |
| `false` | `false` | `EXACT` |
| `true` | `false` | `REGEX` |
| `false` | `true` | `SUBSTRING` |
| `true` | `true` | No valid value; flag for manual review |

## Metrics library choice
As of OCI Helidon 2.0, metrics integration uses the `metrics-lib` library from the OCI telemetry team.

There are multiple libraries available for working with OCI metrics, among them:
* [`com.oracle.pic.telemetry.commons:metrics-lib`](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-lib/browse)
* [`com.oracle.pic.commons:metrics`](https://bitbucket.oci.oraclecorp.com/projects/COMMONS/repos/metrics/browse)

The `metrics-lib` `README.md` states, among other things:
> NOTE: the PIC commons metrics library is more complex and more powerful in some ways but is much more prone to memory leaks if not carefully used. You should consider using this metrics-lib library directly where possible.

For simplicity of our code and for reliability, this release of the T2 metrics integration follows the suggestion above and uses the `metrics-lib` library. This allows us to delegate to the T2 library all the responsibility of buffering metrics data and transmitting it (retrying if necessary) to the backend. That library _does not_ expose a way to control the frequency with which it sends data; it transmits (if values have been reported to it) at least each minute, more frequently if its internal buffers fill.

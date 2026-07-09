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
  * `<scope>.Request.Client.<clientAgg>.Count`
  * `<scope>.Request.Client.<clientAgg>.<n>XX.Count`

  These metrics intentionally do not use automatic `method`, `route`, or `status.family` tags. The endpoint identity is
  encoded in the metric scope so migrated services can continue to use service-core-style dashboards and alerts.

## Overall design
### Overview
OCI Helidon metrics integration provides a new implementation of the Helidon neutral metrics API, with these key aspects:
* Delegates to some other underlying provider (Micrometer is the only one as of this writing) to store data for each meter.
* Periodically samples registered meters and records changed values through the OCI metrics library.
* Implements a new Helidon metrics publisher type, `oci`, which captures the configurable aspects of the connection to the backend.

### Handling sampled meters
All supported Helidon meter types are sampled on the configured interval. Metric mutation methods update only the local
meter/delegate state; they do not report directly to the OCI metrics layer.

Helidon `Counter.count()` reports the local cumulative delegate count. OCI publication for counters emits only the
delta since the previous sample as an interval event count. Timers publish service-core-style duration observations in
milliseconds, using the configured timer metric name. Distribution summaries publish observations using the configured
summary metric name. Gauges publish their current value on each sample. Functional counters publish interval event
counts based on the delta since the previous sample.

Timers and distribution summaries use bounded per-second accumulators. A bucket retains exact raw observations up to
the configured cap. Once the cap is exceeded, the bucket compacts into min, max, and weighted middle mean observations,
preserving count, sum, mean, min, and max while bounding retained objects. Normal sampling drains only closed seconds;
shutdown sampling drains all buckets, including the current open second, before OCI `metrics-lib` shutdown.

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

When `auto-http.user-agent-metrics-enabled` is `true`, which is the default, OCI Helidon also parses the request
`User-Agent` header using service-core-compatible known client parsers and emits client aggregation counters:

```text
StoreEndpoint.listItems.Request.Client.JavaSDK.Count
StoreEndpoint.listItems.Request.Client.JavaSDK.2XX.Count
```

Detailed user-agent series are bounded by `auto-http.max-user-agent-series` (default `1000`). The stable client-family
series do not count against this limit. Once the limit is reached, new detailed identities contribute to
`Request.Client.OTHER.*` counters, and `OciMetrics.AutoHttp.UserAgentCardinalityOverflows.Count` reports the number of
scope/request updates which encountered the limit.

Browser user agents such as Safari, Chrome, and Firefox are grouped as
`BrowserClient.UNKNOWN.UNKNOWN.UNKNOWN.UNKNOWN.UNKNOWN`, matching service-core's generic browser parser.

If a request is not matched to generated endpoint metadata, metrics fall back to the `UnknownMethod` scope. The fallback
still emits response count, status code count, status family count, and `SuccessRate`. If detailed timing is enabled, it
also emits `Time` and any observed wire timers under `UnknownMethod`, but it does not emit `ResourceTime`.

The OCI publisher setting `enable-detailed-timing-auto-metrics` defaults to `true`, matching service-core's
`MetricsConfiguration.enableDetailedTimingAutoMetrics`. When set to `false`, OCI Helidon suppresses `Time`,
`ResourceTime`, `WireReadTime`, and `WireWriteTime`; response counters and `SuccessRate` still emit.

The OCI publisher setting `auto-http.enabled` defaults to `true`. When set to `false`, OCI Helidon does not install the
automatic HTTP metrics filter, so no automatic HTTP metrics are gathered or emitted by this mechanism.

The optional `auto-http.runtime-dimension` section supports one service-core-compatible runtime dimension. OCI Helidon
reads the configured `property-name` from the Helidon request `Context`; if no value is present, it uses
`default-dimension` when configured. The resolved value is emitted as an OCI metric dimension named by
`dimension-name`. Count-style automatic HTTP metrics also insert the value into the dotted metric name after the scope,
matching service-core. The value is inserted as-is, without sanitization, so values containing punctuation create
corresponding metric-name segments and each distinct value can create a distinct metric series:

```yaml
metrics:
  publishers:
    - type: oci
      auto-http:
        runtime-dimension:
          property-name: lab-environment
          dimension-name: lab
          default-dimension: PINTLAB
```

Application code supplies request-specific values with the same property name:

```java
request.context().register("lab-environment", "PINTLAB");
```

Example output:

```text
StoreEndpoint.listItems.Time{"lab": "PINTLAB"}
StoreEndpoint.listItems.PINTLAB.ResponseOut.Count{"lab": "PINTLAB"}
StoreEndpoint.listItems.PINTLAB.Request.Client.JavaSDK.Count{"lab": "PINTLAB"}
```

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

Heliport should migrate service-core automatic HTTP configuration into the OCI publisher and nested `auto-http` section:

| service-core `MetricsConfiguration` setting | Helidon Talon setting |
| --- | --- |
| `enableAutoMetrics` | `auto-http.enabled` |
| `enableDetailedTimingAutoMetrics` | `enable-detailed-timing-auto-metrics` |
| `enableUserAgentMetrics` | `auto-http.user-agent-metrics-enabled` |
| `resourcePackagePrefix` | `resource-package-prefix` |
| `runtimeDimensionConfig.propertyName` | `auto-http.runtime-dimension.property-name` |
| `runtimeDimensionConfig.dimensionName` | `auto-http.runtime-dimension.dimension-name` |
| `runtimeDimensionConfig.defaultDimension` | `auto-http.runtime-dimension.default-dimension` |

The service-core runtime dimension source was a Jersey `ContainerRequest` property. The Helidon equivalent is a
`RoutingRequest.context()` entry using the configured `property-name`; migrated filters or endpoint code should register
the value in the request context before the response is sent.

If service-core code sets the skip-response-status property on a request, Heliport should migrate that request property
write to the Helidon request context using `OciHttpEndpointMetricsContext.SKIP_RESPONSE_STATUS_METRICS`. This suppresses
status-code and status-family automatic HTTP metrics, including status-family user-agent counters, but still allows
`ResponseOut.Count`, `SuccessRate`, and client aggregate count metrics.

Avoid migrating service-core-only metrics behavior that is intentionally out of scope for this integration:

* service-log-only annotations

### Reporter Configuration
The OCI metrics publisher keeps publisher behavior settings at the `oci` publisher level:
* `metrics-scope-name`: The root name segment used as the prefix for built-in JVM metric names. The default is `service`, so JVM metrics use names such as `service.jvm.memory.heap.used`.
* `includes`: Metric names to include.
* `excludes`: Metric names to exclude.
* `filter-matching-mode`: How to treat `includes` and `excludes` entries: `exact`, `regex`, or `substring`. The default is `exact`; regex matching uses full-pattern semantics.
* `includes-attributes`: Metric attribute names to include when reporting derived values.
* `excludes-attributes`: Metric attribute names to exclude when reporting derived values.
* `sample-interval`: Interval between scheduled metric samples.
* `accumulators`: Bounded per-second accumulator settings for timers and distribution summaries.

The DropWizard `BaseReporterFactory` also exposes `durationUnit` and `rateUnit` settings, but Helidon Talon does not
expose them. Timers are emitted as exact duration observations in milliseconds using the configured metric name, matching
the service-core `MetricsScope` timer semantics used by automatic HTTP metrics and other scope timers. They are not
emitted as DropWizard scheduled-reporter one-minute rates.

Distribution summaries also use the bounded observation accumulator. For `SuccessRate`, automatic HTTP metrics record
`1.0` for non-5xx responses and `0.0` for 5xx responses; below the raw cap those observations are emitted exactly, and
above the cap compaction preserves the bucket count and mean.

Reporter include and exclude decisions apply to metric names only. Excludes take precedence over includes, and an empty `includes` list means all non-excluded metrics are eligible for publishing. The programmatic `filter` setting is intentionally not configurable from YAML; it exists only for code which builds the config directly.

Reporter construction settings are nested under the publisher `reporter` object. `reporter.overlay` is the default
variant and preserves the TelemetryReporterBuilder-based overlay behavior. `reporter.substrate` selects the
DianogaReporter-based substrate path. Common settings for both variants include `project`, `fleet`, `client`,
`endpoint`, `hostname` or `host-name`, `availability-domain`, `fault-domain`, and `region`. Overlay-only configured
settings are `request-headers`, `use-metadata-service`, and `override-metric-keys`; these should not be migrated into
`reporter.substrate`.

#### Heliport implications
When Heliport migrates service-core `scheduled-metrics-reporter` configuration to Helidon Talon metrics configuration,
it should map scheduled sampling and filtering settings into the single Helidon Talon publisher object at
`metrics.publishers.oci`:

| service-core `scheduled-metrics-reporter` setting | Helidon Talon setting |
| --- | --- |
| `metricsScopeName` | `metrics-scope-name` |
| `includes` | `includes` |
| `excludes` | `excludes` |
| legacy regex/substring matching flags | `filter-matching-mode` |
| `includesAttributes` | `includes-attributes` |
| `excludesAttributes` | `excludes-attributes` |
| `frequency` | `sample-interval` |

Heliport normally should not synthesize accumulator settings from service-core configuration. The defaults are intended
for migrated services:

```yaml
metrics:
  publishers:
    - type: oci
      sample-interval: PT1S
      accumulators:
        max-pending-seconds: 10
        max-raw-timer-samples-per-second: 1024
        max-raw-summary-samples-per-second: 1024
        pressure-log-interval: PT30S
```

If migration analysis identifies an unusually high-volume service or a service that requires exact raw timer or summary
observations at higher per-second rates, Heliport can surface these settings for owner review instead of guessing.

It should map backend/reporter construction settings into the selected nested reporter variant. For normal overlay
migrations, use `reporter.overlay`:

| service-core / legacy OCI reporter setting | Helidon Talon overlay setting |
| --- | --- |
| `project` | `reporter.overlay.project` |
| `fleet` | `reporter.overlay.fleet` |
| `client` | `reporter.overlay.client` |
| `endpoint` | `reporter.overlay.endpoint` |
| `hostname` or `hostName` | `reporter.overlay.hostname` |
| `host-name` | `reporter.overlay.host-name` |
| `availabilityDomain` | `reporter.overlay.availability-domain` |
| `faultDomain` | `reporter.overlay.fault-domain` |
| `region` | `reporter.overlay.region` |
| `requestHeaders` | `reporter.overlay.request-headers` |
| `useMetadataService` | `reporter.overlay.use-metadata-service` |
| `overrideMetricKeys` | `reporter.overlay.override-metric-keys` |

If the source service is known to target substrate, Heliport should use `reporter.substrate` instead and map only the
common reporter settings:

| service-core / legacy OCI reporter setting | Helidon Talon substrate setting |
| --- | --- |
| `project` | `reporter.substrate.project` |
| `fleet` | `reporter.substrate.fleet` |
| `client` | `reporter.substrate.client` |
| `endpoint` | `reporter.substrate.endpoint` |
| `hostname` or `hostName` | `reporter.substrate.hostname` |
| `host-name` | `reporter.substrate.host-name` |
| `availabilityDomain` | `reporter.substrate.availability-domain` |
| `faultDomain` | `reporter.substrate.fault-domain` |
| `region` | `reporter.substrate.region` |

Heliport should not migrate overlay-only settings such as request headers, metadata-service use, or metric-key override
into `reporter.substrate`; if those settings are present while the migration target is substrate, report them for manual
review.

Helidon Talon samples metrics on the configured interval and does not report metric mutations to the OCI metrics layer
as they happen. Heliport should map the legacy scheduled reporter `frequency` setting to `sample-interval` when a
service has an explicit scheduled reporter frequency; otherwise the OCI Helidon default is `PT1S`. `Counter.count()`
remains the local cumulative Helidon count, but publication uses the configured metric name and emits interval event
counts. Timers use the configured metric name and emit service-core-style duration observations in milliseconds.
Distribution summaries use the configured metric name and emit observations. This preserves service-core-style
`SuccessRate` input values below the raw cap because it records `0.0` or `1.0`; above the cap, compaction preserves
bucket count and mean.

Timer and distribution summary observations are retained in bounded per-second buckets until sampling. For high-volume
meters, exact raw observations are bounded by `max-raw-timer-samples-per-second` and
`max-raw-summary-samples-per-second`; additional observations in the same second compact into min, max, and weighted
middle mean. If the sampler falls behind more than `max-pending-seconds`, oldest buckets are dropped and pressure
metrics/logs are emitted. Use `sample-interval` for sampler frequency and `accumulators` for retained-object and
exactness tuning.

Heliport should not migrate legacy `durationUnit` or `rateUnit`; there is no Helidon Talon equivalent. Attribute filters
still apply to sampled values: counters, gauges, functional counters, timer duration observations, and distribution
summary observations use `value`.

Roughly speaking, a DropWizard reporter corresponds to a Helidon metrics publisher. The legacy OCI metrics configuration can specify multiple reporters, but Helidon Talon supports only one set of reporter-style controls in the `oci` metrics publisher. This is intentional: both the legacy DropWizard-based approach and Helidon Talon periodically report metrics through configured reporter-style controls. If a legacy config contains multiple scheduled metrics reporters, Heliport should choose or consolidate to one Helidon Talon publisher configuration and flag any ambiguous cases for review. If future requirements emerge to support multiple DW reporters, we can look at adding additional OCI-related metrics publishers to match.

Once Heliport identifies which `reporter` to migrate, it should map the `useRegExFilters` and `useSubstringMatching` booleans to the corresponding `FilterMatchingMode` enum value:

| Original `useRegexFilters` | Original `useSubstringMatching` | Migrated `FilterMatchingMode` |
| --- | --- | --- |
| `false` | `false` | `EXACT` |
| `true` | `false` | `REGEX` |
| `false` | `true` | `SUBSTRING` |
| `true` | `true` | No valid value; flag for manual review |

## Metrics library choice
As of OCI Helidon 2.0, metrics integration uses the `metrics-lib` runtime API from the OCI telemetry team.

There are multiple libraries available for working with OCI metrics, among them:
* [`com.oracle.pic.telemetry.commons:metrics-lib`](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-lib/browse)
* [`com.oracle.pic.telemetry.commons:metrics-reporter`](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-reporter/browse)
* [`com.oracle.pic.commons:metrics`](https://bitbucket.oci.oraclecorp.com/projects/COMMONS/repos/metrics/browse)

The `metrics-lib` `README.md` states, among other things:
> NOTE: the PIC commons metrics library is more complex and more powerful in some ways but is much more prone to memory leaks if not carefully used. You should consider using this metrics-lib library directly where possible.

For simplicity of our code and for reliability, this integration follows that suggestion for the application-facing
metrics runtime. OCI Helidon initializes `com.oracle.pic.telemetry.commons.metrics.Metrics` with a configured
`MetricReporter`, so existing service code that emits through metrics-lib can continue to work.

Reporter construction is separate from the metrics-lib runtime choice. The overlay path builds the reporter with
`TelemetryReporterBuilder`. The substrate path builds a `DianogaReporter` using a `MetricTimeSeriesClient`. Both produce a
`MetricReporter` for `Metrics.init`.

Helidon meter sampling frequency is controlled by the OCI publisher `sample-interval`. Any buffering, retrying, or
transmission cadence inside the selected metrics-lib reporter is separate from that Helidon sampling interval and depends
on the selected reporter implementation.

# Metrics

---

## Overview

The Metrics integration provides several features:
* Uses `metrics.publishers` configuration to prepare the OCI `metrics-lib` runtime system.
* Automatically publishes Helidon application metrics to the OCI metrics backend, whether declared and updated imperatively or declaratively through annotations. 
* Allows services to continue to use `com.oracle.pic.telemetry.commons.metrics.Metrics` directly to report metrics updates.
 
When `helidon-oci-metrics` is on the classpath and an OCI metrics publisher is configured, the
integration can:

* periodically sample and publish `Counter`, `Timer`, `DistributionSummary`, gauge, and functional-counter values to OCI metrics
* register automatic HTTP request counters and timers and update them for each incoming request
* register JVM gauges for measurements such as memory usage, thread state, class loading, file descriptors, and garbage collection
* create the required OCI client for the configured reporter and make overlay `Monitoring` clients available through the
  Helidon service registry

Configuration is provided through a Helidon metrics publisher of type `oci`, as shown in
[Configure OCI metrics publishing](#configure-oci-metrics-publishing).

---

## Maven Coordinates

Add the OCI metrics integration dependency to your service:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.metrics</groupId>
    <artifactId>helidon-oci-metrics</artifactId>
</dependency>
```

The `@MetricPrefix` and `@SecondaryMetricPrefix` annotations allow API endpoint methods to modify the default names used for automatic HTTP metrics. To use these annotations, add `helidon-oci-codegen` to the annotation processor path.

Services normally also configure OCI SDK authentication using the shared Helidon Talon SDK configuration under
`helidon.oci.*`.

---

## Usage

### Configure OCI metrics publishing

At minimum, configure an OCI metrics publisher with a reporter project and fleet:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: my-service
          fleet: my-fleet
          region: us-ashburn-1
```

If `region` is omitted, the metrics integration attempts to use a `com.oracle.pic.commons.util.Region` from the Helidon
service registry. The `helidon-oci-envconfig` module can supply that region from the OCI environment.

Configure OCI SDK authentication separately. For example, to use instance principal authentication:

```yaml
helidon:
  oci:
    authentication-method: instance-principal
```

For local testing through an IMDS tunnel:

```yaml
helidon:
  oci:
    authentication-method: instance-principal
    imds-base-uri: http://localhost:8000/opc/v2/
    imds-timeout: PT3S
    imds-detect-retries: 1
```
### Use metrics APIs
Service code can use several APIs as described below, within the same service if helpful.  

#### Use OCI `metrics-lib` `Metrics` API
The metrics integration automatically prepares the OCI metrics runtime based on the configuration, invoking `Metrics.init` during start-up and `Metrics.shutdown` as the service stops.

Once the service has started, service code can invoke the `com.oracle.pic.telemetry.commons.metrics.Metrics` methods such as `emit`, `sensor`, or `record` as normal. Service code should not normally invoke `Metrics.init` or `Metrics.shutdown`; the Helidon Talon metrics integration library does so. 

#### Use Helidon Metrics imperative API

Services can also use the Helidon metrics API to register and update application meters. This integration
periodically samples those Helidon meters and publishes changed values to OCI metrics without requiring application code
to call the OCI Monitoring client or a T2-specific API directly.

The following example shows imperative use of the Helidon metrics API in a hypothetical utility class `WorkService` that counts and times the invocations of the work. (Note that each timer includes a counter. Production code would rarely measure the same code using both; this is just an example to show the imperative API for both timers and counters.)

```java
@Service.Singleton
class WorkService {
    private final Counter counter;
    private final Timer timer;

    @Service.Inject
    WorkService(MeterRegistry registry) {
        this.counter = registry.getOrCreate(Counter.builder("work.requests"));
        this.timer = registry.getOrCreate(Timer.builder("work.duration"));
    }

    void doWork() {
        Timer.Sample sample = Timer.start();
        try {
            counter.increment();
            // perform work
        } finally {
            sample.stop(timer);
        }
    }
}
```

#### Use Helidon metric annotations

Helidon metric annotations also work because Helidon's handling of the metrics annotations uses the Helidon metrics API.

```java
@Http.GET
@Http.Path("/hello/{name}")
@Metrics.Timed(value = "personalized-greeting", absoluteName = true)
String personalizedGreeting(@Http.PathParam("name") String name) {
    return "Hello, " + name + "!";
}
```

### Automatic HTTP request metrics

For annotated endpoint API methods, the integration registers automatic HTTP metrics using the `service-core`
naming/scope convention. The default scope is `ClassSimpleName.methodName`. 

The integration emits:

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

`Time`, `ResourceTime`, `WireReadTime`, and `WireWriteTime` are detailed timing metrics controlled by
`enable-detailed-timing-auto-metrics`, which defaults to `true`. `WireReadTime` is emitted only when the request body
stream is read, and `WireWriteTime` is emitted only when a non-empty response body is written.

`SuccessRate` records `1` for response statuses below `500` and `0` for statuses `500` or higher.

When `auto-http.user-agent-metrics-enabled` is `true`, which is the default, the integration parses the request
`User-Agent` header and emits service-core-style client aggregation counters. Browser user agents such as Safari,
Chrome, and Firefox are grouped as `BrowserClient.UNKNOWN.UNKNOWN.UNKNOWN.UNKNOWN.UNKNOWN`.

The optional `auto-http.runtime-dimension` section reads a value from the Helidon request `Context`. The resolved value
is emitted as an OCI dimension, and count-style automatic HTTP metric names insert the value after the scope, matching
service-core's runtime dimension naming convention. The value is inserted as-is, without sanitization, so choose values
that are appropriate for metric names and backend cardinality.

`@MetricPrefix` can be added to an endpoint class to replace the default scope. Set `appendMethodName=true` to append the
Java method name to the configured prefix. `@SecondaryMetricPrefix` can be added to an endpoint class or method to emit
the same automatic HTTP metrics under an additional scope.

If a request does not carry generated endpoint metadata, the automatic HTTP metrics use `UnknownMethod`. `ResourceTime`
is emitted only when generated endpoint metadata captures resource method timing.

### Automatic JVM gauges

The integration registers built-in JVM gauges for:

* memory usage
* thread state and thread counts
* class loading
* buffer pools
* file descriptors
* garbage collection
* JVM uptime

JVM meter names use `service` as the default root segment, for example
`service.jvm.memory.heap.used` and `service.jvm.threads.count`. Configure
`metrics-scope-name` to use a different root segment. To suppress JVM meters from publishing, use the publisher include
and exclude filters.

### Override the metrics endpoint

By default, the overlay reporter computes the ingestion endpoint from the OCI Monitoring endpoint by replacing
`telemetry.` with `telemetry-ingestion.`. You can override the reporter endpoint directly:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: my-service
          fleet: my-fleet
          endpoint: https://telemetry-ingestion.example.com
```

Applications or libraries can also provide a higher-weight `@OciMetrics Function<Monitoring, URI>` service to choose a
different overlay metrics endpoint.

---

## Configuration

### Publisher configuration

The OCI metrics publisher is configured as an entry under `metrics.publishers` with `type` `oci`.
Reporter construction settings are nested under the `reporter` key. The `overlay` reporter uses
`TelemetryReporterBuilder` and is the default behavior. The `substrate` reporter uses `DianogaReporter`.
When reporter `availability-domain` or `fault-domain` is omitted, the OCI metrics publisher uses
`oci.env.availability-domain` and `oci.env.fault-domain` when those values are available.

The publisher samples Helidon meters on `sample-interval`; metric mutation methods do not report directly to OCI.
Helidon `Counter.count()` still reports the local cumulative delegate count, but OCI publication emits only the
delta since the previous sample as an interval event count using the configured metric name. Timers are emitted as
MetricsScope-style duration observations in milliseconds using the configured metric name, not as one-minute rates.
Distribution summaries are emitted as observations using the configured metric name. Automatic HTTP request metrics use
the same Helidon meter sampling path, subject to the additional `auto-http.enabled` switch. Attribute filters use
`value` for counters, timers, summaries, gauges, and functional counters.

Timers and distribution summaries retain per-second buckets until sampled. Buckets retain exact raw observations up to
the configured accumulator cap; above the cap, the bucket compacts to min, max, and weighted middle mean observations
that preserve count, sum, mean, min, and max. During normal sampling the current second remains open. During shutdown,
OCI Helidon drains all buckets, including the current second, then shuts down OCI `metrics-lib` so it can flush queued
values.

| Key | Default value | Description                                                                                                                                                      |
|-----|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type` | | Set to `oci` to use this publisher.                                                                                                                              |
| `enabled` | `true` | Enables or disables OCI metrics publishing.                                                                                                                      |
| `reporter` | `overlay` | Nested reporter configuration. Use `reporter.overlay` for overlay or `reporter.substrate` for substrate.                                                          |
| `default-dimensions` | `{}` | Default dimensions for OCI `com.oracle.pic.telemetry.commons.metrics.Metrics.init`. Added to emitted metrics data that does not have any dimensions already set. |
| `sample-interval` | `PT1S` | Interval between scheduled metric samples.                                                                                                                       |
| `accumulators` | | Nested configuration for bounded timer and distribution summary accumulators.                                                                                     |
| `auto-http` | | Nested configuration for automatic HTTP metrics.                                                                                                                  |
| `enable-detailed-timing-auto-metrics` | `true` | Enables automatic HTTP `Time`, `ResourceTime`, `WireReadTime`, and `WireWriteTime` timers.                                                                       |
| `resource-package-prefix` | | Optional package-name prefix for generated REST resources included in automatic HTTP metrics.                                                                    |
| `metrics-scope-name` | `service` | Root name segment used as the prefix for built-in JVM metric names.                                                                                              |
| `includes` | `[]` | Metric names to include. An empty list includes all non-excluded metrics.                                                                                        |
| `excludes` | `[]` | Metric names to exclude. Excludes take precedence over includes.                                                                                                 |
| `filter-matching-mode` | `exact` | How to treat `includes` and `excludes` entries: `exact`, `regex`, or `substring`. Regex matching uses full-pattern semantics.                                    |
| `includes-attributes` | `value` | Metric attribute names to include when reporting derived values.                                                                                                  |
| `excludes-attributes` | `[]` | Metric attribute names to exclude when reporting derived values.                                                                                                 |

Accumulator settings:

| Key | Default value | Description |
|-----|---------------|-------------|
| `accumulators.max-pending-seconds` | `10` | Maximum number of pending per-second buckets retained by one meter. If the sampler falls behind, oldest buckets beyond this limit are dropped. |
| `accumulators.max-raw-timer-samples-per-second` | `1024` | Maximum exact timer samples retained per second before compacting the bucket. |
| `accumulators.max-raw-summary-samples-per-second` | `1024` | Maximum exact distribution summary samples retained per second before compacting the bucket. |
| `accumulators.pressure-log-interval` | `PT30S` | Minimum time between accumulator pressure log messages for compaction or drops. |

Automatic HTTP metrics settings:

| Key | Default value | Description |
|-----|---------------|-------------|
| `auto-http.enabled` | `true` | Enables gathering automatic HTTP metrics. Set to `false` to avoid installing the automatic HTTP metrics filter. |
| `auto-http.user-agent-metrics-enabled` | `true` | Emits service-core-style `Request.Client.*` counters based on the request `User-Agent` header. |
| `auto-http.max-user-agent-series` | `1000` | Maximum detailed user-agent counter series retained by one automatic HTTP metrics filter. New detailed identities beyond the limit are aggregated into stable `Request.Client.OTHER.*` counters. |
| `auto-http.runtime-dimension.property-name` | required when `runtime-dimension` is configured | Helidon request `Context` property to read. |
| `auto-http.runtime-dimension.dimension-name` | required when `runtime-dimension` is configured | OCI metric dimension name to emit. |
| `auto-http.runtime-dimension.default-dimension` | absent | Optional fallback value when the request context has no property value. |

Example:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: my-service
          fleet: my-fleet
      metrics-scope-name: my-service
      sample-interval: PT1S
      accumulators:
        max-pending-seconds: 10
        max-raw-timer-samples-per-second: 1024
        max-raw-summary-samples-per-second: 1024
        pressure-log-interval: PT30S
      auto-http:
        enabled: true
        user-agent-metrics-enabled: true
        max-user-agent-series: 1000
      excludes:
        - "my-service\\.jvm\\..*"
      filter-matching-mode: regex
```

### Reporter configuration

Use `reporter.overlay` for overlay environments. Use `reporter.substrate` for substrate environments.

Common reporter settings apply to both reporter variants:

| Overlay key | Substrate key | Default value | Description |
|-------------|---------------|---------------|-------------|
| `reporter.overlay.project` | `reporter.substrate.project` | | OCI metrics project. Required for configured reporter publishing. |
| `reporter.overlay.fleet` | `reporter.substrate.fleet` | | OCI metrics fleet. Required for configured reporter publishing. |
| `reporter.overlay.region` | `reporter.substrate.region` | | Optional public region name. If set, this takes precedence over a registry-provided PIC `Region`. |
| `reporter.overlay.endpoint` | `reporter.substrate.endpoint` | | Optional metrics backend endpoint override. |
| `reporter.overlay.hostname` | `reporter.substrate.hostname` | | Optional hostname override for emitted dimensions. |
| `reporter.overlay.host-name` | `reporter.substrate.host-name` | | Alias for `hostname`; configure only one of the two keys. |
| `reporter.overlay.availability-domain` | `reporter.substrate.availability-domain` | `oci.env.availability-domain` | Optional availability-domain override. |
| `reporter.overlay.fault-domain` | `reporter.substrate.fault-domain` | `oci.env.fault-domain` | Optional fault-domain override. |

Aliases such as `hostname` and `host-name` are provided for user convenience, either to align with native OCI parameter
names or with similar settings in other Helidon Talon modules. Specify at most one name for an aliased setting, not both.

Overlay-only reporter settings:

| Key | Default value | Description |
|-----|---------------|-------------|
| `reporter.overlay.request-headers` | `{}` | Additional headers to send with OCI monitoring requests. |
| `reporter.overlay.use-metadata-service` | | Optional flag passed to the OCI telemetry reporter builder. |
| `reporter.overlay.override-metric-keys` | | Optional flag passed to the OCI telemetry reporter builder. |

There are no substrate-only configured settings. Applications can supply a prebuilt substrate
`MetricTimeSeriesClient` programmatically using `SubstrateMetricReporterConfig.Builder.metricTimeSeriesClient(...)`.

Overlay example:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: my-service
          fleet: my-fleet
          region: us-ashburn-1
          hostname: my-host
          request-headers:
            opc-example: value
```

Substrate example:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        substrate:
          project: my-service
          fleet: my-fleet
          region: us-ashburn-1
          hostname: my-host
```

### Reporter client configuration

Use the nested reporter `client` setting to tune the OCI SDK client used by the reporter. The keys below show the
overlay path; use the same `client` subtree under `reporter.substrate` for substrate.

Applications can also supply a prebuilt `ClientConfiguration` programmatically using the overlay or substrate reporter
config builder `client(...)` method. For substrate, that configuration is used only when the integration creates the
`MetricTimeSeriesClient`; it is ignored if `metricTimeSeriesClient(...)` supplies a prebuilt client.

| Key | Default value | Description |
|-----|---------------|-------------|
| `reporter.overlay.client.connection-timeout` | | Optional OCI SDK connection timeout. |
| `reporter.overlay.client.read-timeout` | | Optional OCI SDK read timeout. |
| `reporter.overlay.client.max-async-threads` | | Optional maximum async thread count. |
| `reporter.overlay.client.disable-data-buffering-on-upload` | | Optional upload buffering flag. |
| `reporter.overlay.client.retry` | | Optional OCI SDK retry configuration. |
| `reporter.overlay.client.circuit-breaker` | | Optional OCI SDK circuit-breaker configuration. |

Example:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: my-service
          fleet: my-fleet
          client:
            connection-timeout: PT7S
            read-timeout: PT11S
            max-async-threads: 13
```

### Retry configuration

Retry configuration mirrors the OCI SDK retry types. The keys below show the overlay path; use the same `retry` subtree
under `reporter.substrate.client` for substrate.

| Key | Default value | Description |
|-----|---------------|-------------|
| `reporter.overlay.client.retry.termination-strategy.type` | | `max-attempts` or `max-time`. |
| `reporter.overlay.client.retry.termination-strategy.max-attempts` | | Maximum attempts for `max-attempts`. |
| `reporter.overlay.client.retry.termination-strategy.max-time` | | Maximum duration for `max-time`. |
| `reporter.overlay.client.retry.delay-strategy.type` | | `fixed`, `exponential`, or `exponential-with-jitter`. |
| `reporter.overlay.client.retry.delay-strategy.delay` | | Fixed delay for `fixed`. |
| `reporter.overlay.client.retry.delay-strategy.max-delay` | | Maximum delay for exponential strategies. |
| `reporter.overlay.client.retry.retry-condition.type` | | `default` or `retry-on-open-circuit-breaker`. |
| `reporter.overlay.client.retry.retry-options.mark-read-limit` | | Optional mark-read limit. |

Example:

```yaml
metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: my-service
          fleet: my-fleet
          client:
            retry:
              termination-strategy:
                type: max-attempts
                max-attempts: 5
              delay-strategy:
                type: exponential
                max-delay: PT2S
              retry-condition:
                type: retry-on-open-circuit-breaker
              retry-options:
                mark-read-limit: 4096
```

### Circuit breaker configuration

The keys below show the overlay path; use the same `circuit-breaker` subtree under `reporter.substrate.client` for
substrate.

| Key | Default value | Description |
|-----|---------------|-------------|
| `reporter.overlay.client.circuit-breaker.failure-rate-threshold` | | Failure-rate threshold. |
| `reporter.overlay.client.circuit-breaker.slow-call-rate-threshold` | | Slow-call-rate threshold. |
| `reporter.overlay.client.circuit-breaker.wait-duration-in-open-state` | | Time to remain open before half-open. |
| `reporter.overlay.client.circuit-breaker.permitted-number-of-calls-in-half-open-state` | | Permitted half-open calls. |
| `reporter.overlay.client.circuit-breaker.minimum-number-of-calls` | | Minimum calls before calculating state. |
| `reporter.overlay.client.circuit-breaker.sliding-window-size` | | Sliding window size. |
| `reporter.overlay.client.circuit-breaker.slow-call-duration-threshold` | | Duration threshold for slow calls. |
| `reporter.overlay.client.circuit-breaker.writable-stack-trace-enabled` | | Optional writable stack trace flag. |

---

## References

* [Metrics example](../../examples/metrics/)
* [Helidon metrics API](https://helidon.io/docs/latest/se/metrics/metrics)

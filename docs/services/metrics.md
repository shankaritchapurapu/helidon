# Metrics

---

## Overview

The Metrics integration provides several features:
* Uses `metrics.publishers` configuration to prepare the OCI `metrics-lib` runtime system.
* Automatically publishes Helidon application metrics to the OCI metrics backend, whether declared and updated imperatively or declaratively through annotations. 
* Allows services to continue to use `com.oracle.pic.telemetry.commons.metrics.Metrics` directly to report metrics updates.
 
When `helidon-oci-metrics` is on the classpath and an OCI metrics publisher is configured, the
integration can:

* publish `Counter`, `Timer`, and `DistributionSummary` updates directly to OCI metrics
* periodically sample and publish gauges and functional counters and report them to OCI metrics
* register automatic HTTP request counters and timers and update them for each incoming request
* register JVM gauges for measurements such as memory usage, thread state, class loading, file descriptors, and garbage collection
* create the required OCI `Monitoring` client and make it available through the Helidon service registry

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

Services normally also configure OCI SDK authentication using the shared Helidon OCI SDK configuration under
`helidon.oci.*`.

---

## Usage

### Configure OCI metrics publishing

At minimum, configure an OCI metrics publisher with a project and fleet:

```yaml
metrics:
  publishers:
    - type: oci
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

While the service is running, the service code can invoke the `com.oracle.pic.telemetry.commons.metrics.Metrics` methods such as `emit`, `sensor`, or `record` as normal. Service code should not normally invoke `Metrics.init` or `Metrics.shutdown`; the Helidon OCI metrics integration library does so. 

#### Use Helidon Metrics imperative API

Services can also use the Helidon metrics API to register and update application meters. This integration
automatically publishes those Helidon meters to OCI metrics without requiring application code to call the OCI Monitoring
client or a T2-specific API directly.

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

The integration registers and updates:

* `http.requests.count` (counter)
* `http.request.duration` (timer)

Both meters are tagged with the HTTP method, the route matching pattern, and the HTTP status family.

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

By default, the T2 endpoint factory computes the ingestion endpoint from the OCI Monitoring endpoint by replacing
`telemetry.` with `telemetry-ingestion.`. You can override the endpoint directly:

```yaml
metrics:
  publishers:
    - type: oci
      project: my-service
      fleet: my-fleet
      endpoint: https://telemetry-ingestion.example.com
```

Applications or libraries can also provide a higher-weight `@OciMetrics Function<Monitoring, URI>` service to choose a
different metrics endpoint.

---

## Configuration

### Publisher configuration

The OCI metrics publisher is configured as an entry under `metrics.publishers` with `type` `oci`.
When publisher `availability-domain` or `fault-domain` is omitted, the OCI metrics publisher uses
`oci.env.availability-domain` and `oci.env.fault-domain` when those values are available.

| Key | Default value | Description |
|-----|---------------|-------------|
| `type` | | Set to `oci` to use this publisher. |
| `enabled` | `true` | Enables or disables OCI metrics publishing. |
| `project` | | OCI metrics project. Required for publishing. |
| `fleet` | | OCI metrics fleet. Required for publishing. |
| `region` | | Optional public region name. If set, this takes precedence over a registry-provided PIC `Region`. |
| `endpoint` | | Optional monitoring ingestion endpoint override. |
| `default-dimensions` | `{}` | Default dimensions for OCI `com.oracle.pic.telemetry.commons.metrics.Metrics.init`. |
| `request-headers` | `{}` | Additional headers to send with OCI monitoring requests. |
| `sample-gauges` | `true` | Enables scheduled gauge and functional-counter sampling. |
| `duration-unit` | `milliseconds` | Unit for emitted timer duration values. |
| `metrics-scope-name` | `service` | Root name segment used as the prefix for built-in JVM metric names. |
| `includes` | `[]` | Metric names to include. An empty list includes all non-excluded metrics. |
| `excludes` | `[]` | Metric names to exclude. Excludes take precedence over includes. |
| `use-regex-filters` | `false` | Treat `includes` and `excludes` entries as regular expressions. Regex matching uses full-pattern semantics. |
| `use-substring-matching` | `false` | Treat `includes` and `excludes` entries as substrings. Used only when `use-regex-filters` is `false`. |
| `includes-attributes` | `max`, `mean`, `min`, `stddev`, `p50`, `p75`, `p95`, `p98`, `p99`, `p999`, `count`, `m1_rate`, `m5_rate`, `m15_rate`, `mean_rate` | Metric attribute names to include when reporting derived values. |
| `excludes-attributes` | `[]` | Metric attribute names to exclude when reporting derived values. |
| `gauge-sample-interval` | `PT1M` | Interval between scheduled gauge and functional-counter samples. |
| `use-metadata-service` | | Optional flag passed to the OCI telemetry reporter builder. |
| `override-metric-keys` | | Optional flag passed to the OCI telemetry reporter builder. |
| `hostname` | | Optional hostname override for emitted dimensions. |
| `host-name` | | Alias for `hostname`; configure only one of the two keys. |
| `availability-domain` | `oci.env.availability-domain` | Optional availability-domain override. |
| `fault-domain` | `oci.env.fault-domain` | Optional fault-domain override. |

Aliases (such as `hostname` and `host-name`) are provided for user convenience, either to align with native OCI parameter names or with similar settings
in other Helidon OCI modules. Specify at most one name for an aliased setting, not both.

Example:

```yaml
metrics:
  publishers:
    - type: oci
      project: my-service
      fleet: my-fleet
      duration-unit: seconds
      metrics-scope-name: my-service
      excludes:
        - "my-service\\.jvm\\..*"
      use-regex-filters: true
```

### Monitoring client configuration

Use `client` to tune the OCI SDK `MonitoringClient`.

| Key | Default value | Description |
|-----|---------------|-------------|
| `client.connection-timeout` | | Optional OCI SDK connection timeout. |
| `client.read-timeout` | | Optional OCI SDK read timeout. |
| `client.max-async-threads` | | Optional maximum async thread count. |
| `client.disable-data-buffering-on-upload` | | Optional upload buffering flag. |
| `client.retry` | | Optional OCI SDK retry configuration. |
| `client.circuit-breaker` | | Optional OCI SDK circuit-breaker configuration. |

Example:

```yaml
metrics:
  publishers:
    - type: oci
      project: my-service
      fleet: my-fleet
      client:
        connection-timeout: PT7S
        read-timeout: PT11S
        max-async-threads: 13
```

### Retry configuration

Retry configuration mirrors the OCI SDK retry types.

| Key | Default value | Description |
|-----|---------------|-------------|
| `client.retry.termination-strategy.type` | | `max-attempts` or `max-time`. |
| `client.retry.termination-strategy.max-attempts` | | Maximum attempts for `max-attempts`. |
| `client.retry.termination-strategy.max-time` | | Maximum duration for `max-time`. |
| `client.retry.delay-strategy.type` | | `fixed`, `exponential`, or `exponential-with-jitter`. |
| `client.retry.delay-strategy.delay` | | Fixed delay for `fixed`. |
| `client.retry.delay-strategy.max-delay` | | Maximum delay for exponential strategies. |
| `client.retry.retry-condition.type` | | `default` or `retry-on-open-circuit-breaker`. |
| `client.retry.retry-options.mark-read-limit` | | Optional mark-read limit. |

Example:

```yaml
metrics:
  publishers:
    - type: oci
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

| Key | Default value | Description |
|-----|---------------|-------------|
| `client.circuit-breaker.failure-rate-threshold` | | Failure-rate threshold. |
| `client.circuit-breaker.slow-call-rate-threshold` | | Slow-call-rate threshold. |
| `client.circuit-breaker.wait-duration-in-open-state` | | Time to remain open before half-open. |
| `client.circuit-breaker.permitted-number-of-calls-in-half-open-state` | | Permitted half-open calls. |
| `client.circuit-breaker.minimum-number-of-calls` | | Minimum calls before calculating state. |
| `client.circuit-breaker.sliding-window-size` | | Sliding window size. |
| `client.circuit-breaker.slow-call-duration-threshold` | | Duration threshold for slow calls. |
| `client.circuit-breaker.writable-stack-trace-enabled` | | Optional writable stack trace flag. |

---

## References

* [Metrics example](../../examples/metrics/)
* [Helidon metrics API](https://helidon.io/docs/latest/se/metrics/metrics)

# Metrics

---

## Overview

The Helidon OCI metrics integration component sends updates of Helidon neutral metrics--registered and updated both imperatively and declaratively (annotations)--to the OCI metrics backend. It layers on top of any
standard Helidon metrics provider (currently Micrometer), so services continue to use the normal Helidon metrics APIs while the
OCI integration publishes metric updates to the backend.

When this module is on the classpath, Helidon can:

* publish `Counter`, `Timer`, and `DistributionSummary` updates to OCI metrics
* periodically sample and publish gauges and functional counters
* register HTTP request counters and timers tagged with the matching path, HTTP method, and HTTP response status family
* register optional JVM gauges for memory usage, thread state, file descriptors, and garbage collection
* create the required OCI `Monitoring` client and make it available to services and libraries via the Helidon service registry

Configuration of the metrics integration is via a Helidon metrics publisher of type `oci` as illustrated in the [example below](#configure-oci-metrics-publishing)

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

If `region` is omitted, the integration attempts to use a `com.oracle.pic.commons.util.Region` from the Helidon
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

### Use Helidon neutral metrics API

Services can use the normal Helidon metrics API. The OCI integration wraps the Micrometer-backed meters and publishes
updates to OCI metrics after successful meter updates.

The following example shows imperative use of the Helidon metrics API in a hypothetical utility class `WorkService` that counts and times the invocations of the work. (Note that each timer includes a counter, so production code would rarely measure the same code using both; this is just an example to show the imperative API for both timers and counters.)

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

### Use Helidon metric annotations

Helidon metric annotations also work because Helidon's handling of the metrics annotations use the Helidon neutral metrics API. Thanks to this library, _all_ uses of the Helidon metrics API update OCI metrics as well.

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

The integration can register gauges for:

* memory usage
* thread state
* file descriptors
* garbage collection

Each group is enabled by default and can be disabled individually.

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

| Key | Default value | Description |
|-----|---------------|-------------|
| `metrics.publishers[].type` | | Set to `oci` to use this publisher. |
| `metrics.publishers[].enabled` | `true` | Enables or disables OCI metrics publishing. |
| `metrics.publishers[].project` | | OCI metrics project. Required for publishing. |
| `metrics.publishers[].fleet` | | OCI metrics fleet. Required for publishing. |
| `metrics.publishers[].region` | | Optional public region name. If set, this takes precedence over a registry-provided PIC `Region`. |
| `metrics.publishers[].endpoint` | | Optional monitoring ingestion endpoint override. |
| `metrics.publishers[].default-dimensions` | `{}` | Default dimensions passed to the OCI metrics library. |
| `metrics.publishers[].request-headers` | `{}` | Additional headers to send with OCI monitoring requests. |
| `metrics.publishers[].reporting-time-unit` | `milliseconds` | Unit for emitted time values. |
| `metrics.publishers[].sample-gauges` | `true` | Enables scheduled gauge and functional-counter sampling. |
| `metrics.publishers[].gauge-sample-interval` | `PT1M` | Interval between scheduled gauge and functional-counter samples. |
| `metrics.publishers[].use-metadata-service` | | Optional flag passed to the OCI telemetry reporter builder. |
| `metrics.publishers[].override-metric-keys` | | Optional flag passed to the OCI telemetry reporter builder. |
| `metrics.publishers[].hostname` | | Optional hostname override for emitted dimensions. |
| `metrics.publishers[].host-name` | | Alias for `hostname`; configure only one of the two keys. |
| `metrics.publishers[].availability-domain` | | Optional availability-domain override. |
| `metrics.publishers[].fault-domain` | | Optional fault-domain override. |

Aliases are provided for user convenience, either to align with native OCI parameter names or with similar settings
in other Helidon OCI modules. Specify at most one name for an aliased setting, not both.

### Monitoring client configuration

Use `client` to tune the OCI SDK `MonitoringClient`.

| Key | Default value | Description |
|-----|---------------|-------------|
| `metrics.publishers[].client.connection-timeout` | | Optional OCI SDK connection timeout. |
| `metrics.publishers[].client.read-timeout` | | Optional OCI SDK read timeout. |
| `metrics.publishers[].client.max-async-threads` | | Optional maximum async thread count. |
| `metrics.publishers[].client.disable-data-buffering-on-upload` | | Optional upload buffering flag. |
| `metrics.publishers[].client.retry` | | Optional OCI SDK retry configuration. |
| `metrics.publishers[].client.circuit-breaker` | | Optional OCI SDK circuit-breaker configuration. |

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
| `metrics.publishers[].client.retry.termination-strategy.type` | | `max-attempts` or `max-time`. |
| `metrics.publishers[].client.retry.termination-strategy.max-attempts` | | Maximum attempts for `max-attempts`. |
| `metrics.publishers[].client.retry.termination-strategy.max-time` | | Maximum duration for `max-time`. |
| `metrics.publishers[].client.retry.delay-strategy.type` | | `fixed`, `exponential`, or `exponential-with-jitter`. |
| `metrics.publishers[].client.retry.delay-strategy.delay` | | Fixed delay for `fixed`. |
| `metrics.publishers[].client.retry.delay-strategy.max-delay` | | Maximum delay for exponential strategies. |
| `metrics.publishers[].client.retry.retry-condition.type` | | `default` or `retry-on-open-circuit-breaker`. |
| `metrics.publishers[].client.retry.retry-options.mark-read-limit` | | Optional mark-read limit. |

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
| `metrics.publishers[].client.circuit-breaker.failure-rate-threshold` | | Failure-rate threshold. |
| `metrics.publishers[].client.circuit-breaker.slow-call-rate-threshold` | | Slow-call-rate threshold. |
| `metrics.publishers[].client.circuit-breaker.wait-duration-in-open-state` | | Time to remain open before half-open. |
| `metrics.publishers[].client.circuit-breaker.permitted-number-of-calls-in-half-open-state` | | Permitted half-open calls. |
| `metrics.publishers[].client.circuit-breaker.minimum-number-of-calls` | | Minimum calls before calculating state. |
| `metrics.publishers[].client.circuit-breaker.sliding-window-size` | | Sliding window size. |
| `metrics.publishers[].client.circuit-breaker.slow-call-duration-threshold` | | Duration threshold for slow calls. |
| `metrics.publishers[].client.circuit-breaker.writable-stack-trace-enabled` | | Optional writable stack trace flag. |

### JVM meter configuration

Each JVM meter group is enabled by default.

| Key | Default value | Description |
|-----|---------------|-------------|
| `metrics.publishers[].jvm-meters.memory-usage-enabled` | `true` | Enables memory-usage gauges. |
| `metrics.publishers[].jvm-meters.thread-state-enabled` | `true` | Enables thread-state gauges. |
| `metrics.publishers[].jvm-meters.file-descriptor-enabled` | `true` | Enables file-descriptor gauges. |
| `metrics.publishers[].jvm-meters.gc-enabled` | `true` | Enables garbage-collection gauges. |

Example:

```yaml
metrics:
  publishers:
    - type: oci
      project: my-service
      fleet: my-fleet
      jvm-meters:
        memory-usage-enabled: true
        thread-state-enabled: true
        file-descriptor-enabled: false
        gc-enabled: true
```

---

## References

* [Metrics example](../../examples/metrics/README.md)
* [Helidon metrics API](https://helidon.io/docs/latest/se/metrics/metrics)

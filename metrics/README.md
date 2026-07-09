# Metrics with T2

---

## Overview
This library supports declarative and imperative Helidon metrics using OCI T2 metrics.

### Design
Most of this component is an implementation of the Helidon neutral metrics API, one that transmits metrics data via the OCI T2 APIs and also keeps minimal in-memory data to support most operations which get the current value of each meter type.

This metrics provider keeps local values for counters, timers, distribution summaries, gauges, and functional counters, then periodically samples registered meters and invokes T2 `Metrics.emit`. Application metric mutation paths do not report changes directly to T2.

Sampled counters are emitted as interval event counts using the configured metric name. Timers are emitted as MetricsScope-style duration observations in milliseconds using the configured metric name; they are not emitted as Dropwizard scheduled-reporter rates. Distribution summaries are emitted as observations using the configured metric name. A `SuccessRate` distribution summary records `0.0` or `1.0`, so the emitted observations preserve the success-rate input values.

Timer and distribution summary observations are retained in bounded per-second accumulators until the next sample cycle. Buckets retain exact raw observations up to the configured cap; above the cap, the bucket compacts to min, max, and weighted middle mean observations that preserve count, sum, mean, min, and max. The `sample-interval` and `accumulators` settings control the tradeoff between sampler frequency, exactness under very high traffic, and peak retained observations.

The component uses Helidon configuration (config file or API) to programmatically set up the runtime behavior of T2, using several config blueprints and builders.

This component also includes a Helidon metrics publisher by which users control almost all aspects of how T2 sends data to the backend, including the following:
* project
* fleet
* endpoint
* hostname
* region
* availability domain
* fault domain
* region ID
* override metric keys
* request headers (added to every transmission to T2)
* default dimensions (name/value pairs added to each metric metadata)
* T2 JVM meters config
* client configuration
  * connection timeout
  * read timeout
  * circuit breaker config
    * failure rate threshold
    * slow call rate threshold
    * slow call duration threshold
    * wait duration in open state
    * permitted number of call sin half-open state
    * minimum number of calls
    * sliding window size
    * writeable stack trace enabled
  * retry configuration
    * delay strategy
    * termination strategy
    * retry condition
    * retry options

Users can control the time unit in which Helidon timer data is expressed when sent to T2:
* reporting time unit (default ms)

Users can control the Helidon-specific periodic metric sampling interval:
* sample interval (default 1 second)

Users can control bounded event accumulator behavior:
* maximum pending seconds per meter (default 10)
* maximum exact raw timer samples per second (default 1024)
* maximum exact raw distribution summary samples per second (default 1024)
* pressure log interval for compaction/drop notices (default 30 seconds)

Users can bound automatic HTTP user-agent metric cardinality:
* maximum detailed user-agent series (default 1000)
* stable client-family counters remain available after the limit is reached
* excess detailed identities are aggregated into `Request.Client.OTHER.*` counters


Users configure these settings as the `oci` metrics publisher:
```yaml
metrics:
  publishers:
    - type: oci
      ...
      sample-interval: PT1S
      accumulators:
        max-pending-seconds: 10
        max-raw-timer-samples-per-second: 1024
        max-raw-summary-samples-per-second: 1024
        pressure-log-interval: PT30S
      auto-http:
        max-user-agent-series: 1000
```

### Usage
To publish metrics to T2, follow two main steps:
1. Add a dependency on `com.oracle.helidon.oci.metrics:helidon-metrics-provider-oci`.
2. Add configuration:
   
   ```yaml
   metrics:
     publishers:
       - type: oci
         
   ```

Notes:
* The region typically comes from the environment, but you can override it using configuration.

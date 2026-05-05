# Metrics with T2

---

## Overview
This library supports declarative and imperative Helidon metrics using OCI T2 metrics.

### Design
Most of this component is an implementation of the Helidon neutral metrics API, one that transmits metrics data via the OCI T2 APIs and also keeps minimal in-memory data to support most operations which get the current value of each meter type.

This metrics provider augments updates to the internal values inside each counter, timer, and distribution summary (histogram) with invocations of the T2 `Metrics.sensor` method, thereby making those updates available for the T2 metrics library to transmit to the T2 backend. Because gauges are not updated via the metrics API but instead are wrappers around separately-updated values, we have no automatic way of discovering when a gauge's underlying value is updated. Therefore, this component includes periodic sampling of all registered gauges.

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

Users can control the Helidon-specific periodic gauge sampling:
* sample gauges (boolean, default true)
* gauge sample interval (default 1 minute)


Users configure these settings as the `oci` metrics publisher:
```yaml
metrics:
  publishers:
    - type: oci
      ...
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


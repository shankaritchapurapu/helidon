# OCI Metrics Integration Notes

## Basic Functionality
The Helidon OCI metrics integration library automatically sends the following metrics to the telemetry backend:
* Any Helidon metric registered by the service or its dependencies, whether imperatively using the neutral Helidon metrics API or declaratively using the Helidon `@Metrics.Timed` and `@Metrics.Counted` annotations.
* Several JVM-related gauges.
* A family of timers `http.requests.duration` and counters `http.request.count` covering all incoming HTTP requests, with tags for:
   * HTTP method name
   * matched routing path expression
   * HTTP response status family.

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

## Metrics library choice
As of OCI Helidon 2.0, metrics integration uses the `metrics-lib` library from the OCI telemetry team.

There are multiple libraries available for working with OCI metrics, among them:
* [`com.oracle.pic.telemetry.commons:metrics-lib`](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-lib/browse)
* [`com.oracle.pic.commons:metrics`](https://bitbucket.oci.oraclecorp.com/projects/COMMONS/repos/metrics/browse)

The `metrics-lib` `README.md` states, among other things:
> NOTE: the PIC commons metrics library is more complex and more powerful in some ways but is much more prone to memory leaks if not carefully used. You should consider using this metrics-lib library directly where possible.

For simplicity of our code and for reliability, this release of the T2 metrics integration follows the suggestion above and uses the `metrics-lib` library. This allows us to delegate to the T2 library all the responsibility of buffering metrics data and transmitting it (retrying if necessary) to the backend. That library _does not_ expose a way to control the frequency with which it sends data; it transmits (if values have been reported to it) at least each minute, more frequently if its internal buffers fill.


# Metrics with T2

---

## Contents


* [Overview](#overview)
* [Maven Coordinates]()
* [Usage](#usage)
* [Configuration](#configuration)
* [Examples](#examples)
* [References](#references)

---

## Overview

The Metrics module provides a way to publish metrics to T2 using the following features:
1. Automatically publishes [Helidon MP metrics](https://helidon.io/docs/v4/mp/metrics/metrics) to T2 
2. Allow to programmatically use [metrics-lib](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-lib/browse) APIs to generate and publish metrics to T2 via the metrics-reporter.


---

## Maven Coordinates

To enable the metrics module, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-metrics</artifactId>
   <scope>runtime</scope>
</dependency>
```

---

## Usage

### Helidon MP Metrics

1. Learn how to [Instrument your Service](https://helidon.io/docs/v4/mp/metrics/metrics#_usage) using the Helidon MP Metrics.
2. Set `oci.metrics` [configuration properties](#configuration) to enable metrics publishing to T2.
3. Check devops/metrics or grafana

### T2 metrics-lib APIs

1. The `TelemetryReporter` is automatically instantiated and initialized using the metrics-lib API when the metrics module is added as a dependency.
2. Developers can use various metrics-lib APIs in their code to start instrumenting their services. Example: 
   ```
   Metrics.emit("CreateRecord.success", 5d);
   try (Scope scope = Metrics.scope("WorkRequestGcRun")) {
       scope.emit("deleteRecordsCount", runStatus.getRecordCount());
       scope.emit("deleteWorkRequestsCount", runStatus.getWorkRequestCount());
       scope.recordSuccess();
   }
   ```
3. Check devops/metrics or grafana   

---

## Configuration

Configure Helidon MP metrics publishing using the Helidon microprofile configuration framework by which the ConfigSource defaults to
`META-INF/microprofile-config.properties`. Alternatively, you can also use other ConfigSources such as `application.yaml`.

| config key               | Default Value | Description                                                                                                                                                                                                                                                                                                                                                    |
|--------------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| oci.metrics.project      | none          | Project is the  is the top-level organizational unit for a team's metrics and usually identifies the application or service sending the metrics.                                                                                                                                                                                                               |
| oci.metrics.fleet        | none          | Fleet is an optional metric attribute used to organize, identify, and aggregate your metrics.                                                                                                                                                                                                                                                                  |
| oci.metrics.initialDelay | 1s            | Delay in seconds before the first metrics transmission to T2 takes place.                                                                                                                                                                                                                                                                                      |
| oci.metrics.delay        | 60s           | Interval in seconds between metrics transmission to T2.                                                                                                                                                                                                                                                                                                        |
| oci.metrics.scopes       | All scopes    | List of metrics scopes (e.g., base, vendor, application) that will be sent to T2.                                                                                                                                                                                                                                                                              |
| oci.metrics.enabled      | true          | Enables or disables metric publishing to T2.                                                                                                                                                                                                                                                                                                                   |
| oci.metrics.batchSize    | 50            | Maximum number of metrics to send in a batch. This option is provided to help throttle the number of metrics sent per publishing. If the value of this property is greater than the collected metrics, then they will be divided up and sent in batches using this value as the maximum count for each batch and with an interval of `oci.metrics.batchDelay`. |
| oci.metrics.batchDelay   | 5s            | Sets the delay interval if metrics are posted in batches.                                                                                                                                                                                                                                                                                                      |

---

## Examples

### Application Metrics
1. Use [Metric-defining annotation](https://helidon.io/docs/v4/mp/metrics/metrics#_api) to count the invocation of the getJobRequest API method, which inn the example below, is achieved using `@Counted`.
   ```java
   @Override
   @Counted(name = "getJobRequest", absolute = true)
   public JobRequest getJobRequest(
           String managedInstanceId,
           String compartmentId,
           String opcRequestId,
           String opcRetryToken,
           HttpHeaders httpHeadersContext) {
       LOGGER.info("getJobRequest(" + managedInstanceId + ") is invoked");
       if (httpHeadersContext.getRequestHeaders().containsKey("invalid")) {
           throw new RenderableException(ErrorCode.InternalError, "Invalid header");
       }
       return DiscoverDetails.builder().jobId(123).build();
   }
   ```
2. Enable metrics publishing to T2 which includes sending application metrics by which the result of `@Counted` will be part of. The META-INF/microprofile-config.properties settings below will send metrics every 3 minutes. 
   ```properties
   # Helidon Metrics to OCI parameters
   # OCI metric namespace
   oci.metrics.project=helidon_oci
   oci.metrics.fleet=dataplane
   # Delay in seconds before the 1st metrics transmission to OCI takes place.  Defaults to 1 second if not specified.
   oci.metrics.initialDelay=10
   # Interval in seconds between metrics transmission to OCI. Defaults to 60 seconds if not specified.
   oci.metrics.delay=180
   # Filter only the scopes that will be sent to OCI. This is optional and will default to all scopes if not specified.
   oci.metrics.scopes.0=base
   oci.metrics.scopes.1=vendor
   oci.metrics.scopes.2=application
   # Enable metric transmission to T2
   oci.metrics.enabled=true
   oci.metrics.batchSize=3
   oci.metrics.batchDelay=5
   ```
3. Use [Metrics Explorer](https://devops.oci.oraclecorp.com/telemetry/mql/explore) or [Grafana](https://grafana.oci.oraclecorp.com/) to filter the results related to the number of invocations of `getJobRequest()` by specifying the `project` name (`helidon_oci` in the example above), the `fleet` name (`dataplane` in the same example)  and the annotated counter metric name (`getJobRequest` from the code example) .


---

## References

* [Overlay Telemetry T2 Service](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/telemetry/landing-telemetryt2.htm)
* [Metrics in Helidon MP](https://helidon.io/docs/v4/mp/metrics/metrics)

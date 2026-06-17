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

### Reporter-Style Configuration
The OCI metrics publisher exposes settings which correspond to DropWizard's `BaseReporterFactory` reporter controls:
* `duration-unit`: The time unit to use when normalizing timer durations before reporting them to OCI. The default is `milliseconds`.
* `metrics-scope-name`: The root name segment used as the prefix for built-in JVM metric names. The default is `service`, so JVM metrics use names such as `service.jvm.memory.heap.used`.
* `includes`: Metric names to include.
* `excludes`: Metric names to exclude.
* `use-regex-filters`: Treat `includes` and `excludes` entries as regular expressions. Matching uses full-pattern semantics.
* `use-substring-matching`: Treat `includes` and `excludes` entries as substrings. This applies only when `use-regex-filters` is not set.
* `includes-attributes`: Metric attribute names to include when reporting derived values.
* `excludes-attributes`: Metric attribute names to exclude when reporting derived values.

The DropWizard `BaseReporterFactory` also exposes a `rateUnit` setting, but Helidon OCI does not expose it because the
service-core scheduled metrics reporter code does not seem to use it.

Reporter include and exclude decisions apply to metric names only. Excludes take precedence over includes, and an empty `includes` list means all non-excluded metrics are eligible for publishing. The programmatic `filter` setting is intentionally not configurable from YAML; it exists only for code which builds the config directly.

#### Heliport implications
When Heliport migrates service-core `scheduled-metrics-reporter` configuration to Helidon OCI metrics configuration, it should map the legacy reporter settings into the single Helidon OCI publisher object at `metrics.publishers.oci`:

| service-core `scheduled-metrics-reporter` setting | Helidon OCI setting |
| --- | --- |
| `durationUnit` | `duration-unit` |
| `metricsScopeName` | `metrics-scope-name` |
| `includes` | `includes` |
| `excludes` | `excludes` |
| `useRegexFilters` | `use-regex-filters` |
| `useSubstringMatching` | `use-substring-matching` |
| `includesAttributes` | `includes-attributes` |
| `excludesAttributes` | `excludes-attributes` |

Heliport should not map the legacy scheduled reporter interval/frequency setting. Helidon OCI reports metric updates to the OCI metrics layer as those updates occur, except for gauges. Gauge sampling is controlled separately using `sample-gauges` and `gauge-sample-interval`.

Roughly speaking, a DropWizard reporter corresponds to a Helidon metrics publisher. The legacy OCI metrics configuration can specify multiple reporters, but Helidon OCI supports only one set of reporter-style controls in the `oci` metrics publisher. This is intentional: the legacy DropWizard-based approach periodically reported all metrics through each configured reporter, while Helidon OCI records most metric updates immediately as they happen. If a legacy config contains multiple scheduled metrics reporters, Heliport should choose or consolidate to one Helidon OCI publisher configuration and flag any ambiguous cases for review. If future requirements emerge to support multiple DW reporters, we can look at adding additional OCI-related metrics publishers to match.

## Metrics library choice
As of OCI Helidon 2.0, metrics integration uses the `metrics-lib` library from the OCI telemetry team.

There are multiple libraries available for working with OCI metrics, among them:
* [`com.oracle.pic.telemetry.commons:metrics-lib`](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-lib/browse)
* [`com.oracle.pic.commons:metrics`](https://bitbucket.oci.oraclecorp.com/projects/COMMONS/repos/metrics/browse)

The `metrics-lib` `README.md` states, among other things:
> NOTE: the PIC commons metrics library is more complex and more powerful in some ways but is much more prone to memory leaks if not carefully used. You should consider using this metrics-lib library directly where possible.

For simplicity of our code and for reliability, this release of the T2 metrics integration follows the suggestion above and uses the `metrics-lib` library. This allows us to delegate to the T2 library all the responsibility of buffering metrics data and transmitting it (retrying if necessary) to the backend. That library _does not_ expose a way to control the frequency with which it sends data; it transmits (if values have been reported to it) at least each minute, more frequently if its internal buffers fill.

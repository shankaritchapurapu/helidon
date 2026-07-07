# Metrics RC2 to RC3 Migration Notes

This note is for pilot teams moving from the RC2 OCI Helidon metrics integration to RC3.

RC3 keeps the previous overlay reporter support and adds substrate reporter support. See [Substrate Reporter](#substrate-reporter) below for the substrate configuration shape.

RC3 moves OCI metrics reporter construction settings out of the top-level OCI metrics publisher config and into a nested `reporter` config. Publisher behavior settings remain at the OCI publisher level.

This doc uses Helidon's `metrics.publishers` list form. If a team refers to this as the `metrics.publisher.oci` level, the migration is the same: settings that used to sit directly on the OCI publisher now sit under the selected `reporter` variant.

## Required Config Change

In RC2, reporter settings were configured directly on the OCI metrics publisher:

```hocon
metrics {
  publishers = [
    {
      type = "oci"
      enabled = true
      project = "my-project"
      fleet = "my-fleet"
      region = "us-ashburn-1"
      endpoint = "https://telemetry-ingestion.example.com"
      hostname = "my-host"
      availability-domain = "iad-ad-1"
      fault-domain = "1"
      request-headers {
        opc-example = "value"
      }
      use-metadata-service = false
      override-metric-keys = true
    }
  ]
}
```

In RC3, put those reporter settings under `reporter.overlay`:

```hocon
metrics {
  publishers = [
    {
      type = "oci"
      enabled = true
      reporter {
        overlay {
          project = "my-project"
          fleet = "my-fleet"
          region = "us-ashburn-1"
          endpoint = "https://telemetry-ingestion.example.com"
          hostname = "my-host"
          availability-domain = "iad-ad-1"
          fault-domain = "1"
          request-headers {
            opc-example = "value"
          }
          use-metadata-service = false
          override-metric-keys = true
        }
      }
    }
  ]
}
```

`overlay` is the default reporter type and preserves the RC2 `TelemetryReporterBuilder` behavior.

## Setting Mapping

Move these RC2 top-level OCI publisher settings to `reporter.overlay` in RC3:

| RC2 setting | RC3 setting |
| --- | --- |
| `project` | `reporter.overlay.project` |
| `fleet` | `reporter.overlay.fleet` |
| `client` | `reporter.overlay.client` |
| `endpoint` | `reporter.overlay.endpoint` |
| `hostname` | `reporter.overlay.hostname` |
| `host-name` | `reporter.overlay.host-name` |
| `availability-domain` | `reporter.overlay.availability-domain` |
| `fault-domain` | `reporter.overlay.fault-domain` |
| `region` | `reporter.overlay.region` |
| `request-headers` | `reporter.overlay.request-headers` |
| `use-metadata-service` | `reporter.overlay.use-metadata-service` |
| `override-metric-keys` | `reporter.overlay.override-metric-keys` |

Programmatic reporter settings move the same way conceptually:

| RC2 programmatic setting | RC3 programmatic setting |
| --- | --- |
| `monitoring(...)` on `OciMetricsPublisherConfig.Builder` | `monitoring(...)` on `OverlayMetricReporterConfig.Builder` |
| `reporter(...)` on `OciMetricsPublisherConfig.Builder` | unchanged; still overrides configured reporter creation |

## Settings That Stay at the Publisher Level

Do not move these settings. They still configure publisher behavior, not reporter construction:

| Setting |
| --- |
| `enabled` |
| `default-dimensions` |
| `sample-interval` |
| `metrics-scope-name` |
| `includes` |
| `excludes` |
| `filter-matching-mode` |
| `includes-attributes` |
| `excludes-attributes` |
| `enable-detailed-timing-auto-metrics` |
| `resource-package-prefix` |

## Substrate Reporter

RC3 also adds a substrate reporter option backed by `DianogaReporter`.

Use `reporter.substrate` instead of `reporter.overlay`:

```hocon
metrics {
  publishers = [
    {
      type = "oci"
      reporter {
        substrate {
          project = "my-project"
          fleet = "my-fleet"
          region = "us-ashburn-1"
          endpoint = "https://t2.example.com"
          hostname = "my-host"
          availability-domain = "iad-ad-1"
          fault-domain = "1"
        }
      }
    }
  ]
}
```

Common reporter settings are the same for overlay and substrate:

| Overlay setting | Substrate setting |
| --- | --- |
| `reporter.overlay.project` | `reporter.substrate.project` |
| `reporter.overlay.fleet` | `reporter.substrate.fleet` |
| `reporter.overlay.client` | `reporter.substrate.client` |
| `reporter.overlay.endpoint` | `reporter.substrate.endpoint` |
| `reporter.overlay.hostname` | `reporter.substrate.hostname` |
| `reporter.overlay.host-name` | `reporter.substrate.host-name` |
| `reporter.overlay.availability-domain` | `reporter.substrate.availability-domain` |
| `reporter.overlay.fault-domain` | `reporter.substrate.fault-domain` |
| `reporter.overlay.region` | `reporter.substrate.region` |

Overlay-only settings do not apply to substrate:

| Overlay-only setting |
| --- |
| `reporter.overlay.request-headers` |
| `reporter.overlay.use-metadata-service` |
| `reporter.overlay.override-metric-keys` |
| programmatic `OverlayMetricReporterConfig.Builder.monitoring(...)` |

Substrate also supports a programmatic-only `MetricTimeSeriesClient` override on `SubstrateMetricReporterConfig.Builder`. It is not configurable from HOCON.

## Important RC3 Notes

RC3 selects the reporter by the nested provider key.

`host-name` remains supported as an alias for `hostname` under the nested reporter config. Do not configure both `hostname` and `host-name` for the same reporter.

If `availability-domain` or `fault-domain` is omitted from the nested reporter config, RC3 can still use root `oci.env.availability-domain` and `oci.env.fault-domain` defaults.

# Oracle APM with Helidon OpenTelemetry

---

## Overview

Oracle Application Performance Monitoring (APM) accepts OpenTelemetry data using OTLP over HTTP.
Helidon applications should use Helidon's OpenTelemetry support to configure tracing export to
Oracle APM.

The APM configuration is documented in the public Helidon OpenTelemetry guide:

* [Exporting to Oracle APM](https://helidon.io/docs/latest/se/telemetry/open-telemetry)

---

## Notes

There is no separate OCI APM module. Configure APM using the standard
Helidon OpenTelemetry dependencies and `telemetry` configuration described in the Helidon docs.

# Helidon OCI Documentation

This repository contains Helidon extensions for OCI-native services and OCI-specific platform
capabilities. The modules in `oci-helidon` help Helidon applications integrate with internal OCI
services, wire OCI-aware configuration and request handling into the runtime, and adopt common OCI
operational patterns such as auditing, metrics publishing, request tracing, and secure secret
retrieval.

The project currently targets [Helidon 4](https://helidon.io/docs/v4/about/doc_overview) and
JDK 25.

## Documentation Guide

### Service Integrations

* [Audit](./audit.md) - OCI `AuditV2Filter` integration for capturing audited request and response activity.
* [Identity](./identity.md) - Authentication, authorization, and request-scoped identity context integration.
* [Kiev](./kiev.md) - Helidon configuration and service registry bindings for Kiev client libraries.
* [Limits](./limits.md) - Helidon service integration for interacting with OCI Limits.
* [Metrics](./metrics.md) - Publishing Helidon and application metrics to T2.
* [Secret Service V2](./secret-service.md) - Secret retrieval and TLS rotation support through Secret Service.
* [SPLAT](./splat.md) - mTLS validation integration for Helidon endpoints using the upstream SPLAT filter.
* [Workflow](./workflow.md) - Generated configuration and bindings for OCI Workflow-as-a-Service integration.

### Platform Utilities

* [Environment Configuration](./environment-config.md) - OCI-aware Helidon config source and region providers.
* [Error Code](./error-code.md) - Support for OCI-standard error code handling in Helidon services.
* [Request ID](./request-id.md) - `opc-request-id` propagation, generation, and logging context support.

### Operational Guidance

* [Jipher](./jipher.md) - Guidance for enabling the Oracle Jipher security provider for FIPS-oriented deployments.
* [Lumberjack](./lumberjack.md) - Logging and onboarding guidance for sending service logs to Lumberjack.

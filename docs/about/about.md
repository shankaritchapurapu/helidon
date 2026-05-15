# Helidon OCI Documentation

Helidon OCI is a collection of Helidon extensions for OCI-native services and OCI-specific platform
capabilities. These modules help Helidon applications integrate with internal OCI
services, wire OCI-aware configuration and request handling into the runtime, and adopt common OCI
operational patterns such as auditing, metrics publishing, request tracing, and secure secret
retrieval.

The project currently targets [Helidon 4](https://helidon.io/docs/v4/about/doc_overview) and JDK 25.

## Documentation Guide

The documentation is organized into:

| Topic                                          | Description                                                            |
|------------------------------------------------|------------------------------------------------------------------------|
| [Service Integrations](../services/README.md)  | Integrations with native services such as Identity, Kiev, etc          |
| [Utility Integrations](../utilities/README.md) | Integrations with platform utilities like error codes and request ids. |
| [Guides](../guides/README.md)                  | Operational guides for using lumberjack, apm and Jipher.               |

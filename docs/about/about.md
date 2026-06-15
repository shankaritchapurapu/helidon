# Helidon OCI Documentation

Helidon OCI is a collection of Helidon extensions for OCI-native services and OCI-specific platform
capabilities. These modules help Helidon applications integrate with internal OCI
services, wire OCI-aware configuration and request handling into the runtime, and adopt common OCI
operational patterns such as auditing, metrics publishing, request tracing, and secure secret
retrieval.

The project currently targets [Helidon 4](https://helidon.io/docs/v4/about/doc_overview) and JDK 25.

## Service Registry Defaults

Helidon OCI modules register framework-provided `Supplier<?>` and
`Service.InjectionPointFactory<?>` services, and framework-provided
`Service.ServicesFactory<?>` registry expansions, with a weight of
`Weighted.DEFAULT_WEIGHT - 30`. These registrations provide library defaults for
clients, configuration objects, request-scoped values, and other integration services.

The lower weight lets applications override a Helidon OCI default by registering the same
contract at Helidon's default weight. In most cases, an application-provided service
with no explicit `@Weight` annotation is enough to take precedence over the library
registration. Use `@Weight` in application code only when you need explicit ordering
among multiple application-provided services for the same contract.

## Documentation Guide

The documentation is organized into:

| Topic                                          | Description                                                            |
|------------------------------------------------|------------------------------------------------------------------------|
| [Service Integrations](../services/README.md)  | Integrations with native services such as Identity, Kiev, etc          |
| [Utility Integrations](../utilities/README.md) | Integrations with platform utilities like error codes and request ids. |
| [Guides](../guides/README.md)                  | Operational guides for using lumberjack, apm and Jipher.               |

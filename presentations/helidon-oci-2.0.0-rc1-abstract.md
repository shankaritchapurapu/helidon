# Helidon Talon 2.0.0 (RC1) Presentation Abstract

Helidon Talon 2.0.0 (RC1) introduces the next platform contract for building OCI-native services on Helidon 4. This presentation covers the release candidate's supported JDK and OCI Java SDK baseline, BOM-managed module set, OCI client integrations, shared OCI SDK authentication, environment configuration for region and location discovery, and operational modules for Identity, Secret Service, Metrics, Metering, Audit, Request ID, Error Code, SPLAT, Kiev, Workflow, Limits, Tagging, and Object Storage Client.

The release aligns Helidon Talon with the Helidon 4 programming model: a Helidon SE-first approach, compile-time injection through the Helidon service registry, generated configuration metadata, and a high-performance runtime designed around straightforward blocking code on modern Java virtual threads. This makes async and reactive plumbing a thing of the past, letting teams write request handling logic that is direct, readable, and debuggable while still relying on Helidon infrastructure for routing, configuration, lifecycle, and observability.

The session focuses on how teams can compose these modules to build Helidon services with less repeated infrastructure wiring and a consistent model for configuration, authentication, service discovery, and documentation.

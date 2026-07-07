# Helidon Talon 2.0.0 (RC2) Presentation Abstract

Helidon Talon 2.0.0 (RC2) advances the platform contract for building OCI-native services on Helidon 4.5.1-M1. This presentation covers the release candidate's JDK 25 and OCI Java SDK baseline, 30 BOM-managed artifacts, OCI client integrations, environment configuration for region and location discovery, and operational modules for Identity, Secret Service, Metrics, Metering, Audit, Request ID, Error Code, SPLAT, Kiev, Workflow, Limits, Tagging, and Object Storage Client.

The presentation begins with a concise introduction to Helidon as a modern, open-source Java framework for cloud-native services. It explains how Helidon 4 uses virtual threads to deliver high concurrency while preserving direct, readable blocking code, then shows how Helidon Talon builds OCI-specific integrations on that foundation through compile-time injection, generated configuration metadata, routing, lifecycle, and observability.

RC2 adds production hardening and clearer operational contracts. The session explains default versus explicit environment-config bootstrap, global and integration-scoped service-principal authentication, scoped Limits authentication, Kiev streaming and explicit S2S certificates, automatic HTTP and JVM metrics with filtering, hardened Audit and Secret Service behavior, and the requirement to expose SPLAT-protected endpoints only on listeners that enforce mTLS.

The session focuses on how teams can compose these modules to build Helidon services with less repeated infrastructure wiring and a consistent model for configuration, authentication, service discovery, observability, and documentation. It also points to the 12 runnable examples as executable configuration references for RC2 adoption.

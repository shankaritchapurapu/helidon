# Helidon OCI SDK Common Client

This module, `helidon-oci-sdk-common-client`, provides shared Helidon blueprint/config support for commonly used OCI SDK
client runtime configuration types.

It owns the `Oci*` helper prototypes for OCI SDK client, retry, retry options, delay strategy, termination strategy, retry condition, and circuit breaker configuration. These helpers are config-friendly Helidon types, but their `build()` methods produce the corresponding OCI SDK runtime objects.

Adopting modules should expose OCI SDK runtime types such as `ClientConfiguration` on containing blueprints and use `ConfigSupport` custom methods to map nested Helidon config into those runtime objects. Service-specific settings, defaults, endpoints, and compatibility behavior should remain in the adopting module.

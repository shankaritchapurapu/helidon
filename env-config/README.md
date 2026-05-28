# Environment Config

---

## Overview

This module provides the `oci-env` Helidon config source and companion declarative region
providers. It publishes derived OCI environment values for Helidon configuration, supports
declarative bootstrap and explicit meta-config usage, reads optional `helidon.oci-env` settings
from `oci-config.yaml`, and supports location overrides and dynamic core-regions import.
When `oci-env` is enabled explicitly from `meta-config.*`, source `properties` are merged over
`helidon.oci-env` from `oci-config.yaml` so the file can fill missing provider settings.

For declarative service-registry bootstrap, the module can provide both the public OCI SDK
`com.oracle.bmc.Region` and the PIC commons `com.oracle.pic.commons.util.Region` from the same
lazy `oci-env` location resolution.

For more details, please check [Environment Config](../docs/utilities/environment-config.md)
under [Helidon-OCI Native Services Integration Guide](../docs/README.md).

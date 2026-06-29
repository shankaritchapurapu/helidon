# Helidon Limits Example

---

This example shows how to use limits.

## Build

```shell
mvn package
```

## Run

To run the client:


```shell
java -jar ./target/helidon-oci-examples-limits.jar
```

The example selects service-principal authentication from `src/main/resources/application.yaml`.
Run it on OCI infrastructure with platform-provided service-principal material available, such as
IDMS/ODO/OMK metadata, through an IMDS-compatible tunnel, or add explicit service-principal
certificate settings to the same `oci.limits.auth` section. The default OCI config file at
`~/.oci/config` or `~/.oraclebmc/config` only supplies common OCI settings for this example.

You can override the path setting the environment variable `OCI_CONFIG_FILE` with a custom path.

## Overview

For more details, please check [Limits](../../docs/services/limits.md) under
[Helidon-OCI Native Services Integration Guide](../../docs/README.md).

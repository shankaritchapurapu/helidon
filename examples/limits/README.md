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

That will load default oci configuration located in `~/.oci/config` or `~/.oraclebmc/config`

You can override the path setting the environment variable `OCI_CONFIG_FILE` with a custom path.

## Overview

For more details, please check [Limits](../../docs/limits.md) under [Helidon-OCI Native Services Integration Guide](../../docs/README.md).

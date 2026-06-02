# OCI SDK Authentication Integration Test

## Overview

This module contains tests related to integration with authentication mechanisms provided by OCI SDK.

## Modules

- `instance-principal`: integration tests for instance-principal authentication details providers.
- `service-principal`: integration tests for service-principal authentication details providers.

## Prerequisites

Please see [How to locally test your OCI SDK Integration](../../../README.md) to set up ssh tunneling for the Instance Metadata Service.

## Steps

1. Run the SSH tunnel command to open up a connection to the remote host and forward any connection on local port 8000 to the
   Instance MetaData Service endpoint (169.254.169.254:80) of the remote host. Note that this example uses `ztb-api-ad1` host
   alias that should have been set up in the OSSH config.
   ```shell
   $ ssh -v -L 8000:169.254.169.254:80 oci-reference-service-ad1 -t watch -n 90 date
   ```
2. Run the integration tests using `STTest` profile, where `ST` stands for `SSH Tunneling`.
   ```shell
   $ mvn clean install -PSTTest
   ```

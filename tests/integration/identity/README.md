# OCI Identity Service Integration Test

## Overview

This module contains tests related to integration with OCI Identity service as provided by OCI AuthSDK. The test
endpoint uses method-level `@AuthorizationPermission` annotations and direct `Principal` method-parameter injection, matching
the current identity integration style.

## Prerequisites

Please see [How to locally test your OCI SDK Integration](../../../README.md) to set up ssh tunneling for the Instance Metadata Service.

## Steps

1. Run the SSH tunnel command to open up a connection to the remote host and forward any connection on local port 8000 to the
   Instance MetaData Service endpoint (169.254.169.254:80) of the remote host. Note that this example uses `ztb-api-ad1` host
   alias that should have been set up in the OSSH config.
   ```shell
   $ ssh -v -L 8000:169.254.169.254:80 helidon-oci-test-instance-1 -t watch -n 90 date
   ```
2. Copy the certificate `/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem` from `helidon-oci-test-instance-1` node locally at same location.
3. Run the unit test using `STTest`profile, where `ST` stands for `SSH Tunneling`.
   ```shell
   $ mvn clean verify -PSTTest
   ```

# Secret Service V2 Integration Test

## Overview

This module starts a Helidon SE WebServer with a declarative HTTP endpoint and
verifies that the endpoint can read a Secret Service V2-backed Helidon config
value.

## Prerequisites

The test uses instance principal authentication. Please see
[How to locally test your OCI SDK Integration](../../../README.md) to set up
SSH tunneling for the Instance Metadata Service when running locally.

The endpoint reads `/secret/helidon/helidon-path/latest` through the
`oci.ssv2` config-source prefix. The expected secret value is
`Helidon secret value`.

## Steps

1. Run the SSH tunnel command to forward local port 8000 to the remote Instance
   Metadata Service endpoint.
   ```shell
   $ ssh -v -L 8000:169.254.169.254:80 oci-reference-service-ad1 -t watch -n 90 date
   ```
2. Run the integration test using the `STTest` profile, where `ST` stands for
   `SSH Tunneling`.
   ```shell
   $ mvn -pl tests/integration/secret-service -am -Pintegration-tests -PSTTest verify
   ```

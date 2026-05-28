# Secret Service V2 Integration Test

## Overview

This module starts a Helidon SE WebServer with a declarative HTTP endpoint and
verifies that the endpoint can read a Secret Service V2-backed Helidon config
value.

## Prerequisites

The test uses instance principal authentication and must run from an OCI
environment that can reach both the Instance Metadata Service and Secret Service
V2.

The endpoint reads `/secret/helidon/helidon-path/latest` through the
`oci.ssv2` config-source prefix. The expected secret value is
`Helidon secret value`.

## Steps

1. Run the integration test from the OCI environment.
   ```shell
   $ mvn -pl tests/integration/secret-service -am -Pintegration-tests verify
   ```

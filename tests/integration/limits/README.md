# Limits Integration Test

## Overview

This module verifies the Helidon OCI Limits integration against the real OCI Limits DP service.
The test resolves `LimitsDPClient` from the Helidon service registry and calls Limits DP APIs, which validates
authentication, endpoint resolution, and basic service reachability without requiring a fixed private fixture.

The test checks:

- `getServiceLimitServiceGroups`, using the visible service group list
- `getServiceLimits`, using the public `database` service group

## Prerequisites

Run this test from an OCI test instance. The instance must have:

- access to the Instance Metadata Service at `http://169.254.169.254/opc/v2/`
- service-principal authentication enabled for the instance
- network access to the OCI Limits DP endpoint for the instance region
- IAM permission for the service principal to call the Limits DP service

The test uses `src/test/resources/oci-config.yaml` with `helidon.oci.authentication-method: "service-principal"`.

# Integration Tests

## Overview
Integration tests for common modules.

## Configuration
Create a submodule named `<module>` for each module under tests/integration.

## Usage

## Build and run example
### Build & Run

```shell
mvn clean package
```

## Creating a test module that uses SSH Tunneling
Using SSH Tunnel as an approach for performing OCI native integration tests helps promote fast iteration to code changes. Check out [oci-t2-metrics-test readme](oci-t2-metrics-test/README.md) for more details on how to use [oci-t2-metrics-test](oci-t2-metrics-test) module as an example for creating a test that uses SSH Tunneling.

# Kiev Integration Test

## Overview

This module contains the integration test coverage for the Helidon Kiev integration in `tests/integration/kiev`.
It validates that a Kiev-backed service can be resolved from the Helidon service registry and used in a real test run.

## Module Structure

- `src/main/java/com/oracle/helidon/oci/tests/integration/kiev/KievStoreService.java`
  - Small Kiev service used by the integration test.
  - Uses `MappedDataStore` and `@Kiev.Transaction` methods for CRUD operations.
- `src/main/java/com/oracle/helidon/oci/tests/integration/kiev/StoreItem.java`
  - Kiev mapped entity used by `KievStoreService`.
- `src/test/java/com/oracle/helidon/oci/tests/integration/kiev/KievIT.java`
  - `@ServerTest` integration test class.
  - Resolves `KievStoreService` from Helidon `Services` and verifies basic CRUD behavior.
- `src/test/resources/application.yaml`
  - Kiev runtime configuration used during test execution.

## Current Test Coverage

`KievIT` runs ordered service lifecycle coverage:

- `cleanupExistingData`
  - Resolves `KievStoreService`, which creates the bucket if it is missing.
  - Deletes only the rows for the current test run's generated UUID key prefix and verifies those rows are absent.
- `testPostItems`, `testGetItem`, `testPutItem`, `testListItems`, `testDeleteItem`
  - Exercises the Kiev-backed service methods for create, read, update, list, and delete operations.
  - Filters list assertions to rows created by the current test run so unrelated bucket contents are ignored.
- `cleanupData`
  - Deletes rows created during the test run and verifies they are no longer readable.

## Configuration Notes

The test configuration currently targets the `SERVICE` backend (`oci.kiev.data-stores[].backend: "SERVICE"`).
Before running this module, ensure values in `src/test/resources/application.yaml` are valid for your test environment
(store name, compartment, endpoint, and auth-related configuration).

### Certificate Bundle

The test uses instance-principal authentication against a KaaS frontend. That single client flow must trust both:

- the private OCI PKI chain used by the Kiev frontend endpoint, for example
  `*.kiev.us-phoenix-1.oci.oracleiaas.com`
- the public/JVM-trusted chain used by the OCI auth endpoint for instance-principal federation, for example
  `https://auth.us-phoenix-1.oraclecloud.com/v1/x509`

The Kiev client treats `oci.kiev.data-stores[].service.auth.tls.root-cert-pem-path` as the trust bundle for this flow.
Using only `/etc/oci-pki/ca-bundle.pem` can trust the Kiev frontend but fail the OCI auth call with
`PKIX path building failed`; using only the JVM defaults can trust OCI auth but miss the private Kiev roots. The circuit
breaker errors that follow those SSL failures are secondary symptoms.

The current test configuration points `root-cert-pem-path` directly at:

```yaml
/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem
```

The Maven module does not generate or filter a combined CA bundle. The machine or container running the test must provide
that file, and it must include the public roots needed for OCI auth and the private OCI PKI roots needed for the Kiev
frontend. The pipeline test stage mounts this host file into the container at the same path. It also mounts
`/etc/oci-pki/ca-bundle.pem`, but the Kiev test does not currently point at that file.

If your environment has the required trust material in a different location, update
`src/test/resources/application.yaml` before running the test, or provide a mounted file at the configured path. If the
configured bundle does not contain both trust chains, the test may fail either during instance-principal federation or
when connecting to the private Kiev frontend.

The test uses `auth.type: "INSTANCE"` and relies on instance-principal authentication through IMDS. There is no
Kiev-specific `oci-config.yaml` in this module. When running locally through an SSH tunnel, use the `STTest` profile from
the integration parent so IMDS requests are directed to `localhost:8000`. When running on an OCI instance, do not use
`STTest`; the test should use the instance's real IMDS endpoint.

## Running the Test

Run only the Kiev integration module from the repository root:

```bash
mvn -f tests/integration/pom.xml -pl kiev verify
```

Run through the local SSH tunnel profile:

```bash
mvn -f tests/integration/pom.xml -pl kiev -PSTTest verify
```

Run through the top-level integration test profile:

```bash
mvn -Pintegration-tests -pl tests/integration/kiev -am verify
```

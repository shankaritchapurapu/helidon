# Helidon Workflow Example

---

This example shows how to use the Helidon OCI WFaaS integration through a small HTTP service that launches a
workflow and polls for status updates.

## Prerequisites

- JDK 21
- Maven
- Access to a WFaaS endpoint for the real integration test path

## Build

From `examples/workflow`:

```shell
mvn package
```

## Run

By default the example starts an HTTP service and uses the WFaaS connection details from
`src/main/resources/application.yaml`.

From `examples/workflow`:

```shell
java -jar ./target/helidon-oci-examples-workflow.jar
```

The service exposes two JSON routes:

- `POST /workflow/instances` with a JSON body containing `resourceId` and optional `simulateFailure`
- `GET /workflow/instances/{id}` to fetch the latest workflow snapshot

The JSON response contains `workflowInstanceId`, `status`, and `tag`.

Example request:

```shell
curl -X POST http://localhost:8080/workflow/instances \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{"resourceId":"ocid1.instance.oc1..example","simulateFailure":false}'
```

## Test

Run the fast example tests:

From `examples/workflow`:

```shell
mvn test
```

These tests use a driver-style fake `WorkflowClient` to exercise the REST contract without requiring a WFaaS
environment.

## WFaaS Integration Test

The integration test profile in `src/test/resources/application-it.yaml` is intended for a real or locally
reachable WFaaS environment. Update the WFaaS endpoint, domain, and authentication-related settings to match
your environment before enabling the test.

From the repository root:

```shell
mvn -pl examples/workflow -am verify -DskipITs=false -Dit.test=WorkflowEndpointIT
```

The integration test launches a workflow through the sample REST service, captures the returned workflow
instance id, and then fetches the latest workflow snapshot over HTTP.

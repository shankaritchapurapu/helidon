# Helidon Audit Example

---

This example shows how to enable the Helidon OCI audit feature in a small HTTP service.

## Prerequisites

- JDK 25
- Maven

## Build

From `examples/audit`:

```shell
mvn package
```

## Run

From `examples/audit`:

```shell
java -jar ./target/helidon-oci-examples-audit.jar
```

The service exposes two JSON routes:

- `GET /audit/orders?orderId=...&expand=...`
- `POST /audit/orders/approve?orderId=...` with a plain-text approver name in the body

The example endpoint injects `AuditPayloadAppender` directly into each route method and enriches the current
request's audit event with an event name, tenant, compartment, resource id, and resource RIO.

The example config in `src/main/resources/application.yaml` sets default tenant, compartment, and resource fields
for fallback events and whitelists:

- request parameters `orderId` and `expand`
- request headers `opc-request-id` and `x-audit-example-tenant`
- response header `x-audit-example-version`

To verify the audit filter ran without inspecting logs, send the `oci-splat-audit-verify: true` header. The
response will then include `oci-splat-audit-event-summary`.

Example request:

```shell
curl -i 'http://localhost:8080/audit/orders?orderId=order-123&expand=details' \
  -H 'Accept: application/json' \
  -H 'opc-request-id: customer123/trace123' \
  -H 'x-audit-example-tenant: ocid1.tenancy.oc1..aaaaaaaahelidonauditexample' \
  -H 'oci-splat-audit-verify: true'
```

To demonstrate opt-out behavior, send `oci-splat-audited: true`. With the default config in this example, that
suppresses audit processing for the request.

## Test

From `examples/audit`:

```shell
mvn test
```

## Overview

For more details, please check [Audit](../../docs/audit.md) under
[Helidon-OCI Native Services Integration Guide](../../docs/README.md).

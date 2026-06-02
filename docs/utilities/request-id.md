# Request ID

---

## Overview

The Request ID integration provides OCI `opc-request-id` handling for Helidon applications.

When `helidon-oci-request-id-webserver` is on the classpath, the request filter:

* reads an incoming `opc-request-id` header when the client sends one
* generates a new request ID when the header is absent
* writes the upstream request ID back to the response header
* stores `OciRequestId` in the Helidon request context
* adds the upstream request ID to MDC under `opc-request-id`

This makes the request ID available both for logging and for endpoint logic.

---

## Maven Coordinates

Add the webserver integration to your project:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.requestid</groupId>
    <artifactId>helidon-oci-request-id-webserver</artifactId>
</dependency>
```

If you only need the `OciRequestId` API without the webserver integration, use:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.requestid</groupId>
    <artifactId>helidon-oci-request-id</artifactId>
</dependency>
```

---

## Usage

When the Helidon service registry is enabled, the request-id webserver feature is loaded automatically from the module dependency. No additional configuration is required.

### Response Header

Every handled request receives an `opc-request-id` response header. If the client already sent one, the response uses the normalized upstream value. Otherwise a new one is generated.

### Accessing `OciRequestId`

`OciRequestId` is request-scoped and is registered into `request.context()`.

For declarative REST endpoints, the preferred approach is to inject it directly as a method parameter:

```java
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.requestid.OciRequestId;

@RestServer.Endpoint
@Http.Path("/request-id")
@Service.Singleton
class RequestIdEndpoint {

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String get(OciRequestId requestId) {
        return requestId.upstreamHeaderValue();
    }
}
```

If you need request-scoped access from an injected singleton field or constructor dependency, you can inject `Supplier<OciRequestId>`:

```java
import java.util.function.Supplier;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.requestid.OciRequestId;

@RestServer.Endpoint
@Http.Path("/request-id")
@Service.Singleton
class RequestIdEndpoint {

    private final Supplier<OciRequestId> requestIdSupplier;

    @Service.Inject
    RequestIdEndpoint(Supplier<OciRequestId> requestIdSupplier) {
        this.requestIdSupplier = requestIdSupplier;
    }

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String get() {
        return requestIdSupplier.get().upstreamHeaderValue();
    }
}
```

For other services, or when you want explicit request-context access, use `ServerRequest`:

```java
import io.helidon.webserver.http.ServerRequest;

import com.oracle.helidon.oci.requestid.OciRequestId;

OciRequestId requestId = serverRequest.context().get(OciRequestId.class).orElseThrow();
```

`Supplier<OciRequestId>` is also available as a per-request service if you need to inject the request ID into another injected component.

---

## Example

The repository includes a runnable example in `examples/request-id`.

Build:

```shell
cd examples/request-id
mvn package
```

Run:

```shell
java -jar ./target/helidon-oci-examples-request-id.jar
```

Call the endpoint:

```shell
curl -i http://localhost:8080/request-id
```

Example response:

```text
HTTP/1.1 200 OK
content-type: text/plain
opc-request-id: /D9C34373C47EC15A5F85740A14898B98/D7D339B1429A3E27AA189FA6E8262321

upstream=/D9C34373C47EC15A5F85740A14898B98/D7D339B1429A3E27AA189FA6E8262321
customer=
trace=D9C34373C47EC15A5F85740A14898B98
span=D7D339B1429A3E27AA189FA6E8262321
```

---

## References

* [Request ID example](../../examples/request-id/src/main/java/com/oracle/helidon/oci/examples/requestid/RequestIdEndpoint.java)
* [Request IDs](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Request+IDs)

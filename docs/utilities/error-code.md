# Error Code

---

## Overview

The Error Code integration provides OCI-style error response handling for Helidon services. It follows the model
described in
[OCI Error Codes](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Error+Codes) as part of the [API Consistency Guidelines](https://confluence.oci.oraclecorp.com/display/DEX/API+Consistency+Guidelines).

The integration provides:

* `ErrorCodes`, an enum for the common OCI error codes and their HTTP statuses
* `RenderableException`, a runtime exception services can throw from endpoint logic
* `ErrorDetail`, the JSON-serializable response body used by servers and clients
* a Helidon WebServer feature that maps `RenderableException` to a JSON response automatically

---

## Maven Coordinates

Add the webserver integration to your application:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.errorcode</groupId>
    <artifactId>helidon-oci-errorcode-webserver</artifactId>
    <scope>runtime</scope>
</dependency>
```

If you only need the shared API types without the webserver mapper, use:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.errorcode</groupId>
    <artifactId>helidon-oci-errorcode</artifactId>
</dependency>
```

---

## Example Application

The [data-plane reference example](../../examples/data-plane/) demonstrates reporting business failures with
`RenderableException` and OCI-style error response payloads.

---

## Usage

### Helidon WebServer

When `helidon-oci-errorcode-webserver` is on the classpath, the Helidon service loader registers
the `oci-error-code` server feature and enables it by default. The feature installs an error
handler for `RenderableException` on the default socket and any configured named sockets.

To disable it, set:

```yaml
server:
  features:
    oci-error-code:
      enabled: false
```

To report an OCI-style error, throw a `RenderableException` from endpoint logic:

```java
import java.util.Map;

import com.oracle.helidon.oci.errorcode.ErrorCodes;
import com.oracle.helidon.oci.errorcode.RenderableException;

if (notAuthenticated()) {
    throw new RenderableException(
            ErrorCodes.NotAuthenticated,
            "User 'helidon' is not authenticated",
            "Authentication failure for 'helidon'",
            "Authentication failure for '{user}'",
            Map.of("user", "helidon"));
}
```

The webserver feature converts the exception to a response using the status associated with the
error code and sends the exception's `ErrorDetail` entity with `application/json` content type.

For the example above, the response status is `401 Unauthorized` and the response body contains:

```json
{
  "code": "NotAuthenticated",
  "message": "User 'helidon' is not authenticated",
  "originalMessage": "Authentication failure for 'helidon'",
  "originalMessageTemplate": "Authentication failure for '{user}'",
  "messageArguments": {
    "user": "helidon"
  }
}
```

For simpler cases, use the default message for the code:

```java
throw new RenderableException(ErrorCodes.MissingParameter);
```

Or provide a service-specific message:

```java
throw new RenderableException(ErrorCodes.InvalidParameter, "Invalid shape value");
```

`RenderableException` also supports `String.format`-style messages and constructors with a cause.
Use the full constructor with `originalMessage`, `originalMessageTemplate`, and `messageArguments`
when callers need the OCI error payload to preserve the original templated message.

### Error Codes

`ErrorCodes` includes the common OCI error codes and maps each one to the HTTP status the server
feature uses. Examples include:

| Error code | HTTP status |
|------------|-------------|
| `InvalidParameter` | `400 Bad Request` |
| `MissingParameter` | `400 Bad Request` |
| `NotAuthenticated` | `401 Unauthorized` |
| `NotAuthorizedOrNotFound` | `404 Not Found` |
| `NoEtagMatch` | `412 Precondition Failed` |
| `Conflict` | `409 Conflict` |
| `TooManyRequests` | `429 Too Many Requests` |
| `InternalError` | `500 Internal Server Error` |
| `ExternalServerTimeout` | `503 Service Unavailable` |

If a service needs an error code that is not in the enum, implement `ErrorCode` and pass that
implementation to `RenderableException`.

The default mapper serializes the existing `ErrorDetail` model. If a service uses
its own response envelope, it can still catch `RenderableException` and build that
custom envelope itself.

### Helidon WebClient

Clients can read OCI-style error responses as `ErrorDetail`:

```java
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.http1.Http1Client;

import com.oracle.helidon.oci.errorcode.ErrorCode;
import com.oracle.helidon.oci.errorcode.ErrorDetail;
import com.oracle.helidon.oci.errorcode.ErrorCodes;

Http1Client client = ...;
ClientResponseTyped<ErrorDetail> response = client.get("/resource")
        .request(ErrorDetail.class);

if (response.status().code() >= 400) {
    ErrorDetail detail = response.entity();
    ErrorCode code = ErrorCode.create(response.status(), detail);

    if (code == ErrorCodes.InvalidParameter) {
        // Handle invalid input.
    }
}
```

`ErrorDetail` supports both JSON-B and Jackson deserialization. Its JSON properties are:

| Property | Description |
|----------|-------------|
| `code` | OCI error code, usually one of the `ErrorCodes` names. |
| `message` | User-facing error message. |
| `originalMessage` | Optional original message value. |
| `originalMessageTemplate` | Optional original template used to produce the message. |
| `messageArguments` | Optional template argument map. |

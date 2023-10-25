# Error Code

## Overview
This module provides support for error codes described in
in [OCI Error Codes](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Error+Codes).

## Prerequisites
- JDK 17 or higher
- Maven 3.6.1 or higher

## Configuration

Just include a dependency to this module in your pom file as shown below. All relevant
providers will be automatically loaded into your application.

```
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-error-code</artifactId>
    <version>...</version>
</depenency>
```

## Usage

### Jakarta REST Server (Helidon MP)

To report an error, simply create and throw a `RenderableException`. 
This module registers an exception mapper that automatically
converts this exception to a `Response`. For example,

```java
    if (notAuthenticated()) {
        throw new RenderableException(null,
            ErrorCode.NotAuthenticated,
            "User 'helidon' is not authenticated",
            "Authentication failure for 'helidon'",
            "Authentication failure for '{user}'",
            Map.of("user","helidon"));
    }
```

will result in a `Response` with a JSON payload sent back to the client
following the schema described in the document linked above.

### Jakarta REST Client API

Error responses can be processed by a Jakarta REST client as follows:

```java
    WebTarget webTarget = ...;
    Response response = webTarget.path("error").request().get();
    if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
        ErrorDetail errorDetail = response.readEntity(ErrorDetail.class);
        processErrorDetail(errorDetail);
    }
```

The class `ErrorDetail` is used to read the entity payload in the error
response.

### Microprofile RestClient API

When using the Microprofile RestClient API, it is possible to map an
error response back to an exception that can be caught client side. This
feature requires an explicit registration of the `ErrorCodeResponseMapper` 
RestClient provider as part of the interface definition as shown next:

```java
    @Path("/")
    @RegisterProvider(ErrorCodeResponseMapper.class)   // maps to RenderableException
    public interface TestResoureClient {

        @GET
        @Path("error")
        Response error() throws RenderableException;
    }
```

With the registration of this provider, a client request can be executed
as follows:

```java
    TestResoureClient client = RestClientBuilder.newBuilder()
                                                .baseUri(webTarget.getUri())
                                                .build(TestResoureClient.class);
    try (Response response = client.error2()) {
        processResponse(response);
    } catch (RenderableException e) {
        handleException(e);
    }
```

Note that the provider will attempt to map any response whose HTTP error code
is greater or equal to 400; thus, if a response is returned with such an
error code but whose entity is not an `ErrorDetail`, an exception will
be thrown while attempting to read the entity--this is the primary reason 
why the API requires explicit registration of `ErrorCodeResponseMapper`.

### Build & Run

```shell
mvn clean package
```
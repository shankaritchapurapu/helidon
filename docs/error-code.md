# error-code

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)

---

## Overview

This module provides support for error codes described in
in [OCI Error Codes](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Error+Codes) as part of the [API Consistency Guidelines](https://confluence.oci.oraclecorp.com/display/DEX/API+Consistency+Guidelines).

---

## Maven Coordinates

To enable the error-code module, add the following dependency to your Helidon SE project’s pom.xml:

```
<dependency>
    <groupId>com.oracle.helidon.oci.errorcode</groupId>
    <artifactId>helidon-oci-error-code-webserver</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage
### Helidon WebServer

To report an error, simply create and throw a `RenderableException`.
Module included in your pom file based on the Helidon flavor as shown [above](#maven-coordinates), registers an exception 
mapper that automatically converts this exception to a `ServerResponse`. For example,

```java
    if (notAuthenticated()) {
        throw new RenderableException(
            ErrorCode.NotAuthenticated,
            "User 'helidon' is not authenticated",
            "Authentication failure for 'helidon'",
            "Authentication failure for '{user}'",
            Map.of("user","helidon"));
    }
```

will result in a `ServerResponse` with a JSON payload sent back to the client
following the schema described in the document linked above.

### Helidon WebClient

Error responses can be processed by a Helidon WebClient as follows:

```java
    Http1Client client = ...;
    ClientResponseTyped<ErrorDetail> response = client.get("/error1")
        .request(ErrorDetail.class);
    if (response.status() == ErrorCodes.InvalidParameter.status()) {
        ErrorDetail errorDetail = response.entity();
    }
```

The class `ErrorDetail` is used to read the entity payload in the error
response.

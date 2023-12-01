# Identity

## Overview
This module provides support for OCI native identity, authentication, and authorization.

## Configuration
Start by including a dependency to this module in your pom file as shown below. All relevant
providers will be automatically loaded into your application.

```
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-identity</artifactId>
    <version>...</version>
</depenency>
```

Next, optionally configure the provider. This typically is placed in your <i>microprofile-config.properties</i>.

| config key                       | Default Value        | Description                                                                                                            | See                                                                                                                 |
|----------------------------------|----------------------|------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| oci-identity.uriPrefix(.0..n)    | /                    | List of URI path prefixes that will trigger a SHA-256 digest header (see headerTag) to be created on the server side   | [AuthenticationSupportingFilter](src/main/java/com/oracle/helidon/oci/identity/AuthenticationSupportingFilter.java) |
| oci-identity.headerTag           | X-HELIDON-DIGEST     | The header tag header key name that will be used.                                                                      | [AuthenticationSupportingFilter](src/main/java/com/oracle/helidon/oci/identity/AuthenticationSupportingFilter.java) |
| oci-identity.memoryThreshold     | 8 K                  | The in-memory buffer size. Anything after this value will be streamed to offline file storage.                         | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.streamThreshold     | 256 MB               | The threshold length of the stream permitted to be offlined on temp file storage before a runtime exception is thrown. | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.useFilesystem       | true                 | Flag indicating whether offline should be enabled. Note: will use temp files that are auto deleted.                    | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.useEncryption       | false                | Flag indicating whether offline storage should be encrypted.                                                           | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.encryptionAlgorithm | AES                  | The encryption algorithm to use for offline encrypted storage.                                                         | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.encryptionCipher    | AES/CBC/PKCS5Padding | The encryption cipher to use for offline encrypted storage.                                                            | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |

## Usage

## Build and run example
### Build & Run

```shell
mvn clean package
```

## References
* [Helidon Security Extensibility](https://helidon.io/docs/v2/#/se/security/05_extensibility)

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

| config key                                         | Default Value                             | Description                                                                                                            | See                                                                                                                 |
|----------------------------------------------------|-------------------------------------------|------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| oci-app.name                                       | xxxx                                      | The application name.                                                                                                  | [OciIdentityConfiguration.AppConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)   |
| oci-app.teamName                                   | x-team                                    | The application team name.                                                                                             | [OciIdentityConfiguration.AppConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)   |
| oci-app.teamName                                   | GBU                                       | The global business unit name.                                                                                         | [OciIdentityConfiguration.AppConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)   |
| oci-app.stage                                      | DEVELOPMENT                               | The application lifecycle stage (DEVELOPMENT, TEST, PRODUCTION).                                                       | [OciIdentityConfiguration.AppConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)   |
| oci-identity.authEnabled                           | false                                     | Flag indicating whether AuthN is enabled. When disabled an offline AuthN authenticator will be returned.               | [OciIdentityConfiguration.AuthConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)  |
| oci-identity.metricsEnabled                        | false                                     | Flag indicating whether AuthN metrics are enabled. This is only applicable when authN is enabled.                      | [OciIdentityConfiguration.AuthConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)  |
| oci-identity.rootCertPath                          | /etc/oci-pki/ca-bundle.pem                | The root certificate path.                                                                                             | [OciIdentityConfiguration.AuthConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)  |
| oci-identity.metadataEndpoint                      | http://localhost:8080                     | The metadata service endpoint. Normally in stages other than DEVELOPMENT this would be set to http://169.254.169.254   | [OciIdentityConfiguration.AuthConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)  |
| oci-identity.authServiceEndpoint                   | https://auth.us-phoenix-1.oraclecloud.com | The auth service endpoint.                                                                                             | [OciIdentityConfiguration.AuthConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)  |
| oci-identity.refreshAuthTokenBeforeSecondsToExpire | 5                                         | The seconds before token expiry to refresh the token.                                                                  | [OciIdentityConfiguration.AuthConfig](src/main/java/com/oracle/helidon/oci/identity/OciIdentityConfiguration.java)  |
| oci-identity.uriPrefix(.0..n)                      | /                                         | List of URI path prefixes that will trigger a SHA-256 digest header (see headerTag) to be created on the server side   | [AuthenticationSupportingFilter](src/main/java/com/oracle/helidon/oci/identity/AuthenticationSupportingFilter.java) |
| oci-identity.headerTag                             | X-HELIDON-SHA-DIGEST                      | The header tag header key name that will be used.                                                                      | [AuthenticationSupportingFilter](src/main/java/com/oracle/helidon/oci/identity/AuthenticationSupportingFilter.java) |
| oci-identity.memoryThreshold                       | 8 K                                       | The in-memory buffer size. Anything after this value will be streamed to offline file storage.                         | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.streamThreshold                       | 256 MB                                    | The threshold length of the stream permitted to be offlined on temp file storage before a runtime exception is thrown. | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.useFilesystem                         | true                                      | Flag indicating whether offline should be enabled. Note: will use temp files that are auto deleted.                    | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.useEncryption                         | false                                     | Flag indicating whether offline storage should be encrypted.                                                           | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.encryptionAlgorithm                   | AES                                       | The encryption algorithm to use for offline encrypted storage.                                                         | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |
| oci-identity.encryptionCipher                      | AES/CBC/PKCS5Padding                      | The encryption cipher to use for offline encrypted storage.                                                            | [RepeatableInputStreamer](src/main/java/com/oracle/helidon/oci/identity/RepeatableInputStreamer.java)               |

## Usage

## Build and run example
### Build & Run

```shell
mvn clean package
```

## References
* [Helidon Security Extensibility](https://helidon.io/docs/v2/#/se/security/05_extensibility)
* [Creating a CA and self-signed Cert](self-singed-cert-readme.md)

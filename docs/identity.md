# Identity

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

---
## Overview
This module provides support for OCI native identity, authentication, and authorization.

## Maven Coordinates
Start by including a dependency to this module in your pom file as shown below. All relevant
providers will be automatically loaded into your application.

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-identity</artifactId>
</depenency>
```

## Usage

Helidon provides its own version of the [Identity SDK](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/identity/config-for-identity-service/integrating-your-service-with-identity-auth-sdk.htm) to make its usage with Jakartified JAX-RS endpoints and latest public OCI SDK possible.
Usage is very similar as with Dropwizard with the main difference being that Auth JAX-RS filters are set over Helidon configuration.


Example of configuration where `FILTER_CONFIG_OPTION` is a placeholder for identity auth JAX-RS filter:
```properties
oci.identity.filters.FILTER_CONFIG_OPTION.paths.path.0=/v1/cars
```

Posible values for `FILTER_CONFIG_OPTION`:

| Identity Filter(FILTER_CONFIG_OPTION)    | Auth SDK filter                                                                                                                                                                                                                                          |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| auth-context                             | [AuthContextRequestFilter](https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/sdk/src/main/java/com/oracle/pic/identity/authorization/sdk/AuthContextRequestFilter.java)                                                                                                                                                                                                                                 |
| backend-service-auth-context             | [BackendServiceAuthContextRequestFilter](https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/sdk/src/main/java/com/oracle/pic/identity/authorization/sdk/BackendServiceAuthContextRequestFilter.java)                                                                                                                                                                                                                   |
| oauth-context                            | [OauthContextRequestFilter](https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/sdk/src/main/java/com/oracle/pic/identity/authorization/sdk/OauthContextRequestFilter.java)                                                                                                                                                                                                                                |
| splat-authorization-verification         | [SplatAuthorizationVerificationFilter](https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/sdk/src/main/java/com/oracle/pic/identity/authorization/sdk/SplatAuthorizationVerificationFilter.java)                                                                                                                                                                                                                     |
| splat-aware-auth-context                 | [SplatAwareAuthContextRequestFilter](https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/sdk/src/main/java/com/oracle/pic/identity/authorization/sdk/SplatAwareAuthContextRequestFilter.java)                                                                                                                                                                                                                       |
| splat-aware-backend-service-auth-context | [SplatAwareBackendServiceAuthContextRequestFilter](https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/sdk/src/main/java/com/oracle/pic/identity/authorization/sdk/SplatAwareBackendServiceAuthContextRequestFilter.java) |


Example of AuthContextRequestFilter usage(configured with `auth-context`) in <i>META-INF/microprofile-config.properties</i>:
```properties
oci.identity.filters.auth-context.paths.path.0=/v1/cars
oci.identity.filters.auth-context.paths.path.1=/v1/bikes
```

Example of AuthContextRequestFilter usage(configured with `auth-context`) in <i>application.yaml</i>:
```yaml
oci.identity:
  filters:
    auth-context:
      paths:
      - path: /v1/cars
      - path: /v1/bikes
```

Example of identity SDK API usage in Helidon JAX-RS resource:
```java
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.context.PrincipalContext;
import com.oracle.pic.identity.authorization.sdk.context.AuthorizationRequestContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PUT;

@Path("/v1")
@Consumes("application/json; charset=utf-8")
@Produces("application/json")
@ApplicationScoped
public class CarResource {

    private final Map<Integer, Car> cars = new ConcurrentHashMap<>();

    @PUT
    @Path("/cars")
    @Consumes("application/json; charset=utf-8")
    @Produces("application/json")
    @AuthorizationPermission("CAR_WRITE")
    public Car addCar(Car car,
                      @PrincipalContext Principal principal,
                      @AuthorizationRequestContext AuthorizationRequest authorizationRequest) {
        cars.put(car.getId(), car);
        return car;
    }
}
```

## Configuration
Next, optionally configure the provider using the Helidon microprofile configuration framework by which the ConfigSource defaults to
`microprofile-config.properties`. Alternatively, you can also use other ConfigSources such as `application.yaml`.

| config key                                         | Default Value                             | Description                                                                                                            |
|----------------------------------------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------- |
| oci.app.name                                       | xxxx                                      | The application name.                                                                                                  | 
| oci.app.teamName                                   | x-team                                    | The application team name.                                                                                             | 
| oci.app.teamName                                   | GBU                                       | The global business unit name.                                                                                         | 
| oci.app.stage                                      | DEVELOPMENT                               | The application lifecycle stage (DEVELOPMENT, TEST, PRODUCTION).                                                       | 
| oci.identity.authEnabled                           | false                                     | Flag indicating whether AuthN is enabled. When disabled an offline AuthN authenticator will be returned.               | 
| oci.identity.metricsEnabled                        | false                                     | Flag indicating whether AuthN metrics are enabled. This is only applicable when authN is enabled.                      | 
| oci.identity.rootCertPath                          | /etc/oci-pki/ca-bundle.pem                | The root certificate path.                                                                                             | 
| oci.identity.metadataEndpoint                      | http://localhost:8080                     | The metadata service endpoint. Normally in stages other than DEVELOPMENT this would be set to http://169.254.169.254   | 
| oci.identity.authServiceEndpoint                   | https://auth.us-phoenix-1.oraclecloud.com | The auth service endpoint.                                                                                             | 
| oci.identity.refreshAuthTokenBeforeSecondsToExpire | 5                                         | The seconds before token expiry to refresh the token.                                                                  | 
| oci.identity.uriPrefix(.0..n)                      | /                                         | List of URI path prefixes that will trigger a SHA-256 digest header (see headerTag) to be created on the server side   | 
| oci.identity.headerTag                             | X-HELIDON-SHA-DIGEST                      | The header tag header key name that will be used.                                                                      | 
| oci.identity.memoryThreshold                       | 8 K                                       | The in-memory buffer size. Anything after this value will be streamed to offline file storage.                         | 
| oci.identity.streamThreshold                       | 256 MB                                    | The threshold length of the stream permitted to be offlined on temp file storage before a runtime exception is thrown. | 
| oci.identity.useFilesystem                         | true                                      | Flag indicating whether offline should be enabled. Note: will use temp files that are auto deleted.                    | 
| oci.identity.useEncryption                         | false                                     | Flag indicating whether offline storage should be encrypted.                                                           | 
| oci.identity.encryptionAlgorithm                   | AES                                       | The encryption algorithm to use for offline encrypted storage.                                                         | 
| oci.identity.encryptionCipher                      | AES/CBC/PKCS5Padding                      | The encryption cipher to use for offline encrypted storage.                                                            | 

## References
* [Helidon Security Extensibility](https://helidon.io/docs/v2/#/se/security/05_extensibility)
* [Creating a CA and self-signed Cert](../identity/self-singed-cert-readme.md)
* [Identity Auth SDK](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/identity/config-for-identity-service/integrating-your-service-with-identity-auth-sdk.htm)
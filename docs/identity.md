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

The Identity module integrates Helidon with the OCI Auth SDK. It provides configured service-registry factories for:

* `ServiceAuthenticationClient`
* `AuthenticatorClient`
* `Optional<IAuthorizationClient>`
* per-request `IdentityContext`

The configuration root is `oci.identity`. Authentication and authorization are configured independently under:

* `oci.identity.authentication`
* `oci.identity.authorization`

For request handling, authorization is applied through the Helidon OCI code generation integration. Methods annotated with OCI authorization annotations such as `@AuthorizationPermission` are intercepted automatically. The generated interceptor runs `AuthContextRequestFilter`, performs authentication and optional authorization, and registers an `IdentityContext` into the Helidon request context. This replaces the older filter-path configuration model and does not require configuring `oci.identity.filters.*`.

---

## Maven Coordinates

Add the Identity module dependency to your project:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.identity</groupId>
    <artifactId>helidon-oci-identity</artifactId>
</dependency>
```

---

## Usage

When the Helidon service registry is enabled, the module contributes the Identity services automatically. The generated authorization interceptor obtains `AuthenticatorClient` and `IAuthorizationClient` from the registry and applies them to intercepted endpoints.

The recommended way to access authenticated request data in a REST endpoint method is to declare an `IdentityContext` parameter directly. The generated handler resolves it from `request.context()` for each request.

```java
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.identity.IdentityContext;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;

import static com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter.PIC_PRINCIPAL;

@RestServer.Endpoint
@Http.Path("/echo")
@Service.Singleton
class EchoEndpoint {

    @Http.POST
    @Http.Path("once")
    @AuthorizationPermission("ECHO_ONCE")
    String once(@Http.Entity String message, IdentityContext identityContext) {
        Principal principal = (Principal) identityContext.get(PIC_PRINCIPAL);
        return principal.getSubjectId() + ": " + message;
    }
}
```

If you need request-scoped access from an injected singleton field or constructor dependency, inject `Supplier<IdentityContext>` instead:

```java
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.helidon.oci.identity.IdentityContext;

class AuditService {

    private final Supplier<IdentityContext> identityContextSupplier;

    @Service.Inject
    AuditService(Supplier<IdentityContext> identityContextSupplier) {
        this.identityContextSupplier = identityContextSupplier;
    }
}
```

If `oci.identity.authorization.enabled=false`, the authorization client is not created. Authentication still remains available through `ServiceAuthenticationClient` and `AuthenticatorClient`.

---

## Configuration

The root configuration is `oci.identity`.

### Authentication

Authentication config is loaded from `oci.identity.authentication`.

Minimal example using region-based endpoint resolution and instance principals:

```yaml
oci:
  identity:
    authentication:
      global-business-unit: "Cloud-Infra"
      team-name: "ExampleTeam"
      application-name: "ExampleService"
      region: "us-phoenix-1"
      use-instance-principal: true
```

Example using an explicit Auth endpoint:

```yaml
oci:
  identity:
    authentication:
      global-business-unit: "Cloud-Infra"
      team-name: "ExampleTeam"
      application-name: "ExampleService"
      service-uri: "https://auth.us-phoenix-1.oraclecloud.com"
      use-instance-principal: true
```

Example using explicit certificates instead of instance principals:

```yaml
oci:
  identity:
    authentication:
      global-business-unit: "Cloud-Infra"
      team-name: "ExampleTeam"
      application-name: "ExampleService"
      region: "us-phoenix-1"
      use-instance-principal: false
      certificates:
        - certificate: "/path/to/sp_cert.pem"
          private-key: "/path/to/sp_key.pem"
          passphrase: ""
      root-cert-path: "/etc/oci-pki/ca-bundle.pem"
```

| Key | Default Value | Description |
|-----|---------------|-------------|
| `oci.identity.authentication.service-uri` | | Explicit Auth service endpoint. Mutually exclusive with `region`. |
| `oci.identity.authentication.global-business-unit` | | Required global business unit passed to the Auth SDK. |
| `oci.identity.authentication.team-name` | | Required team name passed to the Auth SDK. |
| `oci.identity.authentication.application-name` | | Required application name passed to the Auth SDK. |
| `oci.identity.authentication.region` | | Region used to derive the Auth endpoint. Mutually exclusive with `service-uri`. |
| `oci.identity.authentication.use-instance-principal` | `true` | Whether to load certificates from instance metadata. |
| `oci.identity.authentication.instance-principal-uri` | | Optional override for the instance metadata endpoint. |
| `oci.identity.authentication.certificates` | `[]` | Explicit certificate list used when `use-instance-principal=false`. |
| `oci.identity.authentication.certificates[].certificate` | | Required certificate resource path. |
| `oci.identity.authentication.certificates[].private-key` | | Optional private key resource path. |
| `oci.identity.authentication.certificates[].passphrase` | `""` | Optional private key passphrase. |
| `oci.identity.authentication.root-cert-path` | | Optional CA bundle or root certificate path. |
| `oci.identity.authentication.metrics-lib` | | Optional Auth SDK metrics library name. |
| `oci.identity.authentication.hard-coded-key-supplier` | `false` | Uses the Auth SDK hard-coded key supplier, typically for development or test only. |

Authentication validation rules:

* Exactly one of `oci.identity.authentication.service-uri` or `oci.identity.authentication.region` must be configured.
* `global-business-unit`, `team-name`, and `application-name` are required.
* When `use-instance-principal=false` and `hard-coded-key-supplier=false`, at least one certificate entry must be configured.

### Authorization

Authorization config is loaded from `oci.identity.authorization`.

Example using a non-enclave overlay endpoint derived from region and physical AD:

```yaml
oci:
  identity:
    authorization:
      enabled: true
      service-name: "example-service"
      region: "us-phoenix-1"
      physical-ad: "PHX-AD-1"
```

Example using service-enclave authorization with derived endpoint:

```yaml
oci:
  identity:
    authorization:
      enabled: true
      service-name: "example-service"
      service-enclave: true
      availability-domain: "PHX-AD-1"
```

Example using an explicit authorization endpoint:

```yaml
oci:
  identity:
    authorization:
      enabled: true
      service-name: "example-service"
      service-uri: "https://authservice.svc.ad1.us-phoenix-1"
      service-enclave: true
```

| Key | Default Value | Description |
|-----|---------------|-------------|
| `oci.identity.authorization.enabled` | `true` | Enables creation of the authorization client. |
| `oci.identity.authorization.service-uri` | | Optional explicit authorization endpoint. |
| `oci.identity.authorization.service-name` | | Required service name passed to the authorization client. |
| `oci.identity.authorization.region` | | Region for non-enclave authorization. |
| `oci.identity.authorization.physical-ad` | | Physical AD for non-enclave authorization, or the regional AD value for explicit enclave endpoints. |
| `oci.identity.authorization.availability-domain` | | Availability domain used to derive service-enclave endpoints when `service-enclave=true` and `service-uri` is not set. |
| `oci.identity.authorization.service-enclave` | `false` | Enables service-enclave authorization mode. |
| `oci.identity.authorization.root-cert-path` | | Optional CA bundle or root certificate path. |
| `oci.identity.authorization.metrics-lib` | | Optional Auth SDK metrics library name. |

Authorization validation rules:

* If `service-uri` is not configured and `service-enclave=false`, both `region` and `physical-ad` are required.
* If `service-uri` is not configured and `service-enclave=true`, `region` must not be set and `availability-domain` is required.
* If `service-uri` points to a non-service-enclave endpoint, `service-enclave` must be `false`, and both `region` and `physical-ad` are required.
* If `service-uri` points to a service-enclave endpoint, `region` must not be set, `availability-domain` must not be set, and `physical-ad` must be omitted or set to the regional AD value.

### Request Context

`IdentityContext` is registered into `request.context()` only for intercepted endpoints. In practice, that means the endpoint must participate in the authorization integration, typically by using an OCI authorization annotation such as `@AuthorizationPermission`. Direct `IdentityContext` parameter injection depends on that context entry being present.

The context is a read-only map of Auth SDK request properties. A common example is the authenticated principal under `AuthContextRequestFilter.PIC_PRINCIPAL`.

---

## References

* [IdentityConfigBlueprint](../identity/src/main/java/com/oracle/helidon/oci/identity/IdentityConfigBlueprint.java)
* [AuthenticationConfigBlueprint](../identity/src/main/java/com/oracle/helidon/oci/identity/AuthenticationConfigBlueprint.java)
* [AuthorizationConfigBlueprint](../identity/src/main/java/com/oracle/helidon/oci/identity/AuthorizationConfigBlueprint.java)
* [ServiceAuthenticationClientFactory](../identity/src/main/java/com/oracle/helidon/oci/identity/ServiceAuthenticationClientFactory.java)
* [AuthenticatorClientFactory](../identity/src/main/java/com/oracle/helidon/oci/identity/AuthenticatorClientFactory.java)
* [AuthorizationClientFactory](../identity/src/main/java/com/oracle/helidon/oci/identity/AuthorizationClientFactory.java)
* [IdentityContext](../identity/src/main/java/com/oracle/helidon/oci/identity/IdentityContext.java)
* [OciAuthorizationExtension](../codegen/src/main/java/com/oracle/helidon/oci/codegen/OciAuthorizationExtension.java)
* [Echo example](../examples/echo/src/main/java/com/oracle/helidon/oci/examples/echo/EchoEndpoint.java)

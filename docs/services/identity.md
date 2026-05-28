# Identity

---

## Overview

The Identity module integrates Helidon with the OCI Auth SDK. It provides configured service-registry factories for:

* `ServiceAuthenticationClient`
* `AuthenticatorClient`
* `Optional<IAuthorizationClient>`
* SPLAT-aware Auth SDK request filter factory
* per-request `IdentityContext`, `Principal`, and `AuthorizationRequest` injection

The configuration root is `oci.identity`. Authentication and authorization are configured independently under:

* `oci.identity.authentication`
* `oci.identity.authorization`
* `oci.identity.splat-aware`

For request handling, authentication and authorization are applied through the Helidon OCI code generation integration.
Annotated endpoint methods are intercepted automatically. The generated interceptor uses
`AuthContextRequestFilterFactory` to run a `SplatAwareAuthContextRequestFilter`, performs authentication and optional
authorization, and registers identity request data into the Helidon request context. This replaces the older filter-path
configuration model and does not require configuring `oci.identity.filters.*`.

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

When the Helidon service registry is enabled, the module contributes the Identity services automatically. The generated
authorization interceptor obtains `AuthContextRequestFilterFactory` from the registry and applies a SPLAT-aware Auth SDK
filter to intercepted endpoints.

### Interception Annotations

Use Auth SDK authorization annotations on methods that require a permission check:

```java
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;

@AuthorizationPermission("IDENTITY_ONCE")
String once() {
    return "ok";
}
```

Use `@AuthorizationPermissions` for methods that need more than one permission:

```java
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermissions;

@AuthorizationPermissions({
        @AuthorizationPermission("IDENTITY_READ"),
        @AuthorizationPermission("IDENTITY_UPDATE")
})
String update() {
    return "ok";
}
```

Use `@Identity.Authenticated` when the method needs an authenticated request but does not need service-side
authorization:

```java
import com.oracle.helidon.oci.identity.Identity;
import com.oracle.pic.identity.authentication.Principal;

@Identity.Authenticated
String currentUser(Principal principal) {
    return principal.getSubjectId();
}
```

`@Identity.Authenticated` can also be applied to an endpoint class. In that form, all endpoint methods in the class are
intercepted for authentication. Method-level Auth SDK authorization annotations still use the authorization path:

```java
import io.helidon.http.Http;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.identity.Identity;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;

@RestServer.Endpoint
@Http.Path("/identity")
@Identity.Authenticated
class IdentityEndpoint {

    @Http.GET
    @Http.Path("/me")
    String me(Principal principal) {
        return principal.getSubjectId();
    }

    @Http.POST
    @Http.Path("/admin")
    @AuthorizationPermission("IDENTITY_ADMIN")
    String admin() {
        return "ok";
    }
}
```

The code generator recognizes Auth SDK annotations in
`com.oracle.pic.identity.authorization.permissions.annotations` and triggers the same generated interceptor:

* `@AuthorizationPermission`
* `@AuthorizationPermissions`
* `@AuthorizeAssociate`
* `@AuthorizeCreate`
* `@AuthorizeDelete`
* `@AuthorizeReadOnly`
* `@AuthorizeUpdate`
* `@BodyVerification`
* `@NetworkBasedAccessControl`
* `@RejectCrossTenancyRequest`
* `@ResourceAssociationReviewed`
* `@VariableOperationName`
* `@VariableString`
* `@VariableStrings`
* `@ZprBasedAccessControl`

Most Auth SDK annotations use the authorization path. `@BodyVerification` by itself only requires authentication, while
authorization wins when it appears together with a permission or action annotation.

### Request Data Injection

The recommended way to access authenticated request data in a REST endpoint method is to declare a `Principal`,
`AuthorizationRequest`, or `IdentityContext` parameter directly. The generated handler resolves the values from
`request.context()` for each request.

```java
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;

@RestServer.Endpoint
@Http.Path("/identity")
@Service.Singleton
class IdentityEndpoint {

    @Http.POST
    @Http.Path("once")
    @AuthorizationPermission("IDENTITY_ONCE")
    String once(@Http.Entity String message, Principal principal) {
        return principal.getSubjectId() + ": " + message;
    }
}
```

`AuthorizationRequest` can also be injected directly when the endpoint needs to inspect or copy the request prepared by
the Auth SDK:

```java
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;

String create(@Http.Entity CreateRequest request, AuthorizationRequest authorizationRequest) {
    // customize or copy the prepared authorization request
}
```

`AuthorizationRequest` injection requires an intercepted endpoint for which the Auth SDK created an authorization request.

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

If `oci.identity.authorization.enabled=false`, the authorization client is not created. Authentication still remains
available through `ServiceAuthenticationClient` and `AuthenticatorClient`. Endpoints that use authenticated-only
interception continue to work without the authorization client.

The request filter is `SplatAwareAuthContextRequestFilter`. For requests that arrive on the configured SPLAT mTLS port,
it treats the request as already authenticated by SPLAT and hydrates Auth SDK request properties from SPLAT principal
headers. For other requests, it falls back to normal direct Identity authentication behavior.

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
| `oci.identity.authorization.service` | | Alias for `service-name`; configure only one of the two keys. |
| `oci.identity.authorization.region` | | Region for non-enclave authorization. |
| `oci.identity.authorization.physical-ad` | | Physical AD for non-enclave authorization, or the regional AD value for explicit enclave endpoints. |
| `oci.identity.authorization.availability-domain` | | Availability domain used to derive service-enclave endpoints when `service-enclave=true` and `service-uri` is not set. |
| `oci.identity.authorization.service-enclave` | `false` | Enables service-enclave authorization mode. |
| `oci.identity.authorization.root-cert-path` | | Optional CA bundle or root certificate path. |
| `oci.identity.authorization.metrics-lib` | | Optional Auth SDK metrics library name. |

Aliases are provided for user convenience, either to align with native OCI parameter names or with similar settings
in other Helidon OCI modules. Specify at most one name for an aliased setting, not both.

Authorization validation rules:

* If `service-uri` is not configured and `service-enclave=false`, both `region` and `physical-ad` are required.
* If `service-uri` is not configured and `service-enclave=true`, `region` must not be set and `availability-domain` is required.
* If `service-uri` points to a non-service-enclave endpoint, `service-enclave` must be `false`, and both `region` and `physical-ad` are required.
* If `service-uri` points to a service-enclave endpoint, `region` must not be set, `availability-domain` must not be set, and `physical-ad` must be omitted or set to the regional AD value.

### SPLAT-Aware Request Filter

SPLAT-aware filter config is loaded from `oci.identity.splat-aware`.

```yaml
oci:
  identity:
    splat-aware:
      splat-request-port: 8443
      additional-splat-request-ports: []
      skip-authorization-for-splat: true
      validate-splat-cert: true
      disable-tag-only-request-check: false
      reject-x-region-calls: false
      region: "us-ashburn-1"
```

| Key | Default Value | Description |
|-----|---------------|-------------|
| `oci.identity.splat-aware.splat-request-port` | `0` | Port where SPLAT requests are expected to arrive. |
| `oci.identity.splat-aware.additional-splat-request-ports` | `[]` | Additional mTLS-enabled SPLAT request ports. |
| `oci.identity.splat-aware.skip-authorization-for-splat` | `false` | Allows service-side AuthZ to be skipped when SPLAT sends the skip-authorization header. |
| `oci.identity.splat-aware.validate-splat-cert` | `true` | Validates the SPLAT client certificate common name when identifying SPLAT requests. |
| `oci.identity.splat-aware.disable-tag-only-request-check` | `false` | Disables tag-only request detection for SPLAT requests. |
| `oci.identity.splat-aware.reject-x-region-calls` | `false` | Rejects cross-region SPLAT client certificates. |
| `oci.identity.splat-aware.region` | | Optional OCI region override used for SPLAT certificate validation. |

If `region` is omitted, the filter factory resolves the region from `oci.identity.authentication.region`, then `oci.identity.authorization.region`, then the runtime `Region` service.

### Request Context

`IdentityContext` is registered into `request.context()` only for intercepted endpoints. In practice, that means the
endpoint must participate in the identity integration by using `@Identity.Authenticated` or a supported Auth SDK
authorization annotation. Direct `IdentityContext`, `Principal`, and `AuthorizationRequest` parameter
injection depends on that context entry being present.

The context is a read-only map of Auth SDK request properties. A common example is the authenticated principal under `AuthContextRequestFilter.PIC_PRINCIPAL`.

---

## References

* [IdentityConfigBlueprint](../../identity/src/main/java/com/oracle/helidon/oci/identity/IdentityConfigBlueprint.java)
* [AuthenticationConfigBlueprint](../../identity/src/main/java/com/oracle/helidon/oci/identity/AuthenticationConfigBlueprint.java)
* [AuthorizationConfigBlueprint](../../identity/src/main/java/com/oracle/helidon/oci/identity/AuthorizationConfigBlueprint.java)
* [SplatAwareConfigBlueprint](../../identity/src/main/java/com/oracle/helidon/oci/identity/SplatAwareConfigBlueprint.java)
* [AuthContextRequestFilterFactory](../../identity/src/main/java/com/oracle/helidon/oci/identity/AuthContextRequestFilterFactory.java)
* [ServiceAuthenticationClientFactory](../../identity/src/main/java/com/oracle/helidon/oci/identity/ServiceAuthenticationClientFactory.java)
* [AuthenticatorClientFactory](../../identity/src/main/java/com/oracle/helidon/oci/identity/AuthenticatorClientFactory.java)
* [AuthorizationClientFactory](../../identity/src/main/java/com/oracle/helidon/oci/identity/AuthorizationClientFactory.java)
* [IdentityContext](../../identity/src/main/java/com/oracle/helidon/oci/identity/IdentityContext.java)
* [OciAuthorizationExtension](../../codegen/src/main/java/com/oracle/helidon/oci/codegen/OciAuthorizationExtension.java)
* [Identity example](../../examples/identity/src/main/java/com/oracle/helidon/oci/examples/identity/IdentityEndpoint.java)

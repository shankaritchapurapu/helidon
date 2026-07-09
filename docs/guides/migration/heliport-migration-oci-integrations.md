# OCI Integration Migration

Heliport's OCI migration is feature-specific and role-aware. It does not treat all OCI SDK usage as migration debt, and it does not add first-class OCI Helidon artifacts only because a package name appears in source. Each feature row must prove one of these outcomes:

- migrated to a Helidon OCI integration
- retained as SDK/client/provider compatibility
- reported as an application owner frontier with exact source/config evidence
- excluded because the module is a provider/source owner, not a consumer.

The main owner wave is `oci-helidon`, but OCI migration also depends on Maven governance, Guice/service-registry migration, request/response carrier migration, configuration convergence, and generated OpenAPI alignment.

Helidon OCI and Talon integrations are feature modules. The `Preferred Helidon target` column below names the module family Heliport adds when the feature is actually used and the target role is proven. It is not a list of dependencies or Java module requirements that every migrated service receives. Concrete dependency snippets belong in the Maven guide; this page explains which source/config shapes justify each module and what code/config ownership changes after that module is present.

## How Helidon OCI Integrations Appear In Migrated Code

In Dropwizard/Guice services, OCI clients are commonly assembled in modules from app-specific config DTOs, TLS helpers, auth providers, and Jersey client settings. In the Helidon target, the safest shapes move standard construction into Talon/Helidon OCI integration modules and inject the resulting client or request context into application services.

| Source shape | Helidon/Talon target shape | What remains application-owned |
| --- | --- | --- |
| Guice `@Provides` method builds a standard OCI client from endpoint/auth config. | Helidon OCI dependency plus config keys; application service injects the client or a named supplier. | Business retry policy, request payload construction, and domain-specific error handling. |
| Multiple same-type clients, such as worker and poller `WorkflowClient`. | `@Service.Named` constructor parameters or named config blocks. | The reason each named client exists and any workflow-domain orchestration. |
| JAX-RS authentication filter or dynamic feature enforces standard identity authentication. | `@Identity.Authenticated`, injected `IdentityContext`, and Helidon OCI identity interception. | Route-specific authorization policy, SAML/principal enrichment, and local/mock auth branches. |
| Direct TLS/cert/session construction around Secret Service or service-auth clients. | Talon SSv2 TLS manager, Helidon WebServer TLS, or named dynamic SSL context provider when fields map safely. | Custom reload semantics, cert bundle assembly, signer selection, and secret path policy. |
| Direct metrics/audit emitters with service-specific names and payloads. | Helidon metrics publisher or audit integration only for standard publisher/bootstrap behavior. | Metric names, dimensions, payload shape, and fleet/project semantics. |

## Feature Summary

| Feature | Preferred Helidon target | What Heliport migrates or reports |
| --- | --- | --- |
| Audit | `helidon-oci-audit` | Sherlock/audit config dependency convergence, `auditConfig` transposition to `oci.auditv2`, preservation of Talon-owned runtime dependencies, filter/logger supplier frontiers, and app-domain audit payload frontiers. |
| Environment config | OCI environment config support | Region, realm, environment, and location override evidence moved toward Helidon config sources; legacy reader/provider residue is classified. |
| Error Code | OCI error-code plus Helidon WebServer target artifacts | Renderable exception/error-code server evidence, mapper/runtime residue, and payload handling frontiers. |
| Identity/auth | `helidon-oci-identity` and SDK auth modules | Authenticated endpoint annotations, legacy identity filter/binder cleanup, `IdentityContext` ownership, auth-provider frontiers, and `oci.identity` config. |
| Identity Client | `helidon-oci-identity-client` | Plain identity client construction and service-registry candidates; tag lookup, SAML/principal enrichment, local/mock auth, and custom transport remain exact evidence until target equivalence is proven. |
| OCI JAX-RS compatibility | `helidon-oci-jaxrs` where needed | Retained provider/client carrier compatibility without masking app-owned server-side JAX-RS or Servlet residue. |
| Kiev | `helidon-oci-kiev` | Kiev data-store dependency convergence, transaction provider/supplier materialization, config evidence, transaction/frontier packets, and eventual `@Kiev.Transaction` targets where safe. |
| Limits | `helidon-oci-limits` | Plain `LimitsDPClient` provider convergence and `oci.limits` config transposition; CP/custom cache/TLS/transport remains frontier evidence. |
| Metering | `helidon-oci-metering-cp` or `helidon-oci-metering-dp` | BLING dependency materialization by role, `meteringConfig` transposition, wrapper cleanup, and explicit metering behavior frontiers. |
| Metrics | `helidon-oci-metrics` | OCI/T2 metrics publisher config under `metrics.publishers`, automatic HTTP metrics, JVM gauges, prefix annotation metadata, reporter/bootstrap frontiers, and Maven governance for known metrics module conflicts while preserving Talon-owned transitive dependencies. |
| Object Storage Client | `helidon-oci-sdk-object-storage-client` | Plain object-storage client construction and service-registry candidates; domain adapters, custom signers/TLS/transport, request/model mapping, and error policy remain frontiers. |
| OCI SDK auth modules | First-class OCI auth modules | Standard auth-provider class materialization; custom certs, signers, and policy factories remain compatibility/frontier evidence. |
| Request ID | `helidon-oci-request-id-webserver` or `helidon-oci-request-id` | Server `opc-request-id` header/filter ownership, endpoint parameter cleanup, generated header obligations, and client/domain propagation frontiers. |
| Secret Service | `helidon-oci-secret-service-config-source` and `helidon-oci-secret-service-tls-manager` | Secret config-source and TLS-manager dependency materialization, config evidence, and direct lookup/TLS construction frontiers. |
| SPLAT | `helidon-oci-splat` plus WebServer/request-context migration | Source-evidenced SPLAT consumer artifacts, generated OCID extraction migration, request-context/config ownership, and provider/source-owner classification. |
| Tagging | `helidon-oci-tagging` | Plain tagging client/global-settings client construction; slug conversion, authorization validation, security-attribute slice settings, and custom policy remain exact evidence. |
| Workflow/WFaaS | `helidon-oci-workflow` | WFaaS client/module cleanup, named worker/poller injection, workflow config transposition, TLS provider config, and workflow domain/frontier preservation. |

## Shared OCI SDK Authentication And Environment Config

Many Talon client integrations use the shared OCI SDK authentication provider. Heliport migrates explicit auth-provider construction only when the target module can own it; otherwise it records the custom signer, certificate, or client factory as a frontier.

```yaml
helidon:
  oci:
    authentication-method: instance-principal
    imds-timeout: PT3S
    imds-detect-retries: 1

  oci-env:
    prefix: "oci.env"
    location-override:
      region: "us-ashburn-1"
      availability-domain: "iad-ad-1"
      fault-domain: 1
```

Use `helidon.oci` for shared OCI SDK authentication such as instance principal, resource principal, OKE workload identity, config-file, session-token, or service-principal authentication. Use `helidon.oci-env` when migrated endpoint config needs derived values such as `${oci.env.ad-number}` or `${oci.env.iaas-domain-name}`.

Service-principal authentication is selected by configuration and module presence. When `authentication-method: service-principal` uses its default instance-principal backing, IMDS must be available. For local tests or substrate-like deployments without IMDS, the migrated target can use explicit service-principal certificates instead:

```yaml
helidon:
  oci:
    authentication-method: service-principal
    federation-endpoint: https://auth.us-phoenix-1.oraclecloud.com
    tenant-id: ocid1.tenancy.oc1...
    authentication:
      service-principal:
        use-instance-principal: false
        certificates:
          - certificate: /etc/oci/service-principal/cert.pem
            private-key: /etc/oci/service-principal/key.pem
            passphrase: ""
          - certificate: /etc/oci/service-principal/intermediate.pem
```

## Kiev

Kiev migration targets `com.oracle.helidon.oci.kiev:helidon-oci-kiev` when the module is a consumer of Kiev data stores or transaction support.

Preferred target shape:

- Inject `MappedDataStore`, `DataStore`, or `KievTransactionSupport` through constructor injection on `@Service.Singleton` services.
- Use `@Service.Named(<store-name>)` when multiple stores are present.
- Replace ad hoc transaction wrappers with `@Kiev.Transaction` when the service method boundary is the transaction boundary.
- Move backend, endpoint, auth, store, compartment, locality, and related values under `oci.kiev.data-stores`.
- Use `oci.dynamic-ssl-context-providers[]` plus a `dynamic-ssl-context-provider-name` when Kiev service auth uses reusable TLS.

Heliport currently materializes safe transaction-provider suppliers and reports legacy data-store/config construction as precise Kiev frontiers when it cannot map every setting.

**Legacy provider/module shape**

```java
public class DaoModule extends AbstractModule {
    @Provides
    TransactionProvider transactionProvider(MappedDataStore dataStore) {
        return new KievTransactionProvider(dataStore);
    }
}
```

**Helidon-owned supplier shape**

```java
import com.oracle.pic.kiev.mapping.MappedDataStore;
import com.oracle.pic.sfw.dal.TransactionProvider;
import com.oracle.pic.sfw.kiev.KievTransactionProvider;
import java.util.function.Supplier;
import io.helidon.service.registry.Service;

@Service.Singleton
final class TransactionProviderSupplier implements Supplier<TransactionProvider> {
    private final TransactionProvider value;

    @Service.Inject
    TransactionProviderSupplier(@Service.Named("orders")
                                MappedDataStore mappedDataStore) {
        this.value = new KievTransactionProvider(mappedDataStore);
    }

    @Override
    public TransactionProvider get() {
        return value;
    }
}
```

Configuration example:

**Legacy config**

```
kievConfiguration {
  kaasStoreConfig {
    storeName: "orders"
    tenantId: "${TENANT_ID}"
  }
}
```

**Target config shape**

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  dynamic-ssl-context-providers:
    - name: kiev-service-auth
      root-cert-pem-path: /etc/oci-pki/ca-bundle.pem
      cert-reload-duration: PT5M
      cert-ssl-algorithm: SunX509

  kiev:
    data-stores:
      - backend: "SERVICE"
        store-name: orders
        app-name: email-api
        service:
          compartment-id: ${KIEV_COMPARTMENT_ID}
          frontend-endpoint: ${KIEV_ENDPOINT}
          locality: "REGIONAL"
          auth:
            type: "OVERRIDDEN"
            tls:
              dynamic-ssl-context-provider-name: kiev-service-auth
```

The actual store names, application names, backend, locality, auth, and TLS settings must come from source configuration. Heliport reports application-owner actions when it cannot prove the mapping safely. Other Talon-supported Kiev targets include `IN_MEMORY`, `DIRECT_DB`, `SERVICE` with `INSTANCE` auth, `SERVICE` with native `S2S` auth, and `KIAB_LOCAL` for local KaaS testing.

## Workflow / WFaaS

Workflow migration targets `com.oracle.helidon.oci.workflow:helidon-oci-workflow`.

Preferred target shape:

- Inject `WorkflowClient` directly for the default client.
- Inject named worker and poller clients when the legacy module explicitly distinguished those roles.
- Move endpoint and timeout configuration to Duration-based `oci.workflow.endpoint-details.*` settings.
- Move retry settings under `oci.workflow.retry-policy.*`.
- Move reusable workflow TLS config to `oci.dynamic-ssl-context-providers[]` and reference it with `oci.workflow.dynamic-ssl-context-provider-name`.
- Preserve workflow domain classes, definitions, payload construction, and worker-manager orchestration as application logic unless a Helidon OCI abstraction explicitly owns them.

**Legacy Guice provider**

```java
public class WorkflowModule extends AbstractModule {
    @Provides
    @Singleton
    WorkflowClient buildWorkerClient() {
        return ChastWorkflowClient.builder()
                .clientRole(ClientRole.WORKER)
                .domainId("legacy-domain")
                .endpointConfig(endpointConfig())
                .build();
    }
}
```

**After WFaaS ownership**

```java
import com.oracle.pic.workflow.module.WorkflowClientModule;
import com.oracle.pic.workflow.worker.WorkflowClient;

import io.helidon.service.registry.Service;

@Service.Singleton
final class WorkflowRunner {
    private final WorkflowClient workerClient;

    @Service.Inject
    WorkflowRunner(@Service.Named(WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME)
                   WorkflowClient workerClient) {
        this.workerClient = workerClient;
    }
}
```

Configuration example:

```yaml
oci:
  dynamic-ssl-context-providers:
    - name: workflow
      leaf-cert-path: /etc/oci-pki/workflow-leaf.pem
      leaf-cert-key-path: /etc/oci-pki/workflow-leaf.key
      leaf-cert-key-passphrase: ${WORKFLOW_KEY_PASSPHRASE}
      intermediate-cert-path: /etc/oci-pki/workflow-intermediate.pem
      root-cert-path: ${WORKFLOW_ROOT_CERT_PATH}
      duration: PT15M

  workflow:
    domain-id: ${WORKFLOW_DOMAIN_ID}
    dynamic-ssl-context-provider-name: workflow
    endpoint-details:
      server-endpoint: ${WORKFLOW_ENDPOINT}
      connect-timeout: PT10S
      worker-read-timeout: PT30S
      poller-read-timeout: PT90S
    worker-identifier: ${WORKFLOW_WORKER_ID}
    retry-policy:
      max-retry-count: 3
      delay-between-retry: PT0.25S
      jitter-factor: 0.5
```

Heliport removes legacy default client helper methods only when the source shape matches a known default WFaaS provider. Custom client configurators, Jersey client settings, local endpoints, worker domain behavior, and payload logic stay application-owned or are reported as frontiers.

## Identity And Auth

Identity migration targets `com.oracle.helidon.oci.identity:helidon-oci-identity` plus the relevant OCI SDK auth modules.

Preferred target shape:

- Use `@Identity.Authenticated` for endpoints that require authentication but do not carry a service-specific authorization permission.
- Use endpoint parameters of type `IdentityContext` for authenticated request data.
- Inject `Supplier<IdentityContext>` into singleton collaborators that need request-scoped identity access.
- Let OCI Helidon request interception replace app-owned JAX-RS filters and DynamicFeature registration.
- Move standard identity config under `oci.identity`.

**Legacy filter/name binding**

```java
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthRequired {
}

public class Endpoint {
    @AuthRequired
    public String get() {
        return "ok";
    }
}
```

**Helidon OCI identity**

```java
import com.oracle.helidon.oci.identity.Identity;

public class Endpoint {
    @Identity.Authenticated
    public String get() {
        return "ok";
    }
}
```

When the source endpoint used Auth SDK permissions, the Helidon target keeps the permission annotation and receives authenticated values as endpoint parameters:

```java
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;

import io.helidon.http.Http;

@AuthorizationPermission("EMAIL_SEND")
String send(@Http.Entity SendEmailRequest request,
            Principal principal) {
    return principal.getSubjectId();
}
```

Filter cleanup example:

```java
// Before
environment.jersey().register(AuthFilterDynamicFeature.class);

// After
// Registration is removed after OCI Helidon identity owns interception.
```

Heliport preserves or reports custom certificate selection, signer factories, authorization permission checks, tag/domain authorization, SAML principal enrichment, local/mock auth, and custom transport policy.

Configuration example:

```yaml
oci:
  identity:
    authentication:
      global-business-unit: "Cloud-Infra"
      team-name: "ExampleTeam"
      application-name: "email-api"
      use-instance-principal: true
    authorization:
      enabled: true
      service-name: "email-api"
      physical-ad: "IAD-AD-1"
    splat-aware:
      splat-request-port: 8443
      additional-splat-request-ports: []
      skip-authorization-for-splat: true
      validate-splat-cert: true
      disable-tag-only-request-check: false
      reject-x-region-calls: false
      region: "us-ashburn-1"
```

## SPLAT

SPLAT migration is role-aware. Consumer modules with source-evidenced SPLAT runtime usage receive `helidon-oci-splat` and request-context/config migration. SPLAT provider or source-owner modules must not receive client artifacts merely because their packages contain SPLAT provider namespaces.

SPLAT-protected generated endpoints require the Helidon listener itself to terminate mTLS and require client certificates. The Talon SPLAT interceptor runs on generated endpoint handling and reads the peer certificate chain from the request context; it does not select or secure the socket for the application.

```yaml
server:
  port: 8080
  sockets:
    splat:
      port: 8443
      tls:
        client-auth: "REQUIRED"
        endpoint-identification-algorithm: "NONE"
        trust:
          pem:
            cert-chain:
              resource.path: /path/to/trusted-client-ca.pem
        private-key:
          pem:
            key:
              resource.path: /path/to/server-key.pem
            cert-chain:
              resource.path: /path/to/server-cert-chain.pem

oci:
  splat:
    enabled: true
    skip-authz-validation-check: false
    reject-x-region-calls: false
    region: "us-ashburn-1"
```

One supported SPLAT source migration is generated OCID extraction from the legacy SPLAT header carrier to Helidon `ServerRequest`.

**Legacy SPLAT carrier**

```java
import com.oracle.pic.platform.splat.sdk.SplatOcidExtractor;
import javax.ws.rs.core.HttpHeaders;

String imageId = SplatOcidExtractor.getOcid(headers, ENTITY_TYPE);
```

**Helidon request carrier**

```java
import com.google.common.base.Strings;
import com.oracle.pic.commons.exceptions.server.ErrorCode;
import com.oracle.pic.commons.exceptions.server.RenderableException;
import com.oracle.pic.commons.util.EntityType;
import com.oracle.pic.platform.splat.sdk.SplatHelper;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;

import java.io.IOException;

String imageId = heliportSplatGeneratedOcid(request, ENTITY_TYPE);

private static String heliportSplatGeneratedOcid(
        ServerRequest request,
        EntityType entityType) {
    String header = request.headers()
            .first(HeaderNames.create("oci-splat-generated-ocids"))
            .orElse(null);
    if (header != null) {
        try {
            String ocid = SplatHelper.extractGeneratedOCID(header, entityType);
            if (!Strings.isNullOrEmpty(ocid)) {
                return ocid;
            }
        } catch (IOException e) {
            throw new RenderableException(
                    ErrorCode.InternalError,
                    "Internal Server Error",
                    e);
        }
    }
    throw new RenderableException(
            ErrorCode.InternalError,
            "Internal Server Error");
}
```

Remaining SPLAT request-context, authorization, generated identifier, and provider/consumer boundaries are reported with source evidence.

## Request ID

When the server owns `opc-request-id`, Heliport prefers `helidon-oci-request-id-webserver`. When only API carrier types are needed, it uses `helidon-oci-request-id`.

Preferred target shape:

- Use endpoint parameters of type `OciRequestId`.
- Inject `Supplier<OciRequestId>` into singleton collaborators that need request-scoped access.
- Let the webserver filter own the response `opc-request-id` header.
- Preserve method-local headers such as `etag`, `opc-next-page`, work-request IDs, and locations.

```java
import com.oracle.helidon.oci.requestid.OciRequestId;
import io.helidon.http.Http;

@Http.GET
String get(OciRequestId requestId) {
    return requestId.upstreamHeaderValue();
}
```

```java
// Before
void list(@HeaderParam("opc-request-id") String requestId,
          ServerResponse response) {
    response.send();
}

// After, when the value is unused and webserver request-id owns the header
void list(ServerResponse response) {
    response.send();
}
```

Used request ID values are preserved or promoted only when the target shape is safe.

## Metrics And Metering

OCI metrics target `com.oracle.helidon.oci.metrics:helidon-oci-metrics` and a Helidon metrics publisher entry:

```yaml
helidon:
  oci:
    authentication-method: instance-principal

metrics:
  publishers:
    - type: oci
      reporter:
        overlay:
          project: ${T2_PROJECT}
          fleet: ${T2_FLEET}
          region: ${OCI_REGION}
          endpoint: ${T2_ENDPOINT}
      sample-interval: PT1S
      accumulators:
        max-pending-seconds: 10
        max-raw-timer-samples-per-second: 1024
        max-raw-summary-samples-per-second: 1024
        pressure-log-interval: PT30S
      default-dimensions:
        service: email-api
        hostclass: ${HOSTCLASS}
```

Heliport migrates source-evidenced T2 project, fleet, endpoint, region, dimensions, and supported client settings when the mapping is clear. It does not invent metric names or fleet values. Legacy `ServiceConfigurator` metrics wiring, custom reporter bootstrap, and payload/runtime behavior are frontiers until target equivalence is encoded.

Current Talon metrics also owns service-core-compatible automatic HTTP metrics and built-in JVM gauges. Heliport preserves source-evidenced publisher settings such as `enable-detailed-timing-auto-metrics`, `resource-package-prefix`, `auto-http.enabled`, `auto-http.user-agent-metrics-enabled`, `auto-http.max-user-agent-series`, `auto-http.runtime-dimension`, `metrics-scope-name`, `includes`, `excludes`, and filter matching mode. Detailed user-agent series are bounded and excess identities are aggregated as `OTHER`. Runtime-dimension values are inserted as-is into count-style automatic HTTP metric names, matching service-core. Timer and distribution-summary retention is controlled by publisher-level `accumulators`; Heliport should normally keep the defaults and surface them for owner review only for unusually high-volume services. Endpoint prefix annotations such as `@MetricPrefix` and `@SecondaryMetricPrefix` are generated-metadata features and require the Talon codegen processor.

Metering is split by role:

- `com.oracle.pic.bling.emit.*` and `MeteringAgentConfig` select `helidon-oci-metering-cp`.
- Data-plane usage such as `LogFileUsageRecorder` selects `helidon-oci-metering-dp`.

Data-plane metering configuration keeps the native emitter-dp required values under `oci.metering` and, when Object Storage backup is enabled, uses the shared Object Storage client config:

```yaml
oci:
  metering:
    enabled: true
    endpoint: https://bling.example.internal
    metering-dir: /var/opt/oracle/metering
    client-id: email-api-dp
    service: email-api
    region: us-ashburn-1
    os-enabled: false
    k8s-based-deployment: false
    bling-publisher-client:
      endpoint: https://bling.example.internal
      client-id: email-api-dp
  object-storage-client:
    endpoint: https://objectstorage.us-ashburn-1.oraclecloud.com
    region: us-ashburn-1
```

Control-plane metering configuration uses bucket configs and requires a Helidon service factory for `MappedDataStore`, commonly supplied by the Kiev integration:

```yaml
oci:
  metering:
    enabled: true
    endpoint: https://bling.example.internal
    client-id: email-api-cp
    region: us-ashburn-1
    metering-period: PT1M
    bucket-configs:
      - bucket-name: email_api_metering
        service-name: email-api
        meter-name: email-api.requests
```

CP metering records through the Kiev-backed metering-agent path. Application code that records CP metering points still needs to run the metered work in the appropriate Kiev transaction boundary, either through existing transaction logic or through a migrated `@Kiev.Transaction` service method.

```java
import com.oracle.helidon.oci.kiev.Kiev;
import com.oracle.helidon.oci.metering.cp.Metering;

@Kiev.Transaction("email-store")
@Metering.Point(value = "email.requests",
                tags = @Metering.Tag(key = "source", value = "api"))
void recordEmail(@Metering.CompartmentId String compartmentId,
                 @Metering.ResourceId String messageId,
                 @Metering.Amount float amount) {
    service.record(compartmentId, messageId, amount);
}
```

BLING wrapper cleanup can remove temporary Dropwizard-style agent wrapping while preserving application metering behavior:

```java
// Before
MeteringAgent meteringAgent = new MeteringAgent(config, auth, store, host, region);
this.value = new BlingMeteringAgent(meteringAgent);

// After
this.value = new BlingMeteringAgent(config, auth, store, host, region);
```

## Secret Service

Secret Service migration targets:

- `helidon-oci-secret-service-config-source`;
- `helidon-oci-secret-service-tls-manager`.

Preferred config-source shape in explicit Helidon meta-config:

```yaml
sources:
  - type: "oci-env"
  - type: "oci-secret-service"
    properties:
      prefix: "oci.ssv2"
      cache-ttl: "PT5M"
      poll-interval: "PT30M"
      client:
        endpoint: "https://secret-service-ce.${oci.env.iaas-domain-name}/v1"
        tls-config:
          ca-bundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
        retry-config:
          max-retries: 3
          min-retry-delay-in-ms: 100
```

Preferred server TLS rotation shape when the service uses SSv2-backed PKI material:

```yaml
server:
  port: 8443
  host: 0.0.0.0
  tls:
    client-auth: "REQUIRED"
    endpoint-identification-algorithm: "NONE"
    manager:
      oci-ssv2:
        reload.expression: "0 0/30 * * * ? *"
        pki.secret-path: /secret/service/mtls/server/latest
        trust.path: /etc/oci-pki/ca-bundle.pem
```

Heliport preserves secret-source prefix, endpoint, cache, retry, and TLS material settings when source values map cleanly. Direct lookup/domain retrieval, service-specific secret paths, certificate names, and TLS refresh semantics remain frontiers when not proven.

The Secret Service config source is lazy and fail-closed at the key level. A first read failure is exposed to Helidon config as an absent value for one `cache-ttl` retry window instead of leaking the resolver exception; existing cached values are kept if a later refresh fails. Applications that rely on immediate startup failure for a missing secret keep that behavior in owner code by calling `orElseThrow()` or an equivalent typed configuration validation after reading the Helidon config node.

## Limits

Limits migration targets `com.oracle.helidon.oci:helidon-oci-limits`.

Preferred target shape:

- Inject or retrieve the Limits client from Helidon service registry.
- Move timeout, async thread count, and endpoint override settings under `oci.limits`.
- Preserve business logic that decides which limits or quotas to query.

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  limits:
    endpoint: ${LIMITS_ENDPOINT}
    client:
      connection-timeout: PT5S
      read-timeout: PT30S
      max-async-threads: 20
```

Plain `LimitsDPClient` providers can converge automatically. CP behavior, custom cache, TLS, client configurators, and transport policy stay precise compatibility evidence until target API proof exists.

## Object Storage, Identity Client, And Tagging

These target-shape encoded client rows are promoted only for plain client construction or service-registry candidates.

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  identity-client:
    region: us-ashburn-1
    client:
      connection-timeout: PT10S
      read-timeout: PT1M
      max-async-threads: 50
  object-storage-client:
    region-id: us-ashburn-1
    client:
      connection-timeout: PT10S
      read-timeout: PT1M
      disable-data-buffering-on-upload: true
  tagging:
    emit-metrics: false
```

| Client | Target | Retained frontier examples |
| --- | --- | --- |
| Identity client | Inject `com.oracle.bmc.identity.Identity` from `helidon-oci-identity-client`. | Local/mock auth branches, tag lookup policy, SAML/principal enrichment, custom transport. |
| Object Storage | Inject `com.oracle.bmc.objectstorage.ObjectStorage` from `helidon-oci-sdk-object-storage-client`. | Domain adapters, custom signers, TLS/transport, request/model mapping, error policy wrappers. |
| Tagging | Use `helidon-oci-tagging` service-registry injection. | Slug conversion, authorization validation, security-attribute slice settings, global slice policy. |

## Audit, Error Code, And OCI JAX-RS Compatibility

Audit and error-code integrations are broad and payload-sensitive. Heliport adds dependencies and config evidence where source support exists, but keeps payload, logging appender, domain event, and custom runtime behavior as frontiers until a Helidon target preserves behavior.

Talon Audit registers an `AuditV2Filter` as a Helidon WebServer feature and applies it to the default socket and configured named sockets.

When audit support is enabled and the Helidon OCI codegen processor is present, generated endpoint methods can receive Sherlock's request-scoped `AuditPayloadAppender` directly. Heliport preserves application-owned event naming and resource/tenant enrichment unless the source behavior maps exactly.

```java
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import io.helidon.http.Http;

@Http.POST
@Http.Path("/orders")
OrderView create(@Http.Entity CreateOrder request,
                 AuditPayloadAppender audit) {
    audit.setResourceId(request.orderId());
    audit.overrideCompartmentId(request.compartmentId());
    return service.create(request);
}
```

```yaml
oci:
  auditv2:
    enabled: true
    event-source: email-api
    respect-splat-audited-flag: true
    request-parameter-rules:
      - resources: "/orders"
        actions: "POST"
        values: "orderId"
    request-header-rules:
      - resources: "/orders"
        actions: "POST"
        values: "opc-request-id"
    response-header-rules:
      - resources: "/orders"
        actions: "POST"
        values: "etag"
```

For local validation, Talon audit can return an audit summary when the request includes `oci-splat-audit-verify: true`. If `respect-splat-audited-flag` is enabled, `oci-splat-audited: true` skips audit emission for a request while keeping the request appender available to endpoint code.

OCI JAX-RS compatibility is not a blanket exemption. `helidon-oci-jaxrs` can be present for retained provider/client surfaces, but application-owned server-side JAX-RS, Jersey, Servlet, request, response, and filter residue must still be migrated by the runtime waves or reported as owner work.

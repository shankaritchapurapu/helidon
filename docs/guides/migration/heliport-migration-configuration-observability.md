# Configuration, Metrics, and Logging

Configuration and observability are behavior surfaces. Heliport preserves how the source application selects configuration, loads secrets, configures listeners, publishes metrics, and initializes logging, or reports the exact owner work that remains.

## Configuration Principles

Heliport follows these rules:

- Preserve source configuration formats unless a safe target conversion is proven.
- For HOCON-origin services, keep runtime `config/**/*.conf` files and include graphs instead of flattening them into a shadow `application.yaml`.
- Preserve `run.sh` or launcher profile selection and pass the selected source file to Helidon through meta-config when needed.
- Add `helidon-config-hocon` when HOCON files remain runtime sources.
- List environment variables, system properties, and file sources in generated meta-config.
- Move Java consumers of Dropwizard configuration DTOs to Helidon `Config`/typed service-registry ownership when the source keys are known.
- Preserve non-runtime `.conf` assets as domain assets instead of treating them as service config.
- Report owner frontiers for custom config readers, custom `ConfigurationFactoryFactory`, and service-specific value derivation.

## Helidon Config Basics For Dropwizard Users

Dropwizard applications usually deserialize one root configuration object and pass that object through `Application.run(...)`, Guice modules, and resource constructors. Helidon reads an ordered tree of configuration sources. A migrated service can inject `Config`, select a subtree, and read typed values directly, or it can expose typed supplier/services that read from `Config`.

| Dropwizard habit | Helidon equivalent | Migration implication |
| --- | --- | --- |
| One root `AppConfig` object passed into `run`. | Ordered config sources loaded by Helidon from meta-config. | Source files and profile selection stay visible; migrated classes read only the subtree they own. |
| Nested DTO getter such as `config.getFoo().getBar()`. | `config.get("foo.bar")` or `config.get("foo").get("bar")`. | Heliport rewrites this only when the old key path and default value are clear. |
| DTO default values or value derivation in setters/builders. | Explicit `orElse(...)`, typed config provider, or owner action. | Defaults are behavior. Heliport preserves them only when it can prove the same value. |
| Guice module receives config and constructs clients. | Service-registry supplier injects `Config` and builds the client from a named subtree. | The construction moves closer to the produced service instead of remaining in a central module. |

## HOCON And Meta-Config

The first HOCON rule is preservation: the selected profile and include graph remain the source of truth. Heliport then adds Helidon config sources around that profile. When server/listener values can be mapped safely, Heliport will also add a small Helidon-owned overlay file. That overlay contains only target runtime keys, not a flattened copy of the application configuration.

**Source launch/config shape**

```
CFG=${CFG:-config/dev.conf}
java -jar service.jar server "$CFG"
```

```
# config/dev.conf
include "base.conf"

server {
  applicationConnectors: [
    {
      type: http
      bindHost: "0.0.0.0"
      port: ${serviceHttpPort}
    },
    {
      type: https
      bindHost: "0.0.0.0"
      port: ${serviceMtlsPort}
      certPath: "/var/certs/service/current/cert.pem"
      keyPath: "/var/certs/service/current/key.pem"
      intermediatePath: "/var/certs/service/current/intermediates.pem"
      trustStoreLocation: "/etc/pki/java/cacerts"
      trustStorePassword: ${trustStorePassword}
      needClientAuth: true
      sslAutoReloadPeriod: "15 minutes"
    }
  ]
  adminConnectors: [
    { type: http, port: ${adminPort} }
  ]
}

assets {
  resourcePath: "/assets"
  uriPath: "/ui"
  indexFile: "index.html"
}
```

**Helidon target shape**

```
CFG=${CFG:-config/dev.conf}
HELIDON_CFG=${HELIDON_CFG:-config/helidon-server.yaml}

java -Dio.helidon.config.meta-config=meta-config.yaml \
  -Dheliport.config.file="$CFG" \
  -Dheliport.helidon.config.file="$HELIDON_CFG" \
  -jar service.jar
```

```yaml
# meta-config.yaml
sources:
  - type: environment-variables
  - type: system-properties
  - type: file
    properties:
      path: ${heliport.config.file}
  - type: file
    properties:
      path: ${heliport.helidon.config.file}
```

The Helidon overlay is intentionally small and target-specific. It can preserve the connector values while moving them to Helidon WebServer listener and TLS shapes. For static PEM material, the target shape follows Helidon WebServer TLS configuration:

```yaml
# config/helidon-server.yaml
server:
  port: ${serviceHttpPort}
  host: 0.0.0.0
  sockets:
    mtls:
      host: 0.0.0.0
      port: ${serviceMtlsPort}
      tls:
        client-auth: "REQUIRED"
        endpoint-identification-algorithm: "NONE"
        trust:
          pem:
            cert-chain:
              resource.path: /etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem
        private-key:
          pem:
            key:
              resource.path: /var/certs/service/current/key.pem
            cert-chain:
              resource.path: /var/certs/service/current/cert-chain.pem
    admin:
      host: 127.0.0.1
      port: ${adminPort}
```

If the service uses Talon Secret Service V2 TLS rotation, the listener keeps the same Helidon `tls` owner but uses the `oci-ssv2` TLS manager. In that case the migrated values are the PKI secret path or PKI resource, trust bundle, and reload schedule:

```yaml
# config/helidon-server.yaml
server:
  port: ${serviceMtlsPort}
  host: 0.0.0.0
  tls:
    client-auth: "REQUIRED"
    endpoint-identification-algorithm: "NONE"
    manager:
      oci-ssv2:
        reload.expression: "0 0/30 * * * ? *"
        pki.secret-path: /secret/${serviceName}/mtls/server/latest
        trust.path: /etc/oci-pki/ca-bundle.pem
```

For OCI clients such as WFaaS and Kiev, reloadable TLS often moves to a named dynamic SSL context provider under `oci.dynamic-ssl-context-providers`. The consuming client then refers to that name with its own `dynamic-ssl-context-provider-name` property:

```yaml
oci:
  dynamic-ssl-context-providers:
  - name: workflow
    leaf-cert-path: /etc/oci-pki/workflow-leaf.pem
    leaf-cert-key-path: /etc/oci-pki/workflow-leaf.key
    leaf-cert-key-passphrase: ${workflowKeyPassphrase}
    intermediate-cert-path: /etc/oci-pki/workflow-intermediate.pem
    root-cert-path: /etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem
    duration: PT15M

  workflow:
    dynamic-ssl-context-provider-name: workflow
```

Heliport reports a frontier when reload intervals, cert bundle assembly, SNI, proxy protocol, HTTP/2, or mTLS semantics cannot be represented safely by the selected Helidon or Talon target.

| Dropwizard/HOCON property | Expected Helidon disposition |
| --- | --- |
| `applicationConnectors[].type: http` | Maps to the default WebServer listener when there is one clear application listener. |
| `applicationConnectors[].type: https` or `mtls` | Maps to a named WebServer socket with a `tls` block; exact TLS material is copied only when source fields map cleanly. |
| `port`, `bindHost`, `acceptorThreads`, `selectorThreads` | Ports and host usually migrate directly. Threading/tuning fields migrate only when Helidon has an equivalent target; otherwise they are reported as owner actions. |
| `certPath`, `keyPath`, `intermediatePath` | Maps to PEM private-key and certificate-chain configuration, to the Talon `oci-ssv2` TLS manager, or to a named dynamic SSL context provider depending on the target consumer. |
| `trustStoreLocation`, `trustStorePassword`, `rootCertPath`, `caBundle` | Maps to Helidon trust material or Talon/OCI TLS provider config when the consumer is known. |
| `sslAutoReloadPeriod` | Preserved as a dynamic TLS refresh setting only when the target provider supports reload. Otherwise Heliport records a TLS reload frontier. |
| `adminConnectors[].port` | Maps to a named admin socket when admin traffic remains separate; ambiguous admin behavior is reported. |
| `assets.resourcePath`, `assets.uriPath`, `indexFile` | Maps to Helidon static content only when the classpath/file root, URI prefix, welcome file, and cache behavior are clear. |

If the source service already used YAML and Heliport can safely merge target keys, it updates `src/main/resources/application.yaml`. For HOCON source services, Heliport will not create YAML shadows that override the original profile/include behavior.

## Java Configuration Consumers

Heliport inventories Java consumers of legacy config DTOs:

| Source pattern | Target owner |
| --- | --- |
| `Application<T>` root config passed into `run` | Helidon launch, `Config` injection, and service-registry ownership. |
| `ServiceConfiguration` subclasses | Typed Helidon config/service-registry providers while preserving source keys. |
| App-local nested config DTOs | Typed nested config providers or owner frontiers for custom construction. |
| `TypesafeConfigProvider`, `TypeSafeFileReader`, custom readers | Helidon config source or explicit bounded frontier. |
| `ConfigurationFactoryFactory` customizers | Helidon config/object-mapping ownership or frontier. |
| Dropwizard `ServerFactory` and connector consumers | Helidon WebServer listener/TLS config ownership or frontier. |
| Dropwizard assets config | Helidon WebServer static content config preserving route prefixes and index/resource paths. |
| Test `YamlConfigurationFactory`/`TypesafeConfigProvider` loaders | Helidon Config HOCON/object-mapping test fixtures. |

Example typed config migration:

**Legacy root config injection**

```java
final class FeatureConsumer {
    private final AppConfig config;

    FeatureConsumer(AppConfig config) {
        this.config = config;
    }

    boolean enabled() {
        return config.getFeatureConfiguration().isEnabled();
    }
}
```

**Helidon Config owner**

```java
import io.helidon.service.registry.Service;
import io.helidon.config.Config;

@Service.Singleton
final class FeatureConsumer {
    private final Config config;

    @Service.Inject
    FeatureConsumer(Config config) {
        this.config = config.get("feature");
    }

    boolean enabled() {
        return config.get("enabled").asBoolean().orElse(false);
    }
}
```

If a source DTO mutates values, derives defaults from multiple sources, or drives application policy, Heliport reports a config owner frontier instead of rewriting it as a plain property lookup.

For straightforward consumers, the migration will make the original key path and target key path obvious:

| Legacy Java read | Target Helidon read |
| --- | --- |
| `config.getServerFactory().getApplicationConnectors().get(0).getPort()` | `config.get("server.port").asInt()`, when the application listener maps to the default Helidon listener. |
| `config.getFeatureConfiguration().isEnabled()` | `config.get("feature.enabled").asBoolean()`, preserving the old default behavior if one was encoded in the DTO. |
| `config.getWorkflowEndpointConfiguration().getEndpoint()` | `config.get("oci.workflow.endpoint-details.server-endpoint").asString()`, preserving equivalent endpoint ownership when multiple clients exist. |
| `config.getSecretServiceConfig().getTlsConfig().getCaBundle()` | Moves to the Secret Service/Talon TLS config key that owns the client; direct lookup remains a frontier when the client construction is custom. |
| `config.getAssetsConfiguration().getUriPath()` | Moves to Helidon static content route config only when the resource root and cache/welcome behavior are known. |

### Dropwizard DataSize Config Cleanup

Dropwizard `DataSize` fields in configuration classes are runtime-neutralized when the initializer has a safe, source-evidenced unit. Heliport converts the field to a string value that Helidon config can carry without retaining Dropwizard utility types. Unsupported units and custom conversions remain visible as owner actions.

#### DataSize field defaults

Dropwizard config DTO:

```java
import io.dropwizard.util.DataSize;

public final class AppConfiguration {
    private DataSize bufferSize = DataSize.kilobytes(16);
    private DataSize maxPayloadSize = DataSize.megabytes(2);

    public DataSize getBufferSize() {
        return bufferSize;
    }
}
```

Helidon-compatible config DTO:

```java
public final class AppConfiguration {
    private String bufferSize = "16KiB";
    private String maxPayloadSize = "2MiB";

    public String getBufferSize() {
        return bufferSize;
    }
}
```

#### Local DataSize variables

Dropwizard config DTO:

```java
import io.dropwizard.util.DataSize;

void configure() {
    final DataSize maxFileSize = getMaxFileSize();
    DataSize totalSizeCap = getTotalSizeCap();
}
```

Helidon-compatible config DTO:

```java
void configure() {
    final var maxFileSize = getMaxFileSize();
    var totalSizeCap = getTotalSizeCap();
}
```

**Legacy DTO-backed client construction**

```java
final class DownstreamClientFactory {
    private final AppConfig config;

    DownstreamClientFactory(AppConfig config) {
        this.config = config;
    }

    DownstreamClient create() {
        ClientConfig client = config.getDownstreamClient();
        return DownstreamClient.builder()
                .endpoint(client.getEndpoint())
                .connectTimeoutMillis(client.getConnectTimeoutMillis())
                .readTimeoutMillis(client.getReadTimeoutMillis())
                .build();
    }
}
```

**Config-backed service construction**

```java
import io.helidon.config.Config;
import io.helidon.service.registry.Service;
import java.time.Duration;
import java.util.function.Supplier;

@Service.Singleton
final class DownstreamClientSupplier implements Supplier<DownstreamClient> {
    private final Config config;

    @Service.Inject
    DownstreamClientSupplier(Config config) {
        this.config = config.get("downstream-client");
    }

    @Override
    public DownstreamClient get() {
        return DownstreamClient.builder()
                .endpoint(config.get("endpoint").asString().get())
                .connectTimeout(config.get("connect-timeout")
                        .as(Duration.class).orElse(Duration.ofSeconds(5)))
                .readTimeout(config.get("read-timeout")
                        .as(Duration.class).orElse(Duration.ofSeconds(30)))
                .build();
    }
}
```

## OCI Config Key Transposition

OCI features preserve old source values while moving generic target keys toward Helidon OCI config.

**Legacy keys**

```
authConfig {
  serviceName: "email-api"
  tenantId: ${tenantId}
}
secretServiceConfig {
  endpoint: "https://secrets.{region}.oci.oraclecorp.com"
  secretPath: "/secret/service/client/latest"
  tlsConfig {
    caBundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
  }
}
kievConfiguration {
  storeName: "EmailStore"
  appName: "email-api"
}
workflowEndpointConfiguration {
  endpoint: "https://workflow.{region}.oci.oraclecorp.com"
  connectTimeoutMillis: 5000
  readTimeoutMillis: 30000
}
limitsClientConfig {
  endpoint: "https://limits.{region}.oci.oraclecorp.com"
  rootCertPath: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
}
metricsConfig {
  t2Config {
    project: "project"
    fleet: "fleet"
  }
}
```

**Target shape**

```yaml
helidon:
  oci:
    authentication-method: instance-principal
  oci-env:
    prefix: oci.env
  oci-secret-service:
    prefix: oci.ssv2
    client:
      endpoint: https://secret-service-ce.${oci.env.iaas-domain-name}/v1
      tls-config:
        ca-bundle: /etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem

oci:
  identity:
    authentication:
      global-business-unit: Cloud-Infra
      team-name: ExampleTeam
      application-name: email-api
      use-instance-principal: true
    authorization:
      service-name: email-api
  kiev:
    data-stores:
      - backend: "SERVICE"
        store-name: EmailStore
        app-name: email-api
        service:
          frontend-endpoint: https://kiev.{region}.oci.oraclecorp.com
          auth:
            type: "INSTANCE"
  workflow:
    domain-id: ${WORKFLOW_DOMAIN_ID}
    endpoint-details:
      server-endpoint: https://workflow.{region}.oci.oraclecorp.com
      connect-timeout: PT5S
      worker-read-timeout: PT30S
      poller-read-timeout: PT90S
    retry-policy:
      max-retry-count: 3
      delay-between-retry: PT0.25S
  limits:
    endpoint: https://limits.{region}.oci.oraclecorp.com
    client:
      connection-timeout: PT5S
      read-timeout: PT30S
  dynamic-ssl-context-providers:
    - name: service-mtls
      root-cert-path: /etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem
      duration: PT15M

metrics:
  publishers:
    - type: oci
      project: project
      fleet: fleet
      region: us-ashburn-1
      endpoint: https://telemetry-ingestion.{region}.oci.oraclecloud.com
```

Application-specific values are carried from source config. Heliport will not invent service names, region mappings, endpoints, secret paths, store names, or certificates.

## Server, Assets, And Static Content Config

Dropwizard `ServerFactory`, connector factories, and assets bundles are configuration behavior:

- Application and admin ports map to Helidon WebServer listener config when source values are clear.
- Heliport migrates source-evidenced listener ports and TLS material when Helidon WebServer or Talon TLS target config is proven; unproven mTLS, SNI, proxy-protocol, HTTP/2, URI compliance, and health-check connector behavior remain frontier evidence.
- Dropwizard assets mappings move to Helidon static content config only when route prefixes, index files, classpath/resource roots, and cache behavior are preserved.

| Source evidence | Expected result |
| --- | --- |
| One HTTP application connector with literal or property-backed port. | Populate the default Helidon listener `server.port` and preserve the property expression if the source used one. |
| Separate HTTP and HTTPS/mTLS application connectors. | Use the default listener plus a named socket, or two named sockets if the source names are operationally meaningful. |
| Separate admin connector used only for health/metrics/admin traffic. | Create a named admin socket and ensure health/metrics routes are bound as intended; report if route ownership is unclear. |
| Dropwizard assets bundle with `resourcePath`, `uriPath`, and `indexFile`. | Create Helidon static content config preserving classpath/file root, route prefix, welcome file, and cache behavior. |
| Custom connector factory, TLS reload listener, SNI selector, proxy-protocol handler, or Jetty-specific thread pool tuning. | Report an application-owner action with the exact fields and classes. Heliport does not collapse this behavior into a generic port migration. |

Example frontier wording generated from source evidence names the exact target:

```
Replace Dropwizard ServerFactory and connector config consumers with Helidon
WebServer listener/TLS config while preserving ports, connector types, SNI,
mTLS, proxy-protocol, and health-check semantics.
```

## Metrics

Heliport migrates Codahale and Dropwizard metrics toward Helidon declarative metrics and Helidon metric registry APIs.

Metrics have feature-specific build requirements. Helidon metrics annotations and imperative metrics APIs require `io.helidon.metrics:helidon-metrics-api` on the compile path. OCI/T2 publishing requires `com.oracle.helidon.oci.metrics:helidon-oci-metrics` and the selected OCI authentication module. Talon metrics prefix annotations require `com.oracle.helidon.oci:helidon-oci-codegen` during annotation processing. The Maven guide owns the concrete dependency and processor examples.

Existing service code that emits metrics through `com.oracle.pic.telemetry.commons.metrics.Metrics` is not rewritten to the Helidon metrics API. Those `emit`, `sensor`, and `record` calls continue to work when `helidon-oci-metrics` and an OCI publisher are configured; the Talon integration initializes and shuts down the OCI metrics runtime. Direct application ownership of `Metrics.init(...)`, `Metrics.shutdown()`, reporter construction, or other bootstrap lifecycle remains migration or frontier work. This compatibility path is distinct from the Codahale and Dropwizard APIs below, which Heliport can migrate when their names and lifecycle are clear.

### Annotation Mapping

| Dropwizard/Codahale | Helidon |
| --- | --- |
| `@Timed("name")` | `@Metrics.Timed("name")` |
| `@Counted(name = "name")` | `@Metrics.Counted("name")` |
| `@Gauge(name = "name")` on numeric method | `@Metrics.Gauge("name")` |
| `@Metered` | Closest supported target is usually `@Metrics.Counted`; report if semantics matter. |
| `@ExceptionMetered` | Remove or report manual counter work; no direct automatic equivalent. |

**Before**

```java
import com.codahale.metrics.annotation.Timed;

@Timed("listItems")
public List<Item> list() {
    return service.list();
}
```

**After**

```java
import io.helidon.metrics.api.Metrics;

@Metrics.Timed("listItems")
public List<Item> list() {
    return service.list();
}
```

### Programmatic Metrics

Programmatic `MetricRegistry` calls, including counters, timers, meters, histograms, and gauges, are migrated only when the name and lifecycle are clear.

**Dropwizard/Codahale**

```java
private final MetricRegistry metrics;

void complete(int bytes) {
    metrics.counter(MetricRegistry.name(Job.class, "complete")).inc();
    metrics.histogram(MetricRegistry.name(Job.class, "bytes")).update(bytes);
}
```

**Helidon target**

```java
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.DistributionSummary;
import io.helidon.metrics.api.MeterRegistry;

private final MeterRegistry meterRegistry;

void complete(int bytes) {
    meterRegistry.getOrCreate(
            Counter.builder("job.complete"))
            .increment();
    meterRegistry.getOrCreate(
            DistributionSummary.builder("job.bytes"))
            .record(bytes);
}
```

Dynamic metric names, external dashboard contracts, or custom reporter behavior are reported with the metric name and source path when Heliport cannot prove a safe equivalent.

### OCI Metrics Publisher

OCI/T2 metrics move to a Helidon metrics publisher when source config maps cleanly:

```yaml
metrics:
  publishers:
    - type: oci
      project: ${T2_PROJECT}
      fleet: ${T2_FLEET}
      endpoint: ${T2_ENDPOINT}
      region: ${OCI_REGION}
```

### Automatic HTTP And JVM Metrics

When `helidon-oci-metrics` is present and an OCI publisher is configured, Talon also registers automatic HTTP request metrics for generated endpoint methods and built-in JVM gauges. Customers will see Heliport preserve the publisher settings that affect those names and filters rather than treating them as custom reporter code.

```yaml
metrics:
  publishers:
    - type: oci
      project: ${T2_PROJECT}
      fleet: ${T2_FLEET}
      region: ${OCI_REGION}
      enable-detailed-timing-auto-metrics: true
      resource-package-prefix: com.example.orders.rest
      duration-unit: milliseconds
      metrics-scope-name: orders
      excludes:
        - "orders\\.jvm\\..*"
      filter-matching-mode: regex
```

Automatic HTTP metrics use service-core-style names such as `<scope>.Time`, `<scope>.ResourceTime`, response status counts, response family counts, response count, and success rate. The default scope is the generated endpoint class and method name. `@MetricPrefix` and `@SecondaryMetricPrefix` can preserve legacy endpoint metric scopes and require `helidon-oci-codegen` in annotation processing.

```java
import java.util.List;

import com.oracle.helidon.oci.metrics.MetricPrefix;
import com.oracle.helidon.oci.metrics.SecondaryMetricPrefix;
import io.helidon.http.Http;

@MetricPrefix(value = "Orders", appendMethodName = true)
@SecondaryMetricPrefix("OrdersAll")
final class OrdersEndpoint {
    @Http.GET
    @Http.Path("/orders")
    List<Order> list() {
        return service.list();
    }
}
```

JVM gauges use `metrics-scope-name` as their root segment, for example `orders.jvm.memory.heap.used` and `orders.jvm.threads.count`. Include and exclude filters can suppress these meters when the service already has an external JVM metrics contract.

Legacy `ServiceConfigurator` metrics wiring, `MetricsModules` bootstrap, custom reporter startup, payload behavior, and T2 compatibility wrappers remain frontiers unless an exact OCI Helidon metrics target owns them.

### Dropwizard Environment Metrics

Calls through `environment.metrics()` are operational behavior:

```
environment.metrics().register("queue.size", gauge);
environment.metrics().counter("requests").inc();
```

Heliport migrates direct counters, timers, meters, histograms, and gauges when it can prove names and lifecycle. It does not restore Codahale dependencies just to keep a logging metrics hook compiling.

## Logging

Heliport handles two distinct logging surfaces:

1. Build/runtime logging dependencies and Helidon log initialization.
2. Dropwizard logging factory/appender configuration behavior.

Logging also has feature-specific build requirements, but there is no single logging dependency set that applies to every service. A Helidon launcher that uses `LogConfig` and a JUL `logging.properties` file normally needs `io.helidon.logging:helidon-logging-jul`. Services that intentionally keep SLF4J, Logback, or custom bridge behavior keep the matching artifacts. Heliport cleans known bad bridge cycles, but ambiguous logging backend intent is reported instead of silently changing the service's logging pipeline.

### Helidon Log Initialization

When source still calls Helidon logging runtime configuration, Heliport ensures class initialization is present:

**Before**

```java
public class Main {
    public static void main(String[] args) {
        LogConfig.configureRuntime();
    }
}
```

**After**

```java
import io.helidon.logging.common.LogConfig;

public class Main {
    static {
        LogConfig.initClass();
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();
    }
}
```

### Dropwizard Logging Factories

Dropwizard logging factories and appenders can encode behavior that is not just dependency noise:

- custom JSON fields
- async appender behavior
- custom layout and filter factories
- smart-log or SPLAT appender configuration
- test fixture serialization/deserialization of logging config.

Heliport reports these as logging config owner work unless it has a safe Helidon logging config or service-registry provider target.

Example action target:

```
Move Dropwizard logging factory/appender configuration providers to Helidon
logging config or explicit service-registry logging providers while preserving
levels, appenders, layouts, async behavior, and custom JSON keys.
```

## Maven Logging Governance

Maven governance classifies logging bridges and bindings. Known bad Heliport-added bridge cycles can be cleaned deterministically, but original application logging intent is often ambiguous. In that case Heliport reports an owner action rather than rewriting all logging dependencies to fit a generic pattern.

## Validation Evidence

Configuration and observability findings appear in:

```
.heliport/migration-report.md
.heliport/application-action-plan.json
.heliport/evidence/maven-governance-audit.json
.heliport/evidence/
```

The customer report distinguishes:

- completed config/observability migrations
- customer action items that require application knowledge
- validation proof failures
- maintainer-only telemetry such as raw source signatures or compatibility inventories.

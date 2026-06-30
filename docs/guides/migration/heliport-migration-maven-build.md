# Maven, Dependency, and Plugin Migration

Heliport treats Maven as a migration surface, not just a way to run tests. A Helidon source tree is not complete if the POMs still depend on retired Dropwizard/Jersey/Jetty runtime ownership, have stale annotation processors, or hide dependency intent behind literal versions and unmanaged transitive artifacts.

Maven work happens in two places:

1. The early `dropwizard-build` and `helidon-se-baseline` work establishes the Helidon build shape needed by later source recipes.
2. The pre-validation Maven governance boundary normalizes POMs after runtime family waves have added all known Helidon, OCI, generated-source, and test dependencies.

## Build Baseline

The early build waves move a Dropwizard Maven project toward a Helidon 4.5 SE shape:

- Java 25 compiler release and toolchain compatibility at a high level. The recommended customer path is to complete Java/JDK migration before Heliport and use Heliport validation to verify the Helidon migration baseline.
- Helidon SE WebServer, declarative HTTP, JSON, media, validation, config, and service-registry dependencies where source evidence needs them.
- Helidon logging implementation for modules that Heliport makes runnable with `io.helidon.Main`.
- Generated OpenAPI contract owner dependencies.
- Jakarta validation API in place of legacy `javax.validation` ownership.
- Lombok annotation processing for modules that use Lombok under Java 25.
- OCI Helidon artifacts only when source/config/build evidence identifies a feature target.

The baseline is deliberately not allowed to migrate application-owned source logic. It prepares the Maven project so later family waves can do source migration with parseable classpaths and deterministic postconditions.

Helidon and Talon feature modules are selected by evidence. Importing BOMs does not mean every feature artifact is added directly. A direct dependency appears only when the migrated source or configuration needs that feature's APIs, generated metadata, runtime provider, config source, server feature, or service-registry binding. For services that own `module-info.java`, the same evidence rule applies to `requires` entries. This is the same rule shown by HOCON support: `helidon-config-hocon` appears only when HOCON config remains part of the migrated runtime.

The build changes are traceable back to source or configuration evidence. Typical examples:

| Evidence Heliport finds | Build change customers will see |
| --- | --- |
| Source now contains `@Service.Singleton`, `@Service.Inject`, generated service descriptors, or `Supplier<T>` service-registry providers. | Add Helidon service-registry/runtime dependencies and configure the Helidon APT bundle in the compiler plugin. |
| Resources are migrated to `@RestServer.Endpoint`, Helidon HTTP method annotations, `ServerRequest`, or `ServerResponse`. | Add Helidon WebServer/declarative HTTP dependencies and remove stale Jersey server dependencies only after source no longer needs them. |
| Runtime config still loads `config/*.conf` through HOCON. | Add `helidon-config-hocon` and preserve the configured HOCON source in meta-config. |
| OpenAPI generated sources or generated model helpers remain part of compile/test. | Keep generated-source dependencies and annotation processing needed by generated contracts; do not prune Jackson/model dependencies prematurely. |
| Kiev, WFaaS, Identity, Request ID, Metrics, Secret Service, Limits, or SPLAT consumer evidence appears in source/config. | Add only the matching Helidon OCI/Talon feature artifacts. Provider/source-owner modules are not given client artifacts from namespace evidence alone. |
| Jetty, Servlet, Netty, Jersey client, or SDK provider code remains source-referenced as a compatibility/frontier shape. | Keep the required dependency and report the frontier instead of deleting the dependency for cosmetic cleanliness. |

## Dependency Management

Maven governance reviews root and child POMs using structured POM evidence. It does not remove a dependency only because its name looks old. It asks:

- Is the dependency directly used by source, generated source, tests, or a retained provider/client compatibility edge?
- Is the dependency only a stale managed row, an aggregator dependency, or a duplicate of an imported BOM?
- Does the child POM inherit root dependency management?
- Is the version governed by a root property or inherited management?
- Does an OCI/Talon feature bring Talon-owned transitive dependencies that must be preserved, or a non-Talon conflict that still needs Maven governance?

Governance commonly applies these changes:

- Import Helidon and OCI BOMs in root dependency management.
- Preserve Talon-owned transitive dependencies brought by selected OCI/Talon feature artifacts; do not report, prune, or exclude them as stale migration residue.
- Replace literal versions with root properties, following the application's existing property style.
- Remove inline child dependency versions when root management applies.
- Remove invalid `<scope>` values from managed library rows while preserving BOM import scopes.
- Remove unused Jersey, Jetty, Netty, Dropwizard, and test-provider management rows when no source/build evidence remains.
- Remove stale direct dependencies such as Guava when source no longer imports the library.
- Keep or report source-referenced legacy dependencies as compatibility or action items instead of deleting them.

### Parent And BOM Shape

**Before**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.oracle.pic.sfw</groupId>
      <artifactId>oci-internal-bom</artifactId>
      <version>${oci-internal-bom-version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-server</artifactId>
      <version>11.0.22</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**After**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.helidon</groupId>
      <artifactId>helidon-dependencies</artifactId>
      <version>${helidon.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>com.oracle.helidon.oci</groupId>
      <artifactId>helidon-oci-dependencies</artifactId>
      <version>${helidon.oci.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Stale Jetty and Jersey management rows are removed only when they no longer serve source, generated-source, test, or retained compatibility behavior.

### Direct Dependency Versions

**Before**

```xml
<dependency>
  <groupId>io.helidon.webserver</groupId>
  <artifactId>helidon-webserver</artifactId>
  <version>4.5.0</version>
</dependency>
<dependency>
  <groupId>org.reflections</groupId>
  <artifactId>reflections</artifactId>
  <version>0.10.2</version>
</dependency>
```

**After**

```
<!-- Version supplied by imported Helidon BOM. -->
<dependency>
  <groupId>io.helidon.webserver</groupId>
  <artifactId>helidon-webserver</artifactId>
</dependency>

<!-- Non-BOM version promoted to a root property. -->
<dependency>
  <groupId>org.reflections</groupId>
  <artifactId>reflections</artifactId>
  <version>${version.reflections}</version>
</dependency>
```

External-parent modules that do not inherit root management may keep direct versions or receive local properties. Heliport does not assume sibling dependency management applies to them.

## Core Helidon Feature Dependencies

Several Helidon SE modules appear often enough that customers will recognize them as feature evidence, not as a generic starter bundle:

| Evidence Heliport finds | Build change customers will see |
| --- | --- |
| Source contains `@Service.Singleton`, `@Service.Inject`, named services, lifecycle annotations, generated services, or service-registry suppliers. | Add `io.helidon.service:helidon-service-registry` and configure `io.helidon.bundles:helidon-bundles-apt` annotation processing. |
| Source contains Helidon declarative endpoints, `ServerRequest`, `ServerResponse`, WebServer features, or generated REST resources. | Add `io.helidon.webserver:helidon-webserver` and the service-registry/annotation-processing setup needed for generated endpoint metadata. |
| Endpoint bodies or generated models require JSON binding. | Add `io.helidon.http.media:helidon-http-media-json-binding` and `io.helidon.json:helidon-json-binding` when source or generated-source evidence needs JSON media support. |
| Application config is loaded from YAML, HOCON, meta-config, or OCI config sources. | Add the matching config source module, such as `io.helidon.config:helidon-config-yaml` or `io.helidon.config:helidon-config-hocon`, only for the formats that remain runtime sources. |
| Source uses Bean Validation annotations on endpoint parameters, request models, or generated resources. | Add `io.helidon.validation:helidon-validation` and `io.helidon.webserver:helidon-webserver-validation` when validation is part of the request path. |
| Tests or retained application code create Helidon HTTP clients. | Add `io.helidon.webclient:helidon-webclient-http1` or the matching Helidon WebClient module only where client source evidence remains. |
| Tests migrate from Dropwizard/Jersey fixtures to Helidon WebServer tests. | Add `io.helidon.webserver.testing.junit5:helidon-webserver-testing-junit5` for Helidon server test fixtures. |

## Plugins And Annotation Processing

Helidon declarative services require annotation processing. Governance normalizes old or partial Helidon processor setup into the canonical APT bundle when the module uses Helidon service registry.

This is the build side of Helidon Injection. Guice discovers bindings from modules at runtime. Helidon Service Registry relies on generated metadata for services, suppliers, named qualifiers, endpoints, health checks, and other injectable components. If the migrated source contains Helidon service annotations but the compiler plugin does not run the Helidon APT bundle, the application may compile partially or fail to start because services are not discoverable.

| Migrated source evidence | Why annotation processing is needed |
| --- | --- |
| `@Service.Singleton`, `@Service.Named`, `@Service.Inject` | Generates service descriptors so Helidon can construct and wire the service. |
| `@RestServer.Endpoint` with Helidon HTTP annotations | Generates endpoint metadata used by the declarative WebServer integration. |
| `Supplier<T>` service converted from a Guice provider | Registers the supplier as the owner for the produced type and named qualifier. |
| Generated OpenAPI service classes or header functions annotated as services | Keeps generated endpoint/header behavior discoverable after source regeneration. |

**Before**

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>25</release>
    <proc>full</proc>
    <annotationProcessorPaths>
      <path>
        <groupId>io.helidon.service</groupId>
        <artifactId>helidon-service-codegen</artifactId>
        <version>${helidon.build-tools.version}</version>
      </path>
      <path>
        <groupId>io.helidon.codegen</groupId>
        <artifactId>helidon-codegen-apt</artifactId>
        <version>${helidon.build-tools.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

**After**

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>${maven.compiler.release}</release>
    <annotationProcessorPaths>
      <path>
        <groupId>io.helidon.bundles</groupId>
        <artifactId>helidon-bundles-apt</artifactId>
        <version>${helidon.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Governance also promotes plugin versions, such as Surefire, into root properties when that matches project style. Nested tool-plugin configuration versions are not blindly rewritten.

## Jetty And Netty

Jetty and Netty are handled by source evidence.

| Case | Heliport behavior |
| --- | --- |
| Unreferenced managed Jetty/Netty rows | Remove from dependency management before validation. |
| Unreferenced direct Netty dependency | Remove from the child POM and record the applied governance finding. |
| Source-referenced Netty transport type, such as `ChannelHandlerContext` | Keep dependency and report review-required action before validation. |
| Simple Jetty wrapper that only launched Dropwizard infrastructure | Retire wrapper in Dropwizard bootstrap wave and remove POM rows when no Jetty source remains. |
| Custom Jetty connector, TLS reload, SNI, HTTP/2, proxy-protocol, JMX, or metrics bridge | Classify as a protected operational frontier until Helidon WebServer target behavior is explicit. |

Example Netty disposition:

**Unreferenced**

```xml
<dependency>
  <groupId>io.netty</groupId>
  <artifactId>netty-handler</artifactId>
  <version>4.1.133.Final</version>
</dependency>
```

Removed when source no longer imports Netty.

**Source-referenced**

```java
import io.netty.channel.ChannelHandlerContext;

final class NettyCarrier {
    ChannelHandlerContext context;
}
```

Kept and reported for review because the source still owns Netty behavior.

## Jackson

Jackson is intentionally evidence-based:

- Generated model and application source that still require Jackson keep the needed dependencies.
- Direct Jackson versions are deferred to enforcer or dependency evidence when normalizing them too early would hide the real owner.
- Dropwizard Jackson compatibility shims are materialized only when source or unit-test evidence needs a retired Dropwizard Jackson class, such as `FuzzyEnumModule` or `GuavaExtrasModule`.
- Stale shim files are removed when no source or proof gate references them.

Heliport does not remove Jackson only because the target is Helidon. Helidon uses Jackson through JSON binding and generated model surfaces, and premature Jackson pruning can break generated model and validation paths.

## Metrics And Logging Dependencies

Metrics and logging follow the same evidence rule as other Helidon modules, but they are common enough to call out separately.

| Evidence Heliport finds | Build change customers will see |
| --- | --- |
| Source uses Helidon metrics annotations such as `@Metrics.Timed`, imperative `MeterRegistry`, `Counter`, `Timer`, or `Metrics.globalRegistry()`. | Add `io.helidon.metrics:helidon-metrics-api`. This is compile-time API evidence, separate from where metrics are published. |
| Source/config uses OCI/T2 metrics publishing through `metrics.publishers[].type = oci` or migrated OCI metrics bootstrap. | Add `com.oracle.helidon.oci.metrics:helidon-oci-metrics` and the OCI auth feature module required by the selected authentication method. The integration supplies the OCI publisher/provider behavior, automatic HTTP metrics, and JVM gauges; Heliport still preserves project, fleet, region, endpoint, dimensions, and filter settings from source config. |
| Source uses Talon metrics endpoint-prefix annotations such as `@MetricPrefix` or `@SecondaryMetricPrefix`. | Add `com.oracle.helidon.oci:helidon-oci-codegen` to annotation processing, alongside the normal Helidon APT setup. |
| The migrated launcher initializes Helidon logging with `LogConfig`, or `logging.properties` uses Helidon JUL handlers or formatters. | Add the Helidon logging implementation artifact for the selected backend, usually `io.helidon.logging:helidon-logging-jul`. Logging backend, bridge, and formatter dependencies are selected from source/runtime evidence. |
| Source still uses SLF4J, Logback, custom appenders, Dropwizard logging factories, or logging bridge behavior. | Keep or add only the required bridge/binding/backend artifacts and report ambiguous logging intent as owner work. Heliport does not replace all logging with one generic dependency set. |

Lumberjack and Chainsaw are deployment/log-collection concerns rather than ordinary application feature dependencies. A service may need Helidon structured logging formatters so emitted logs are useful to Lumberjack, but onboarding and delivery policy remain environment and owner work.

## OCI Dependency Selection

OCI dependencies are role-aware. Heliport adds feature artifacts only when source/config/build evidence identifies a consumer shape:

- `helidon-oci-audit` for OCI AuditV2 WebServer filtering; add `helidon-oci-codegen` to annotation processing when endpoint methods inject `AuditPayloadAppender`.
- `helidon-oci-envconfig` when migrated configuration or clients rely on `oci-env`, `oci.env.*`, or runtime region/AD/fault-domain providers.
- `helidon-oci-errorcode` for shared OCI error-code API types, and `helidon-oci-errorcode-webserver` when Helidon owns rendering for `RenderableException` responses.
- `helidon-oci-workflow` for WFaaS client/module usage.
- `helidon-oci-kiev` for Kiev data-store and transaction ownership.
- `helidon-oci-identity` and auth modules for identity filters, auth providers, and authenticated endpoint ownership.
- `helidon-oci-identity-client` and `helidon-oci-sdk-object-storage-client` for public OCI SDK Identity and Object Storage clients created through the service registry.
- `helidon-oci-sdk-oci` plus exactly the authentication modules the configured methods can use, such as `helidon-oci-sdk-authentication-instance`, `helidon-oci-sdk-authentication-resource`, `helidon-oci-sdk-authentication-oke-workload`, or `helidon-oci-sdk-authentication-service-principal`.
- `helidon-oci-splat` for SPLAT mTLS validation on generated Helidon endpoints; provider/source-owner SPLAT code is not enough by itself.
- `helidon-oci-tagging` for local tag slug conversion and tagging-client injection.
- Exactly one of `helidon-oci-metering-cp` or `helidon-oci-metering-dp`, based on the metering role; CP also needs a `MappedDataStore` provider, often from `helidon-oci-kiev`.
- `helidon-oci-metrics`, `helidon-oci-limits`, `helidon-oci-secret-service-config-source`, `helidon-oci-secret-service-tls-manager`, `helidon-oci-request-id`, `helidon-oci-request-id-webserver`, and `helidon-oci-jaxrs` when their feature evidence is present.

Customers can read these dependencies as feature ownership, not as a blanket OCI migration bundle. If the source only contains custom OCI SDK clients, provider-side SPLAT implementation code, direct Secret Service lookup logic, or custom TLS/auth factories, Heliport preserves the required compatibility dependencies or reports owner work instead of adding unrelated Helidon OCI feature modules.

Once a Talon feature artifact is selected, its Talon-owned transitive dependency path is part of that feature's runtime contract. Heliport still governs non-Talon stale dependencies and conflicts, but it will not exclude or prune dependencies solely because they arrive through Talon.

Provider or source-owner modules are excluded from client artifact selection when namespace evidence alone is not enough. This prevents, for example, SPLAT implementation modules from receiving SPLAT client artifacts only because they contain provider package names.

## Reports And Evidence

Maven governance emits:

```
.heliport/evidence/maven-governance-audit.json
```

The audit records applied changes, skipped changes, ambiguous dependency intent, legacy dependency findings, and whether agent review is required before validation gates. Customer mode applies deterministic fixes automatically and turns ambiguous or frontier findings into specific action items instead of burying them inside compile failures.

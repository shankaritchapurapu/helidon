# Heliport Migration Guide

This guide explains what Heliport, the Codex skill for migrating OCI services to Helidon, changes when it moves a Dropwizard 3-based OCI service to Helidon 4.5 SE. It complements the guide and the generated `.heliport/migration-report.md`: the quick-start tells teams how to run Heliport, while this guide explains what code, build, configuration, and OCI integration surfaces Heliport will change or report.

## Guide Map

- [Maven, Dependency, and Plugin Migration](heliport-migration-maven-build.md)
- [Runtime Family Waves](heliport-migration-runtime-families.md)
- [Generated OpenAPI And Model Migration](heliport-migration-generated-openapi-model.md)
- [OCI Integration Migration](heliport-migration-oci-integrations.md)
- [Configuration, Metrics, and Logging](heliport-migration-configuration-observability.md)
- [Known Frontiers And Application-Owner Actions](heliport-migration-known-frontiers-owner-actions.md)

## Helidon Concepts Used In This Guide

The source applications are Dropwizard, Jersey/JAX-RS, Guice, Jetty, and OCI SDK based OCI service applications. The migrated applications are Helidon SE declarative OCI service applications using Helidon WebServer, declarative HTTP, Helidon Service Registry, and first-class OCI/Talon integrations. The examples in this guide use these Helidon concepts repeatedly:

| Helidon concept | What it replaces or owns | How to read it in migrated code |
| --- | --- | --- |
| `io.helidon.Main.main(args)` | Dropwizard `Application.run(...)` server bootstrap. | The service starts through Helidon runtime discovery instead of a Dropwizard `Environment`. |
| `@Service.Singleton` | Guice `@Singleton`, module-owned singleton bindings, and many provider-created service objects. | The annotated class is registered in Helidon Service Registry and can be injected into other services. |
| `@Service.Inject` | Guice constructor injection and provider method parameters. | Helidon constructs the service and supplies constructor parameters from the service registry. |
| `@Service.Named` | Guice `@Named` and named provider bindings. | Used when multiple services of the same Java type exist, such as worker and poller workflow clients. |
| `@Service.PostConstruct` and `@Service.PreDestroy` | Dropwizard `Managed.start()`/`stop()` and lifecycle registration. | Lifecycle methods run when Helidon creates or shuts down the service. |
| `ServerLifecycle.afterStart(WebServer)` | Event-independent Dropwizard server lifecycle listeners. | The listener is a Helidon service that runs after the WebServer starts. |
| `@RestServer.Endpoint` | Jersey resource registration through `Environment.jersey().register(...)`. | The class is a Helidon declarative HTTP endpoint. It is also usually a `@Service.Singleton`. |
| `io.helidon.http.Http.GET`, `POST`, `Path`, `Produces` | JAX-RS HTTP method and route annotations. | These annotations describe the route exposed by Helidon WebServer. |
| `ServerRequest` | Servlet request, JAX-RS `@Context` request objects, headers, query parameters, and route context. | Use it when the method needs request metadata that is not represented as a simple typed parameter. |
| `ServerResponse` | JAX-RS `Response`, servlet response, manual status/header/body writes. | Methods that take `ServerResponse` usually set status and headers explicitly, then call `send(...)`. |
| `Config` | Dropwizard configuration DTO reads when the key mapping is straightforward. | Helidon reads configuration from ordered sources such as system properties, environment variables, HOCON, and YAML overlays. |
| `Supplier<T>` service | Guice `@Provides` methods and provider classes. | The supplier is itself a Helidon service. Its `get()` method creates or returns the owned object. |

When a sample shows one of these APIs, read it as a change in ownership. Heliport is not only changing imports; it is moving server routing, injection, lifecycle, configuration, and OCI integration ownership from Dropwizard/Guice/Jersey scaffolding into Helidon runtime services.

## Feature Modules Are Evidence-Based

Helidon and Talon are intentionally modular. At the Java module level, feature APIs and runtime integrations live behind specific named modules; at the Maven level, those modules appear as specific feature artifacts. A migrated service will not receive every Helidon or Talon module in every POM. Heliport adds feature modules only when source, configuration, generated-source, or build evidence proves that the application uses the related feature.

Read feature names in this guide as ownership signals. For example, HOCON configuration requires HOCON support, Kiev data-store ownership requires the Kiev integration, WFaaS client ownership requires the Workflow integration, request-id server ownership requires the request-id webserver integration, OCI metrics publishing requires the OCI metrics integration, and Helidon JUL logging requires the Helidon JUL logging module. The drill-down pages name the relevant feature module so customers understand why a dependency appears, but the Maven guide owns concrete dependency and plugin examples.

If source evidence shows provider/source-owner code, custom transport, custom auth, custom TLS, or retained compatibility behavior instead of a standard feature consumer, Heliport does not add a feature module just to satisfy a package-name match. It keeps the necessary compatibility dependency or reports an application-owner action with the exact evidence.

## What Heliport Migrates

| Area | Migrated or reported by Heliport |
| --- | --- |
| Maven build | Java 25 compiler shape, Helidon and OCI BOM imports, Helidon runtime dependencies, annotation processing, generated-source dependencies, stale dependency classification and cleanup, and Maven governance evidence. |
| Generated OpenAPI | Generated server contracts, generated model shape, generated response/body/header semantics, enum/string boundaries, and test fixtures for generated resources and models. |
| Dropwizard | Application/bootstrap shells, environment registrations, lifecycle services, server lifecycle listeners, health checks, admin tasks, Jersey registrations, resource scaffolding, configuration consumers, static assets, object mapper ownership, and residual Dropwizard scaffold cleanup. |
| Guice | `@Inject`/`@Singleton`/qualifier migration, service-registry services, provider methods, supplier materialization, module shells, bindings, multibinders, injector call sites, and compatibility/frontier classification. |
| JAX-RS, Jersey, Servlet | Server resources, request and response carriers, headers, URI and request context, principal-only security context, immediate async responses, direct SSE sinks, streaming responses, exception mappers, filters, servlet adapters, route parity, generated contract handoff, and retained client/provider compatibility. |
| OCI integrations | Feature-specific Helidon OCI dependencies, config transposition, service-registry injection targets, filter/client/provider cleanup, and precise frontiers for app-owned transport, TLS, auth, policy, or domain behavior. |
| Configuration | HOCON/YAML/source-file preservation, Helidon meta-config, typed config/service-registry ownership, Dropwizard `DataSize` cleanup, test config loading, OCI config key transposition, static assets, server connector config, and owner frontiers where exact mapping is unsafe. |
| Metrics and logging | Codahale/Dropwizard metrics migration toward Helidon metrics, OCI metrics publisher config, Dropwizard logging factory/appender frontiers, Helidon `LogConfig` initialization, and Maven logging dependency governance. |
| Tests and validation | Dropwizard/Jersey/Guice test fixture migration where production ownership is known, generated-source/compile/unit/install proof gates, Maven governance audit, and customer action plans. |

## Family Wave Order

Heliport exposes coarse run phases, but runtime migration is implemented as ordered owner waves. A wave is complete only when its owned residue is migrated or classified.

| Owner wave | Primary migration responsibility |
| --- | --- |
| `dropwizard-build` | Establish Helidon build baseline, Java 25 compiler shape, generated-source prerequisites, Helidon dependencies, and safe early dependency governance. |
| `generated-resource-closeout` | Prepare generated OpenAPI resources and generated model owners before handwritten runtime conversion relies on them. |
| `guice-injection` | Move Guice service, provider, module, binding, and injector ownership toward Helidon service registry. |
| `lifecycle-health-task` | Convert Dropwizard `Managed`, health checks, lifecycle registration, and admin task behavior. |
| `dropwizard-bootstrap` | Retire Dropwizard `Application`, bootstrap, environment, object mapper, launcher, and simple Jetty wrapper ownership when safe. |
| `jaxrs-resource` | Convert app-owned JAX-RS/Jersey resources and resource registrations to Helidon endpoint/service shapes. |
| `request-response-carriers` | Migrate servlet/JAX-RS request, response, context, header, URI, auth, and filter carriers. |
| `request-response-carrier-promotions` | Promote previously protected request/response helpers when source evidence proves a generic Helidon target. |
| `jaxrs-runtime-promotions` | Drain remaining server-side JAX-RS runtime constants, response helpers, exception helpers, and route parity issues. |
| `jaxrs-client-closeout` | Close simple outbound JAX-RS client shapes and classify retained client/provider compatibility edges. |
| `oci-helidon` | Add and configure OCI Helidon integrations where behavior maps cleanly; classify custom OCI frontiers. |
| `service-registry-metadata` | Ensure Helidon service metadata and descriptors reflect migrated services and suppliers. |
| `test-migration` | Update tests after production owners are migrated or classified. |
| `dropwizard-scaffold-closeout` | Remove final Dropwizard scaffold helpers, resource registries, no-op launch code, and stale compatibility shells. |

Maven governance then runs as a pre-validation boundary. It normalizes POMs and classifies ambiguous dependencies before compile, unit, and install proof gates are treated as meaningful validation evidence.

## Example: Simple Resource

**Dropwizard / JAX-RS source**

```java
package example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ping")
public class PingResource {
    @GET
    public String ping(String ignored) {
        return "ok";
    }
}
```

**Helidon target**

```java
package example;

import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;
import io.helidon.http.Http.GET;
import io.helidon.http.Http.Path;

@Service.Singleton
@RestServer.Endpoint
@Path(value = "/ping")
public class PingResource {
    @GET
    public String ping() {
        return "ok";
    }
}
```

This shape is intentionally narrow: simple, app-owned server resources are converted. Outbound clients, provider SDK compatibility, generated resource contracts, and complex request/response carrier behavior are handled by their own waves or reported as frontiers.

## Example: Lifecycle Service

**Dropwizard lifecycle**

```java
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;

@Singleton
public final class QueueLifecycle implements Managed {
    private final Worker worker;

    @Inject
    public QueueLifecycle(Worker worker) {
        this.worker = worker;
    }

    @Override
    public void start() throws Exception {
        worker.start();
    }

    @Override
    public void stop() {
        worker.stop();
    }
}
```

**Helidon service lifecycle**

```java
import io.helidon.service.registry.Service;

@Service.Singleton
public final class QueueLifecycle {
    private final Worker worker;

    @Service.Inject
    public QueueLifecycle(Worker worker) {
        this.worker = worker;
    }

    @Service.PostConstruct
    public void start() throws Exception {
        worker.start();
    }

    @Service.PreDestroy
    public void stop() {
        worker.stop();
    }
}
```

Conditional lifecycle registration, ordering-sensitive startup, or external framework startup semantics are not silently deleted. Heliport either proves the service owns a Helidon lifecycle target or reports a lifecycle frontier.

## Example: Identity Annotation

**Legacy name binding**

```java
public class Endpoint {
    @GET
    @AuthRequired
    public String get() {
        return "ok";
    }
}
```

**Helidon OCI Identity**

```java
import com.oracle.helidon.oci.identity.Identity;

public class Endpoint {
    @GET
    @Identity.Authenticated
    public String get() {
        return "ok";
    }
}
```

When all targeted usages are migrated, the old JAX-RS `@NameBinding` annotation and matching filter registrations can be removed. If other code still depends on the annotation or custom authorization logic, Heliport preserves or reports that edge instead of guessing.

## Example: Build Governance

**Before governance**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-server</artifactId>
      <version>11.0.22</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>25</release>
    <annotationProcessorPaths>
      <path>
        <groupId>io.helidon.service</groupId>
        <artifactId>helidon-service-codegen</artifactId>
        <version>${helidon.build-tools.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

**After governance**

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
  </dependencies>
</dependencyManagement>

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

The real output depends on the existing POM hierarchy, inherited parent, effective dependency graph, source references, and generated/OCI features.

## What Heliport Does Not Hide

Heliport reports rather than silently rewrites:

- app-specific business logic, payload policy, or authorization policy
- service-specific endpoint, tenancy, realm, fleet, store, certificate, secret, or workflow names that cannot be inferred safely
- custom Jetty connector, TLS reload, SNI, HTTP/2, proxy-protocol, and JMX behavior until a Helidon WebServer target is proven
- servlet request-body replay, response compression, request-context `ThreadLocal`, and custom auth filters until a route-equivalent Helidon target is proven
- source-referenced Netty transport behavior
- Dropwizard logging factories or async appender behavior without a safe Helidon logging target
- Jackson dependencies when generated or application source still needs them
- retained SDK/client/provider compatibility edges that do not own server runtime behavior.
- application-specific Guice module policy, provider roots, and static injector helpers whose construction behavior is not source-evidenced enough to replace automatically.

The generated action plan is part of the migration, not an afterthought. A precise owner frontier is better than a broad source edit that compiles but silently drops behavior.

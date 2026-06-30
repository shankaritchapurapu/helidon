# Runtime Family Waves

Heliport runtime migration is organized by ownership. Each wave owns a framework family and leaves one of three outcomes for every detected code point: migrated, classified as compatibility, or reported as a protected owner frontier.

Compile and unit tests are not the first planned detector for these families. If structural Dropwizard, Guice, server-side JAX-RS/Jersey/Servlet, generated OpenAPI, OCI, or runtime-shape residue remains unclassified, validation must send the work back to the owning family wave before treating compile or unit gates as meaningful proof.

## How To Read Helidon Runtime Code

Helidon SE applications do not have a Dropwizard `Environment`, Jersey `ResourceConfig`, or Guice `Injector` as the central application object. Heliport moves that ownership into discovered Helidon services:

- A class annotated with `@Service.Singleton` is a Helidon Service Registry singleton. Other services can receive it through constructor injection.
- A class annotated with both `@Service.Singleton` and `@RestServer.Endpoint` is a singleton service and a WebServer endpoint.
- Constructor parameters annotated or resolved through `@Service.Inject` replace Guice constructor injection and many `@Provides` method parameters.
- `@Service.Named` distinguishes same-type services, such as multiple workflow clients, stores, or credential providers.
- `@Service.PostConstruct` and `@Service.PreDestroy` replace lifecycle registration when the old `Managed` behavior is safe to own directly.
- `ServerLifecycle.afterStart(WebServer)` replaces event-independent Dropwizard server lifecycle listeners.
- `ServerRequest` and `ServerResponse` are Helidon WebServer request/response carriers. They are not Jersey `ContainerRequestContext` or servlet wrappers.

When Heliport cannot prove that a Dropwizard, Guice, Jersey, or Servlet behavior has one of those Helidon owners, it will leave a compatibility classification or an application-owner action instead of making a broad source edit.

## Family Matrix

| Family | Phase or wave | Migrated shapes | Classified shapes |
| --- | --- | --- | --- |
| Build/runtime baseline | `dropwizard-build` plus early `helidon-se-baseline` prerequisite work | Java 25, Helidon dependencies, service registry, generated-source prerequisites, logging implementation, and dependency governance. | Build tool gaps, unresolved generated-source prerequisites, ambiguous dependency intent. |
| Generated OpenAPI/model | `generated-resource-closeout`, `post-runtime-generated-alignment` | Generated server resources, model builders, enum/string boundaries, collection/temporal helpers, response body/status/header carriers, generated test fixtures. | Template-owned residuals, retained generated compatibility helpers, source drift requiring owner review. |
| Guice/service registry | `guice-injection`, `service-registry-metadata` | `@Inject`, singleton services, providers, suppliers, module shells, bindings, multibinders, injector call sites, qualifiers, lifecycle provider methods. | Provider/client compatibility, app-specific module policy, dynamic provider factories, unproven custom scopes. |
| Dropwizard lifecycle/health/task | `lifecycle-health-task` | `Managed`, event-independent server lifecycle listeners, health checks, lifecycle registration, health check registration, bounded admin tasks, paired tests. | Conditional lifecycle ordering, servlet-coupled tasks, task-name API tests, external startup policy. |
| Dropwizard bootstrap/scaffold | `dropwizard-bootstrap`, `dropwizard-scaffold-closeout` | `Application` shells, `Bootstrap`/`Environment` helpers, object mapper ownership, resource registries, no-op launch scaffolding, simple Jetty wrappers. | Custom Jetty/listener/TLS/JMX behavior, server config consumers, static asset mapping requiring exact target config. |
| JAX-RS/Jersey/Servlet server runtime | `jaxrs-resource`, `request-response-carriers`, `request-response-carrier-promotions`, `jaxrs-runtime-promotions` | Endpoint annotations, resource registrations, request/response/header/context carriers, principal-only security context, immediate async responses, direct SSE sinks/builders, exception mappers, filters, route parity, promoted helper classes. | SDK/client/provider JAX-RS compatibility, complex servlet adapters, custom auth filters, protected request/response carriers. |
| JAX-RS clients | `jaxrs-client-closeout` | Simple `ClientBuilder.newClient()` GET clients and closeout-safe response entity readers. | Custom client config, filters, TLS, retries, SDK provider surfaces, downstream compatibility. |
| OCI Helidon | `oci-helidon` | First-class OCI Helidon dependencies, config transposition, service-registry injection, filter/helper cleanup where target behavior is known. | App-owned auth, policy, transport, TLS, payload, tenancy, endpoint, and domain behavior. |
| Tests | `test-migration`, validation closeout | Tests for migrated production owners, generated model/resource fixtures, direct health/task/resource tests, Helidon server-test setup. | Tests whose production subject is still a frontier, Dropwizard API assertions with no Helidon target. |

## Dropwizard

### Application And Bootstrap

Heliport retires Dropwizard bootstrap once source evidence proves the behavior has a Helidon owner:

- `Application<T>` launchers move toward `io.helidon.Main.main(args)`.
- `Bootstrap` helpers are removed after their owned config, object mapper, health, lifecycle, or resource-registration behavior has moved.
- `Environment.jersey().register(...)` and resource class registries are drained after resources are Helidon endpoints.
- `Environment.getObjectMapper()` and validation wiring are moved to Helidon JSON/config ownership when the target shape is known.
- Simple Jetty wrappers that only carried Dropwizard runtime startup are removed.

Heliport does not flatten all Dropwizard server config. If a service has custom Jetty connector factories, TLS reload, SNI, mTLS, proxy-protocol, HTTP/2, JMX, or health-check connector behavior, the report names the exact owner frontier.

A completed bootstrap migration will make it clear where each old registration went:

**Dropwizard application registration**

```java
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;

public final class EmailApi extends Application<EmailConfig> {
    @Override
    public void run(EmailConfig config, Environment environment) {
        environment.jersey().register(
                new SenderResource(config.getSenderConfig()));
        environment.lifecycle().manage(
                new QueueLifecycle(config.getQueueConfig()));
        environment.healthChecks().register(
                "kiev", new KievHealthCheck(config.getKievConfiguration()));
        environment.admin().addTask(
                new DeepcheckTask(config.getDeepcheckConfig()));
        environment.getObjectMapper()
                .registerModule(new JavaTimeModule());
    }
}
```

**Helidon-owned source shape**

```java
import io.helidon.Main;
import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.http.Http.Path;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

public final class EmailApiMain {
    public static void main(String[] args) {
        Main.main(args);
    }
}

@Service.Singleton
@RestServer.Endpoint
@Path("/senders")
final class SenderResource {
    @Service.Inject
    SenderResource(SenderConfig config) {
        this.config = config;
    }
}

@Service.Singleton
final class QueueLifecycle {
    @Service.PostConstruct
    void start() {
        worker.start();
    }
}

@Service.Singleton
final class KievHealthCheck implements HealthCheck {
    public HealthCheckResponse call() {
        return checkKiev();
    }
}
```

The old `Environment` calls are removed only after the endpoint, lifecycle service, health check, task, object mapper/config behavior, and tests have a Helidon owner or a recorded frontier. If one registration is still app-owned, Heliport will leave an action item that names that exact registration rather than deleting the whole bootstrap class blindly.

### Lifecycle

**Dropwizard**

```java
import io.dropwizard.lifecycle.Managed;

public final class Poller implements Managed {
    @Override
    public void start() {
        startAsync();
    }

    @Override
    public void stop() {
        stopAsync();
    }
}
```

**Helidon**

```java
import io.helidon.service.registry.Service;

@Service.Singleton
public final class Poller {
    @Service.PostConstruct
    public void start() {
        startAsync();
    }

    @Service.PreDestroy
    public void stop() {
        stopAsync();
    }
}
```

Registration calls such as `environment.lifecycle().manage(...)` are removed only when the service is already Helidon-owned and the registration is not inside an ordering-sensitive branch, loop, switch, or try block.

### Server Lifecycle Listeners

Event-independent Dropwizard server lifecycle listeners migrate to Helidon `ServerLifecycle` services. Heliport preserves listeners that read Jetty-specific connectors, sessions, servlet context state, or ordering-sensitive runtime data as owner frontiers.

**Dropwizard server lifecycle listener**

```java
import io.dropwizard.lifecycle.ServerLifecycleListener;
import org.eclipse.jetty.server.Server;

public final class StartupListener implements ServerLifecycleListener {
    @Override
    public void serverStarted(Server server) {
        warmCache();
    }
}
```

**Helidon server lifecycle service**

```java
import io.helidon.service.registry.Service;
import io.helidon.webserver.ServerLifecycle;
import io.helidon.webserver.WebServer;

@Service.Singleton
public final class StartupListener implements ServerLifecycle {
    @Override
    public void afterStart(WebServer server) {
        warmCache();
    }
}
```

### Health Checks

**Dropwizard / Codahale**

```java
import com.codahale.metrics.health.HealthCheck;

public final class StoreHealthCheck extends HealthCheck {
    @Override
    protected Result check() throws Exception {
        if (healthy()) {
            return Result.healthy();
        }
        return Result.unhealthy("store is down");
    }
}
```

**Helidon health**

```java
import io.helidon.service.registry.Service;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheck;

@Service.Singleton
public final class StoreHealthCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        if (healthy()) {
            return HealthCheckResponse.builder()
                    .status(HealthCheckResponse.Status.UP)
                    .build();
        }
        return HealthCheckResponse.builder()
                .status(HealthCheckResponse.Status.DOWN)
                .detail("message", "store is down")
                .build();
    }
}
```

The health wave also updates direct health-check tests from `check()` and `Result` assertions to `call()` and `HealthCheckResponse` assertions.

### Admin Tasks

Bounded `Task` subclasses become Helidon endpoints while preserving the old `execute(Map<String, List<String>>, PrintWriter)` body for direct tests.

**Dropwizard task**

```java
public class SimpleAdminTask extends Task {
    public SimpleAdminTask() {
        super("deepcheck");
    }

    @Override
    public void execute(Map<String, List<String>> args, PrintWriter out) {
        out.println("ok");
    }
}
```

**Helidon endpoint**

```java
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;
import io.helidon.http.Http.POST;
import io.helidon.http.Http.Path;
import io.helidon.http.Http.Produces;
import io.helidon.webserver.http.ServerRequest;

@RestServer.Endpoint
@Service.Singleton
@Path("/tasks/deepcheck")
public class SimpleAdminTask {
    @POST
    @Produces(value = "text/plain")
    public String execute(ServerRequest request)
            throws Exception {
        java.io.StringWriter writer = new java.io.StringWriter();
        java.io.PrintWriter output = new java.io.PrintWriter(writer);
        execute(request.query().toMap(), output);
        output.flush();
        return writer.toString();
    }

    public void execute(Map<String, List<String>> args, PrintWriter out) {
        out.println("ok");
    }
}
```

Servlet-coupled tasks, tasks relying on inherited Dropwizard task APIs, or tasks whose name/authorization is computed through app-specific policy remain frontiers.

## Guice

The Guice wave moves application composition to Helidon Service Registry. A Guice module says how to build objects at injector creation time. A Helidon service says that the class itself is discoverable, injectable, and optionally named. This means the target code usually has fewer central module files and more constructor-injected service classes.

Supported shapes include:

- `@Inject`, `@Singleton`, and named qualifier normalization.
- Constructor-injected service singleton promotion.
- Provider method migration to generated or existing `Supplier<T>` services.
- Direct constructor provider wrappers.
- Service registry metadata and descriptors.
- Module shell cleanup after all bindings are migrated.
- Multibinder and lifecycle/provider method drains when source evidence proves the target service set.
- Injector and `Guice.createInjector(...)` cleanup when the call site no longer owns runtime behavior.
- Safe `Injector.getInstance(Key.get(...))`, named static injector lookups, and concrete `TypeLiteral` lookups when the requested contract is known.

### Provider To Supplier

Helidon does not need a Guice module method for every produced object. When the produced object is not itself a service class, Heliport often creates a service-registry `Supplier<T>`. This is the closest Helidon shape to a simple Guice `@Provides` method.

**Guice provider**

```java
public class AppModule extends AbstractModule {
    @Provides
    @Singleton
    @Named("clock")
    Clock provideClock() {
        return Clock.systemUTC();
    }
}
```

**Helidon service-registry supplier**

```java
import io.helidon.service.registry.Service;
import java.time.Clock;
import java.util.function.Supplier;

@Service.Singleton
@Service.Named("clock")
final class ClockSupplier implements Supplier<Clock> {
    private final Clock value;

    ClockSupplier() {
        this.value = Clock.systemUTC();
    }

    @Override
    public Clock get() {
        return value;
    }
}
```

For runtime clients, Heliport uses a lazy supplier when client construction must not move to application startup unless the source already owned eager startup:

```java
import io.helidon.service.registry.Service;

@Service.Singleton
final class WidgetClientSupplier implements Supplier<WidgetClient> {
    private final Supplier<WidgetClient> value;

    WidgetClientSupplier(Endpoint endpoint) {
        this.value = () -> WidgetClient.builder().endpoint(endpoint).build();
    }

    @Override
    public WidgetClient get() {
        return value.get();
    }
}
```

### Injector Lookup Drains

Heliport migrates narrow injector lookups when the target contract is known and the lookup no longer owns Guice module policy. Plain and concrete `TypeLiteral` lookups become `Services.get(...)`; named lookups keep the qualifier name.

**Guice injector lookup**

```java
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.oracle.pic.platform.splat.InjectorFactory;

public final class MetadataFactory {
    public static MetadataClient metadataClient() {
        return InjectorFactory.getInjector().getInstance(
                Key.get(MetadataClient.class, Names.named("canary")));
    }
}
```

**Helidon service-registry lookup**

```java
import io.helidon.service.registry.GlobalServiceRegistry;

public final class MetadataFactory {
    public static MetadataClient metadataClient() {
        return GlobalServiceRegistry.registry()
                .getNamed(MetadataClient.class, "canary");
    }
}
```

### What Remains A Guice Frontier

Heliport reports rather than guesses:

- dynamic provider factories with app-specific policy
- custom scopes that do not map to Helidon service lifecycle
- `PrivateModule`/`expose(...)` graphs whose visibility policy is part of application behavior
- assisted-injection factories and request-time object construction policy
- `requestInjection(...)`, `requestStaticInjection(...)`, and other side-effect-only injection calls
- AOP interceptors, `TypeListener`, and `ProvisionListener` hooks
- broad generic collection `TypeLiteral` lookups where Heliport cannot prove the concrete Helidon contract
- provider methods that call inherited helpers or external factories whose behavior is not source-evidenced
- same-file provider/helper relationships where removing one provider would break another
- retained SDK/provider compatibility where Guice only wraps a provider API that has no safe Helidon target yet.

## JAX-RS, Jersey, And Servlet

Heliport splits server migration from client/provider compatibility. Helidon declarative HTTP looks familiar to JAX-RS users, but it is not Jersey running inside Dropwizard. Endpoints are Helidon services, routes are served by Helidon WebServer, and request/response control flows through Helidon types.

| Old server-side concept | Helidon target concept |
| --- | --- |
| `Environment.jersey().register(Resource.class)` | `@Service.Singleton` plus `@RestServer.Endpoint` on the resource class. |
| `javax.ws.rs.GET`, `POST`, `Path`, `Produces` | `io.helidon.http.Http.GET`, `POST`, `Path`, `Produces`. |
| `@Context HttpHeaders`, `UriInfo`, servlet request | Method parameters, `ServerRequest`, and helper functions reading headers/query/path data from Helidon request APIs. |
| `Response.ok(...).header(...).build()` | Return a simple entity when possible, or use `ServerResponse` for explicit status/header/body/stream behavior. |
| Jersey filters, servlet filters, interceptors | Helidon route filters or OCI/Talon integrations when equivalent behavior is proven; otherwise a protected frontier. |

Server-owned shapes migrate through the JAX-RS and request/response waves:

- resource classes and HTTP method annotations
- `@Path`, `@Produces`, and route parity
- resource registrations in `Environment.jersey()`
- `@Context`, `HttpHeaders`, `UriInfo`, `Request.getMethod()`, servlet request/response, and context carriers
- principal-only `SecurityContext` usage when OCI identity interception ownership is proven
- immediate `AsyncResponse.resume(...)` endpoint methods
- direct `SseEventSink`/`Sse` sends and simple event builders
- response status/body/header construction, including simple `StreamingOutput` and input-stream responses
- status/entity exception mappers and error handlers
- filters, auth helpers, and request body replay helpers when source evidence proves a generic Helidon target
- generated OpenAPI response/header obligations.

### Resource Endpoint

**JAX-RS**

```java
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/v1")
public class ItemResource {
    @GET
    @Path("/items")
    public List<Item> list() {
        return service.list();
    }
}
```

**Helidon**

```java
import io.helidon.http.Http.GET;
import io.helidon.http.Http.Path;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

@Service.Singleton
@RestServer.Endpoint
@Path(value = "/v1")
public class ItemResource {
    @GET
    @Path(value = "/items")
    public List<Item> list() {
        return service.list();
    }
}
```

Route parity recipes normalize trailing class-level slashes when a relative method path would otherwise expose a different Helidon route.

### Request ID Header

When the OCI request-id webserver integration owns `opc-request-id`, unused manual header parameters can be removed:

**Manual header carrier**

```java
void list(
        @HeaderParam("opc-request-id") String opcRequestId,
        ServerResponse response) {
    response.send();
}
```

**After OCI request-id ownership**

```
void list(ServerResponse response) {
    response.send();
}
```

If the method uses the value, Heliport preserves it or promotes it to an `OciRequestId` target when that target is supported.

### Request Context, Principal, Async, And SSE

Several server-side JAX-RS context shapes now migrate directly when their behavior is narrow. Heliport keeps role checks, auth-scheme checks, async lifecycle callbacks, timeout behavior, SSE broadcasters, and computed reconnect/media behavior as frontiers until a source-equivalent target is proven.

#### Principal-only security context

Dropwizard/JAX-RS source:

```java
import com.oracle.pic.identity.authentication.Principal;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@GET
public String me(@Context SecurityContext securityContext) {
    Principal principal = (Principal) securityContext.getUserPrincipal();
    return principal.getSubjectId();
}
```

Helidon target:

```java
import com.oracle.helidon.oci.identity.Identity;
import com.oracle.pic.identity.authentication.Principal;
import io.helidon.http.Http.GET;

@GET
@Identity.Authenticated
public String me(Principal principal) {
    return principal.getSubjectId();
}
```

The Helidon target uses the OCI identity principal, not `java.security.Principal`. Direct identity parameter resolution depends on `IdentityContext` being registered for the request by `@Identity.Authenticated` or a supported Auth SDK authorization annotation.
Plain `java.security.Principal#getName()` access is not equivalent to `Principal#getSubjectId()` unless the source proves that mapping, so Heliport preserves that shape as frontier evidence rather than changing identity semantics.

#### Immediate async response

Dropwizard/JAX-RS source:

```java
import javax.ws.rs.GET;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

@GET
public void status(@Suspended AsyncResponse response) {
    response.resume(service.status());
}
```

Helidon target:

```java
import io.helidon.http.Http.GET;

@GET
public Status status() {
    return service.status();
}
```

#### Direct SSE event sink

Dropwizard/JAX-RS source:

```java
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

@GET
public void events(@Context SseEventSink sink, @Context Sse sse) {
    sink.send(sse.newEventBuilder()
            .id("1")
            .name("ready")
            .comment("first")
            .data("ready")
            .build());
    sink.close();
}
```

Helidon target:

```java
import io.helidon.http.Http.GET;
import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;

@GET
public void events(SseSink sink) {
    sink.emit(SseEvent.builder()
            .id("1")
            .name("ready")
            .comment("first")
            .data("ready")
            .build());
    sink.close();
}
```

### Response And Streaming Endpoints

Heliport treats response migration as behavior migration, not just a type replacement. The response wave must preserve status, content type, headers, empty-body sends, entity sends, and stream sends when those facts are visible in source or generated OpenAPI contracts.

**JAX-RS response builder**

```java
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

Response download(InputStream body) {
    return Response.ok(body, MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=\"data.bin\"")
            .build();
}
```

**Helidon response ownership**

```java
import io.helidon.http.HeaderNames;
import io.helidon.http.Http.Entity;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import java.io.InputStream;

void download(@Entity InputStream body, ServerResponse response) {
    response.status(Status.OK_200);
    response.header(HeaderNames.CONTENT_TYPE, "application/octet-stream");
    response.header("Content-Disposition", "attachment; filename=\"data.bin\"");
    response.send(body);
}
```

For transformation endpoints that sometimes pass the input stream through and sometimes send a transformed body, Heliport keeps the explicit response control:

```java
import io.helidon.http.HeaderNames;
import io.helidon.http.Http.Entity;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import java.io.InputStream;

void transform(@Entity InputStream input, ServerResponse response) {
    if (!trackingEnabled()) {
        response.status(Status.OK_200);
        response.header(HeaderNames.CONTENT_TYPE, "message/rfc822");
        response.send(input);
        return;
    }

    String transformed = transformMessage(input);
    response.status(Status.OK_200);
    response.header(HeaderNames.CONTENT_TYPE, "message/rfc822");
    response.send(transformed);
}
```

Simple `StreamingOutput` responses move to explicit writes on the Helidon response output stream:

**JAX-RS streaming output**

```java
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

Response export() {
    StreamingOutput body = out -> writer.writeTo(out);
    return Response.ok(body, "text/csv").build();
}
```

**Helidon response output stream**

```java
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;

void export(ServerResponse response) throws Exception {
    StreamingOutput body = out -> writer.writeTo(out);
    response.status(Status.OK_200);
    response.header(HeaderNames.CONTENT_TYPE, "text/csv");
    body.write(response.outputStream());
}
```

### Exception Mappers

Status/entity `ExceptionMapper` implementations become Helidon `ErrorHandler` features. Heliport leaves complex builder flows, custom headers, logging side effects, and provider chains as frontiers until the handler behavior is explicit.

**JAX-RS exception mapper**

```java
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public final class NotFoundExceptionMapper
        implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(exception.getMessage())
                .build();
    }
}
```

**Helidon error handler feature**

```java
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ErrorHandler;
import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

@Service.Singleton
public final class NotFoundExceptionMapper
        implements HttpFeature, ErrorHandler<IllegalArgumentException> {
    @Override
    public void setup(HttpRouting.Builder routing) {
        routing.error(IllegalArgumentException.class, this);
    }

    @Override
    public void handle(ServerRequest request,
                       ServerResponse response,
                       IllegalArgumentException exception) {
        response.status(Status.NOT_FOUND_404);
        response.send(exception.getMessage());
    }
}
```

If source behavior depends on gzip interceptors, servlet response wrappers, downstream header replay, or request-body replay, Heliport reports that shape as frontier evidence unless a route-equivalent Helidon target is proven.

### Compatibility Boundaries

The JAX-RS client closeout wave does not delete all JAX-RS imports. It classifies:

- outbound client code and provider SDK surfaces
- retained OCI/Jersey compatibility bridges
- test-only fixtures whose production subject is still a frontier
- complex servlet adapters such as connection-close/GOAWAY, proxy context, zip-bomb protection, session-cookie auth, and request-body hashing.

Those shapes stay visible as compatibility or owner-frontier evidence.

## Generated OpenAPI And Models

Generated OpenAPI/model migration is source and template aware. The [Generated OpenAPI And Model Migration](heliport-migration-generated-openapi-model.md) child page is the detailed contract. At this level, it owns:

- generated server-resource base classes
- generated resource delegate return types
- generated `Response` and status/body/header carriers
- model builder, copy, enum, collection, and temporal transformations
- generated symbol import drift
- generated test fixtures
- re-running generated-source alignment when runtime waves invalidate generated facts.

Generated code is not application logic to hand-edit. Heliport records whether a residual belongs to generated templates, generated postcondition cleanup, handwritten implementation edges, or compatibility.

## Jetty, Netty, And Jackson

These are handled where their ownership appears:

- Jetty server bootstrap and simple wrapper code is Dropwizard bootstrap ownership. Custom listener/TLS/proxy/JMX behavior is a WebServer frontier.
- Netty transport dependencies are Maven governance unless source imports show application-owned Netty behavior. Source-referenced Netty becomes an action item before validation, not a hidden dependency prune.
- Jackson is build/runtime compatibility. Heliport removes stale Dropwizard Jackson shims, materializes shims when proof gates still need retired Dropwizard Jackson classes, and keeps Jackson dependencies when generated or app source needs them.

## Test Migration

Test migration follows production ownership:

- direct health-check tests migrate after health-check classes migrate
- task tests keep calling the preserved `execute(Map, PrintWriter)` body when the task becomes a Helidon endpoint
- generated resource and generated model tests follow generated contract migration
- Dropwizard/Jersey/Guice fixtures are removed or converted only when their production subject has a Helidon owner
- tests for protected frontiers remain action items or compatibility evidence.

Unit-test failures are never used as the first planned detector for known Dropwizard, Guice, JAX-RS, Servlet, generated, or OCI shapes.

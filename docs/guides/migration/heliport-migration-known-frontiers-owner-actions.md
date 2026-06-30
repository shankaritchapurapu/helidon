# Known Frontiers and Application-Owner Actions

Heliport migrates framework ownership where behavior is known and repeatable. It stops at protected frontiers when source code contains application policy, operational behavior, or runtime contracts that do not have a safe Helidon target yet.

A frontier is not hidden cleanup. It must appear in the migration report or action plan with enough evidence for the application owner to make a decision.

Customer action plans distinguish true application-owner decisions from Heliport enhancement candidates. A Heliport enhancement candidate means the source shape appears repeatable and can be promoted into a future Heliport recipe or Talon/Helidon target; it is not customer remaining work unless the report separately classifies the behavior as a true owner decision.

## What A Good Action Item Contains

Each application-owner item names:

- the exact file and source shape
- the family that found it
- why Heliport did not rewrite it automatically
- the behavior that must be preserved
- the likely owner decision or follow-up.

Example action item:

```
Owner action: migrate servlet request-body replay helper.
Family: request-response-carriers.
Evidence: proxy/RequestReplayFilter.java wraps HttpServletRequest and buffers
the body for downstream retry.
Reason: Heliport has no generic Helidon replacement that preserves retry,
hashing, and memory-limit policy.
Expected owner work: choose a Helidon route filter/body buffering strategy and
preserve existing size limits and metrics.
```

## Jetty And Servlet Runtime Frontiers

Heliport can retire simple Dropwizard or Jetty launch wrappers when they only start migrated Helidon infrastructure. It does not automatically migrate custom Jetty or servlet behavior such as:

- connector factories, admin/application connectors, proxy protocol, HTTP/2, SNI, mTLS, TLS reload, JMX, or custom health connector behavior
- raw `HttpServlet`, `Filter`, `ServletContextHandler`, or listener graphs with route-specific policy
- servlet request drains and input-stream wrappers
- servlet response wrappers and writer-close behavior
- request-context `ThreadLocal` propagation
- servlet-coupled TLS authorization filters.

**Servlet filter frontier**

```java
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class TlsAuthorizationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!authorized(httpRequest)) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

This becomes an explicit owner action unless Heliport can prove a Helidon route filter that preserves the same certificate, authorization, response, and ordering behavior.

## Response, Streaming, And Proxy Frontiers

Heliport migrates simple `Response` status/body/header behavior, simple input-stream and `StreamingOutput` responses, immediate `AsyncResponse.resume(...)` endpoints, and direct SSE event-sink sends/builders. It reports frontiers when response behavior depends on:

- gzip response interceptors or content negotiation policy
- downstream header replay
- servlet response wrappers
- request-body replay or hashing
- upstream/downstream proxy context mutation
- async timeout, cancel, callback, or suspended-response lifecycle behavior
- SSE broadcasters, computed reconnect values, or arbitrary media negotiation
- streaming behavior with memory, retry, or backpressure policy.

**Gzip response frontier**

```java
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

Response list(ProxyContext context, Listing listing) {
    Response.ResponseBuilder builder = Response.ok()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .header("opc-next-page", listing.nextPage())
            .entity(listing.items());

    if (context.acceptsGzip()) {
        builder.header("Content-Encoding", "gzip");
        context.setGzipResponseRequired();
    }

    listing.downstreamHeaders().forEach(builder::header);
    return builder.build();
}
```

Heliport does not classify this as a simple `ServerResponse` rewrite. The owner must preserve compression, downstream header relay, metrics, and proxy context behavior.

## Guice Provider And Injector Frontiers

Heliport migrates direct injection, simple singleton services, provider methods, supplier shapes, safe `Key.get(...)` lookups, named static lookups, concrete `TypeLiteral` lookups, and eager-singleton binding drains when the target dependency is known. It reports frontiers for:

- overlay/substrate module provider roots
- `PrivateModule`/`expose(...)` visibility policy
- assisted-injection factories
- `requestInjection(...)`, `requestStaticInjection(...)`, AOP interceptors, `TypeListener`, and `ProvisionListener` hooks
- dynamic provider factories with service-specific policy
- custom scopes
- provider methods that call inherited helpers whose behavior is not source-evidenced
- Guice modules that are source owners for SDK/provider APIs rather than consumers of a Helidon integration.

**Provider-root frontier**

```java
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import javax.inject.Singleton;

public final class OverlayModule extends AbstractModule {
    @Provides
    @Singleton
    LimitsClient limitsClient(OverlayConfig config) {
        return new LimitsClientFactory(config.dynamicTls(), config.retryPolicy())
                .create();
    }
}
```

This remains owner work until the dynamic TLS and retry policy have a proven Helidon OCI Limits target.

## OCI Integration Frontiers

OCI integration migration is feature-specific. Heliport adds Helidon OCI dependencies and configuration only for proven consumer shapes. It reports frontiers for application-owned OCI behavior such as:

- Kiev transaction wrappers with custom transaction policy
- WFaaS client factories with custom Jersey, TLS, worker/poller, or workflow domain behavior
- Identity filters with local/mock auth branches, SAML/principal enrichment, or route-specific authorization policy
- SPLAT provider or source-owner modules where namespace evidence is not enough to add SPLAT client artifacts
- Secret Service direct lookup clients and TLS manager construction with custom domain logic
- Limits, Tagging, Object Storage, or Identity clients with custom transport, signer, cache, or policy behavior
- direct metrics, audit, MDC, or metering emitters where metric names, dimensions, payloads, or fleet values are application policy.

**Secret/TLS frontier**

```java
ServiceAuthenticationClient authClient(SecretConfig config) {
    return ServiceAuthenticationClient.builder()
            .endpoint(config.endpoint())
            .dynamicSslContextProvider(config.sslContextProvider())
            .sessionKeySupplier(config.sessionKeySupplier())
            .build();
}
```

Heliport can classify the Secret Service or Identity feature, but the application owner must preserve endpoint, TLS, session, and retry semantics unless an exact Helidon target is encoded.

## Metrics And Logging Frontiers

Heliport migrates known Helidon and OCI metrics publisher configuration. It does not invent metric names, dimensions, fleets, or logging delivery policy.

Common action items include:

- direct `Metrics.emit(...)` calls tied to business events
- audit event emitters with domain-specific payloads
- response filters that enrich OCI MDC from entity bodies
- Dropwizard logging factories, smart-log appenders, or async appender policy
- custom upstream/downstream response metrics in proxy flows.

These items stay visible in the report so application owners can decide whether to keep direct emitters, move to a Helidon metric, or add a future Heliport recipe.

## JAX-RS Provider Frontiers

Heliport migrates status/entity `ExceptionMapper` implementations to Helidon `ErrorHandler` features when the response behavior is direct. It reports provider frontiers for `MessageBodyReader`, `MessageBodyWriter`, `ReaderInterceptor`, `WriterInterceptor`, `ParamConverterProvider`, role/auth-scheme `SecurityContext` checks, `Request.evaluatePreconditions(...)`, and provider chains whose ordering or content negotiation is application behavior.

## Generated OpenAPI Frontiers

Generated OpenAPI frontiers are assigned by ownership:

- generated template drift belongs to generated-source owners
- handwritten implementation behavior belongs to the application owner
- response/header/body obligations belong to the generated resource wave when the target behavior is known
- streaming, gzip, proxy, multipart, or request replay behavior remains owner work when the generated contract does not fully describe runtime behavior.

The detailed generated-model guidance is in [Generated OpenAPI And Model Migration](heliport-migration-generated-openapi-model.md).

## Test Frontiers

Tests migrate after production ownership is known. Heliport reports test frontiers instead of broad-editing fixtures when:

- the production subject is still a Jetty, Servlet, proxy, OCI, or generated frontier
- the test asserts Dropwizard API behavior with no Helidon owner yet
- generated fixtures still fail after known generated-model closeouts, such as builder/setter, enum/string, collection input, and `@MethodSource` provider alignment, have been applied
- mock SDK/provider setup is compatibility evidence rather than Helidon runtime ownership.

The report makes clear whether a failing test is validation evidence for a migrated owner or an expected action item for an unresolved frontier.

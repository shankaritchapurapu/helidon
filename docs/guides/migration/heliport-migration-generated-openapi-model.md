# Generated OpenAPI and Model Migration

Generated OpenAPI and generated model migration is a separate drill-down because it crosses build, source, endpoint, model, and test behavior. Heliport must preserve the generated contract while moving handwritten implementations toward Helidon.

The detailed wave names are usually `generated-resource-closeout` before runtime migration and `post-runtime-generated-alignment` after runtime migration. The first wave prepares generated facts that handwritten code depends on. The second wave rechecks generated contracts, response carriers, and tests after JAX-RS, request/response, OCI, and Guice waves have changed the surrounding code.

## Splat/RQS Validation And Helidon Resource Generation

For OCI services that use Splat/RQS metadata, Heliport preserves the split Talon build model: the Splat Maven plugin validates the Splat-facing Swagger 2 specification and downstream specification, while the Helidon Extensions OpenAPI generator produces Helidon declarative resources from the same API specification. Splat validation does not generate Helidon Java resources, and the Helidon generator does not validate, submit, or publish Splat/RQS metadata.

**Splat validation**

```xml
<plugin>
    <groupId>com.oracle.pic.platform.splat</groupId>
    <artifactId>splat-swagger-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>validate-splat-template</id>
            <phase>compile</phase>
            <goals>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <disable>false</disable>
        <outputFileLocation>${project.build.directory}/splat-spec-validation-output.txt</outputFileLocation>
        <configs>
            <param>${project.build.directory}/generated-splat/splat-validation.conf</param>
        </configs>
    </configuration>
</plugin>
```

**Helidon resource generation**

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generate-helidon-resources</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>helidon-declarative</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/swagger/service-api.yaml</inputSpec>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <configOptions>
                    <apiPackage>com.example.generated.api</apiPackage>
                    <modelPackage>com.example.generated.model</modelPackage>
                    <invokerPackage>com.example.generated</invokerPackage>
                    <helidonVersion>${helidon.version}</helidonVersion>
                    <generateClient>false</generateClient>
                    <generateErrorHandler>false</generateErrorHandler>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.helidon.extensions.openapi-generator</groupId>
            <artifactId>helidon-extensions-openapi-generator</artifactId>
            <version>${version.helidon.extensions.openapi-generator}</version>
        </dependency>
    </dependencies>
</plugin>
```

Customers will see Heliport preserve the generated contract and the build-time validation/generation boundary. Runtime Splat validation is documented separately under the SPLAT OCI integration; generated `/openapi` output is served by Helidon OpenAPI when the generated `META-INF/openapi.yaml` is on the application classpath.

## What Heliport Migrates

| Source shape | Heliport target or classification |
| --- | --- |
| Generated server resource contracts | Helidon-compatible handwritten implementations, endpoint annotations, generated response obligations, and route parity. |
| Generated model builders | Setter-based generated bean construction or helper-wrapped construction when the target model no longer exposes a compatible builder. |
| Generated enum/string boundaries | Explicit enum conversion, string conversion, and list conversion helpers where generated types differ from handwritten domain types. |
| Generated model helper fixtures | Test fixture updates for builder chains, enum constants, generated helper chains, temporal fields, and collection defaults. |
| Generated collection and JUnit fixture drift | Source-evidenced collection input alignment, such as `Set` to `List` when the generated target requires it, and external `@MethodSource` provider qualification when the migrated JUnit runtime requires a sibling provider class name. |
| Generated response carriers | Explicit `ServerResponse` status, headers, empty-body sends, entity sends, and stream sends. |
| Generated symbol drift | Import and type cleanup when generated models move packages, stop exposing old nested types, or require helper conversion. |
| Template-owned residuals | Reported as generated-template or generated-source owner work instead of hand-edited as application logic. |

Generated code is not a place for manual application fixes. Heliport classifies each residual as one of these outcomes:

- migrated generated contract behavior
- migrated handwritten implementation behavior required by generated contracts
- retained compatibility helper
- generated-template drift requiring template-owner action
- application-owner action when source policy cannot be inferred safely.

## Helidon Contract Shapes Generated Resources Use

Generated OpenAPI resources often force the handwritten implementation to preserve exact HTTP behavior. In Dropwizard/Jersey code that behavior was usually hidden in a returned `Response` object. In Helidon declarative HTTP it is often explicit through `ServerResponse`.

| Generated/JAX-RS expectation | Helidon shape | What customers check |
| --- | --- | --- |
| Method returns `Response` with status, headers, and entity. | Method takes `ServerResponse`, sets status/header values, then calls `send(...)`. | The status code, content type, paging/work-request/location headers, and body are still present. |
| Method returns a simple model object or collection. | Method can return the model object directly when no extra response control is needed. | No generated header or status obligation was dropped. |
| Generated model used a builder API. | Setter-based generated bean construction or a local generated helper. | The helper only adapts generated shape; it does not encode business policy. |
| Generated enum differs from old string/domain model shape. | Explicit conversion helper at the boundary. | String values, null handling, and list handling match the old behavior. |

## Generated Model Construction

Many generated models do not preserve the same builder API after regeneration or after the generated source baseline changes. Heliport rewrites local construction where it can prove the setter target and field conversion.

**Builder-style source**

```java
import java.time.Instant;

WidgetSummary toSummary(WidgetEntity entity) {
    return WidgetSummary.builder()
            .id(entity.id())
            .displayName(entity.displayName())
            .timeCreated(entity.timeCreated())
            .lifecycleState(WidgetSummary.LifecycleState.Active)
            .build();
}
```

**Generated bean target**

```java
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

WidgetSummary toSummary(WidgetEntity entity) {
    return generatedOpenApiBean(new WidgetSummary(), summary -> {
        summary.setId(entity.id());
        summary.setDisplayName(entity.displayName());
        summary.setTimeCreated(toOffsetDateTime(entity.timeCreated()));
        summary.setLifecycleState(WidgetSummary.LifecycleState.Active);
    });
}

private static <T> T generatedOpenApiBean(T bean, Consumer<T> initializer) {
    initializer.accept(bean);
    return bean;
}

private static OffsetDateTime toOffsetDateTime(Instant value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
}
```

The helper is intentionally local. It preserves expression shape for complex call sites without pretending that generated models own application policy.

## Enum And String Boundaries

Generated OpenAPI models often expose enum values while domain services or old generated clients still pass strings. Heliport makes the conversion explicit instead of relying on raw `name()` calls hidden inside larger expressions.

**Mixed generated/domain boundary**

```java
List<WidgetMode> modes(WidgetDetails details) {
    return details.getModes();
}
```

**Explicit generated boundary**

```java
import java.util.List;

List<String> modes(WidgetDetails details) {
    return generatedOpenApiEnumStringListOrNull(details.getModes());
}

private static List<String> generatedOpenApiEnumStringListOrNull(List<WidgetMode> values) {
    if (values == null) {
        return null;
    }
    return values.stream()
            .map(Enum::name)
            .toList();
}
```

When a value crosses back into a generated enum, the conversion is just as explicit:

```java
private static WidgetMode generatedOpenApiWidgetModeOrNull(String value) {
    return value == null ? null : WidgetMode.valueOf(value);
}
```

## Generated Resource Response Carriers

Generated server contracts frequently require a specific status, content type, header, and body shape. Heliport migrates the handwritten implementation to `ServerResponse` when that behavior is source-evidenced.

**JAX-RS resource implementation**

```java
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

Response getManifest(String serviceName, InputStream manifest) {
    return Response.ok(manifest, MediaType.APPLICATION_JSON_TYPE)
            .header("opc-request-id", requestId())
            .build();
}
```

**Helidon resource implementation**

```java
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import java.io.InputStream;

void getManifest(String serviceName, InputStream manifest, ServerResponse response) {
    response.status(Status.OK_200);
    response.header(HeaderNames.CONTENT_TYPE, "application/json");
    response.header("opc-request-id", requestId());
    response.send(manifest);
}
```

If request-id ownership has moved to `helidon-oci-request-id-webserver`, Heliport removes unused manual request-id carriers. Method-local headers such as `etag`, `opc-next-page`, work-request IDs, and locations are preserved unless a specific Helidon integration owns them.

## Streaming And Pass-Through Bodies

Some endpoints stream an input body through unchanged under one branch and send a transformed body under another branch. Heliport preserves those branches because they are observable API behavior.

```java
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import java.io.InputStream;

void transform(InputStream input, boolean enabled, ServerResponse response) {
    response.status(Status.OK_200);
    response.header(HeaderNames.CONTENT_TYPE, "message/rfc822");

    if (!enabled) {
        response.send(input);
        return;
    }

    response.send(transformToString(input));
}
```

Known retained frontiers in this area include gzip response interceptors, servlet response wrappers, downstream header replay, range requests, multipart streaming, and request-body replay. Those behaviors remain application-owner action items until Heliport has a proven Helidon target for the exact shape.

## Test Fixture Migration

Generated model tests are migrated after the production contract owner is known. Typical fixture updates include:

- builder-chain fixtures that move to setter-based generated beans
- enum constant fixtures that move through explicit string or enum helpers
- generated helper build chains that need import and temporal conversion cleanup
- collection fixture inputs that move between `Set` and `List` when generated setters or builders require the target collection type
- external `@MethodSource` references that need fully qualified sibling provider class names under the migrated JUnit runtime
- generated resource tests that assert status/header/body behavior after `ServerResponse` migration
- test-only Dropwizard or Jersey fixtures that stay as action items when their production subject is still a frontier.

These known generated-test closeouts do not remain application-owner action items when Heliport can prove the generated target type or provider class. Remaining owner actions are limited to template-owned drift, production behavior outside the generated contract, or test assertions whose production subject is still a frontier.

Heliport does not let unit failures be the first detector for generated model or generated resource drift. The generated OpenAPI waves own those findings before validation is treated as meaningful proof.

## Customer Review Checklist

Review generated OpenAPI findings with these questions:

- Did generated source regenerate cleanly before compile validation?
- Did Heliport preserve status, header, content type, and body semantics for generated resource implementations?
- Are enum/string conversions explicit at generated boundaries?
- Are local helper methods limited to generated compatibility and not hiding business policy?
- Are template-owned findings assigned to generated-source owners rather than application code owners?
- Are remaining streaming, gzip, response wrapper, or request-body replay items listed as action items with exact file evidence?

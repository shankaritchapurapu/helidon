# Helidon Talon Speaker Notes

These notes explain the intent behind each bullet in the presentation. They are meant as presenter guidance, not slide text.

## Title Slide

No bullet points.

## What Is Helidon Talon?

- **Helidon extensions for building OCI Native Services**

  Explain that Helidon Talon is a set of OCI-focused integrations layered on Helidon 4, intended to help service teams build applications that fit OCI platform expectations.

- **A versioned BOM for consistent adoption across service teams**

  Emphasize that teams adopt a coordinated dependency set instead of independently choosing versions of every OCI client and integration library.

- **Less infrastructure wiring in every application**

  The point is that common OCI plumbing, such as auth, region discovery, clients, metrics, and audit, should be provided by reusable modules rather than repeated in every service.

- **OCI integrations and injectable clients**

  The lower half summarizes the concrete runtime value: service integrations and clients should be available through Helidon's normal injection model.

- **Environment config, auth, and observability**

  These are the shared platform concerns that Helidon Talon standardizes so applications do not rebuild them service by service.

- **Helidon services running in OCI**

  The primary audience is Helidon-based services that need to run cleanly inside OCI environments.

- **DP services first; stronger CP support next**

  Current emphasis is on data-plane service patterns, while control-plane support is expected to improve in later iterations.

## At A Glance

No bullet points.

## Helidon 4 Foundation

- **Helidon SE-first services**

  Helidon Talon is built around the Helidon SE programming model rather than a heavyweight application server model.

- **Compile-time injection via the service registry**

  Injection is resolved through Helidon 4's service registry and compile-time metadata, which keeps runtime startup and wiring predictable.

- **Typed configuration with generated metadata**

  Configuration is modeled through typed definitions, with generated metadata so users get documented, discoverable settings rather than tribal knowledge.

- **Direct service code with platform interceptors**

  Application code can stay direct and debuggable while platform interceptors handle cross-cutting behavior where the integration requires it.

- **JDK 25 baseline**

  The runtime column starts with the Java baseline because it shapes language features, performance assumptions, and supported deployment targets.

- **High-performance request handling with virtual threads**

  Helidon 4 can use virtual threads to keep blocking service code efficient under high concurrency.

- **Blocking code instead of async/reactive plumbing**

  The point is developer ergonomics: services can often avoid complex reactive chains while relying on Helidon infrastructure for scalability.

- **Helidon infrastructure for routing, lifecycle, config, and observability**

  Helidon provides the runtime services that Helidon Talon integrations plug into, including routing, startup lifecycle, configuration, and telemetry.

## Baseline and Size

- **Helidon 4**

  Helidon Talon is aligned with the Helidon 4 runtime model and currently targets Helidon `4.5.0-M1` in this deck.

- **JDK 25**

  JDK 25 is the compiler and documented runtime baseline for this release line.

- **OCI SDK 2.88.0**

  OCI Java SDK `2.88.0` is managed through Helidon Talon dependency management rather than chosen independently by each application.

- **28+ managed artifacts in the BOM**

  The BOM currently manages at least 28 Helidon Talon artifacts, coordinating modules and related dependencies so service teams can adopt a consistent set.

  For scale, the Helidon core BOM manages roughly 350 Helidon artifacts, so Helidon Talon is intentionally much smaller and focused on OCI-native service integration.

- **13+ documented service integrations**

  The integration count is expressed as `13+` because the deck covers the documented integration set, and the platform is expected to keep growing.

- **9+ runnable example applications**

  The example count is also `9+` to signal that the repository includes multiple runnable starting points and more can be added over time.

## OCI Environment Config

- **Discovers and publishes OCI location data**

  `helidon-oci-envconfig` finds runtime location information and exposes it as Helidon configuration.

- **Keeps region/domain handling out of application code**

  Application code should not need custom logic to discover region, AD, FD, or domain names.

- **Supports runtime discovery, dynamic core-regions metadata, IMDS fallback, and test overrides**

  The module works in real OCI environments, supports updated region metadata, can fall back to instance metadata, and remains testable.

## How `oci-env` Is Used

- **Keeps region and domain logic out of client integration code**

  Client integrations can rely on shared environment config rather than each implementing its own location discovery.

- **Supports runtime discovery, IMDS fallback, dynamic metadata, and test overrides**

  Reiterate that the same mechanism works in production and in tests.

- **Example: Secret Service can use `${oci.env.iaas-domain-name}`**

  This is a concrete example of endpoint construction using environment-derived config values.

## OCI SDK Authentication

- **Shared authentication selection for OCI Java SDK clients**

  The common OCI SDK integration centralizes how applications choose the credentials used by public OCI Java SDK clients.

- **Exposes the selected `BasicAuthenticationDetailsProvider`**

  Once the method is resolved, Helidon Talon publishes the selected OCI SDK provider through the service registry.

- **Auth methods are contributed by classpath modules**

  Applications choose the authentication methods they want to support by adding modules such as instance principal, resource principal, OKE workload identity, or service principal.

- **Client integrations consume one provider contract**

  SDK-backed modules can build clients from the same provider contract rather than implementing their own auth selection logic.

## OCI SDK Authentication Configuration

- **Config root is `helidon.oci`**

  Shared OCI SDK authentication settings live under this configuration subtree.

- **`authentication-method` selects or requires a provider**

  Services can explicitly require one method, such as `instance-principal`, when deployment shape is known.

- **`auto` tries available methods from the service registry**

  In auto mode, Helidon Talon asks the available authentication method services for a provider and uses the first one that succeeds, optionally constrained by `allowed-authentication-methods`.

- **`BasicAuthenticationDetailsProvider` can be injected**

  Application code and SDK-backed integration modules can inject the selected provider directly from the Helidon service registry.

## Service Integrations

No bullet points.

This slide introduces the three main integration styles used throughout the service section. Some integrations participate in request handling through filters and interceptors, some expose OCI clients through injection, and some rely on declarative APIs plus generated code. The Helidon Registry is the common foundation that makes these pieces discoverable and injectable at runtime.

## Audit

- **Inserts a Helidon WebServer filter for OCI Audit v2 events**

  Audit is implemented as a WebServer filter that participates in the request path and creates audit events.

- **Captures request path, action, request id, caller, response status, and timing**

  The filter records the baseline request and response facts needed for audit trails.

- **Uses config rules to whitelist audited parameters and headers**

  Audit does not blindly record all request data; config rules decide which parameters and headers are safe and relevant.

- **Emits events through the Sherlock audit logging path**

  Helidon Talon creates Audit v2 events and sends them through the standard Sherlock audit logger.

- **Respects `oci-splat-audited` to avoid duplicate SPLAT audit work**

  If SPLAT has already audited a request and the module is configured to respect that signal, Helidon Talon can skip duplicate audit emission.

- **Lets endpoint logic enrich the current audit event**

  Application code can add resource-specific audit details when the generic request data is not enough.

  - **`AuditPayloadAppender` is available in request scope**

    The enrichment object is per request, so endpoint code should treat it as current-request state rather than a singleton service.

## Audit Configuration

- **`oci.auditv2` enables auditing and names the audit source**

  The config root turns Audit on and identifies the source name that appears in emitted events.

- **Rules explicitly whitelist audited request values**

  Rules control which query parameters and headers may appear in audit data.

- **A rule applies only when resource and action match**

  A rule is scoped by path and HTTP action, so a value is recorded only for matching routes and methods.

## Identity

- **Integrates Helidon endpoints with the OCI Auth SDK**

  Identity wires OCI authentication and authorization into Helidon endpoint handling.

- **Provides authentication, authorization, and SPLAT-aware request handling**

  It handles normal signed requests and SPLAT-forwarded request shapes.

- **Registers `Principal`, `AuthorizationRequest`, and `IdentityContext` per request**

  Identity makes request identity and authorization context available to downstream code.

  - **In the Helidon service registry**

    These objects are exposed through Helidon's request-aware service registry mechanisms.

- **Uses annotations and generated interceptors instead of hand-wired filters**

  Endpoint code declares permissions and identity needs; generated infrastructure performs the enforcement.

## Identity Configuration

- **Config root is `oci.identity`**

  All Identity module settings live under this configuration subtree.

- **Authentication and authorization are configured independently**

  Services can tune who the caller is and what the caller may do as separate concerns.

- **Region and location defaults can come from `oci-env`**

  Identity can rely on shared environment configuration for region-aware behavior.

## Identity Example

- **Auth SDK annotations drive generated authorization interceptors**

  The code-level permission annotation is used to generate authorization enforcement around the resource method.

- **Request data can be injected directly into resource methods**

  Endpoint method parameters can receive request-derived values instead of manually reading them from the HTTP request.

## Kiev

- **Provides Helidon configuration and service registry bindings for Kiev**

  Helidon Talon makes Kiev data stores available through Helidon config and injection.

- **Supports in-memory, direct database, and Kiev-as-a-service backends**

  The same programming model can target local/test, database-backed, or service-backed Kiev deployments.

- **Exposes `DataStore`, `MappedDataStore`, and transaction support**

  Services can inject the native Kiev abstractions they need.

- **Adds `@KievTransaction` for declarative transaction handling**

  Transaction handling can be declared with an annotation instead of opened manually in service code.

## Kiev Configuration

- **Config root is `oci.kiev`**

  Kiev integration settings are grouped under this configuration subtree.

- **Each entry defines one named data store**

  Applications can configure multiple Kiev stores and identify them by name.

- **Store names qualify injected Kiev services and transactions**

  The configured names are used to select the correct injected store and transactional context.

## Kiev Example

- **Inject `MappedDataStore` by configured store name**

  The example shows using the configured name to select a specific Kiev mapped store.

- **Open mapped buckets from service code**

  Service code can work with Kiev buckets using the injected native Kiev API.

## Kiev Transactions

- **`@KievTransaction` opens the transaction boundary around a service method**

  The annotation is processed at build time and results in a generated Helidon interceptor for the annotated method.

- **The generated interceptor supplies a `Transaction` parameter when one is declared**

  Service code can accept `com.oracle.pic.kiev.Transaction` as a method parameter and pass it to mapped bucket operations.

- **Successful write transactions commit on method exit**

  Non-read-only transactions commit after the method returns normally.

- **Failures abort in-flight writes, then close the transaction**

  If the method throws, Helidon Talon aborts an in-flight write transaction, rethrows the exception, and closes the transaction in all cases.

## Limits

- **Provides Helidon integration for OCI Limits**

  Helidon Talon wires the OCI Limits client into Helidon applications.

- **Exposes a configured Limits DP client**

  The data-plane Limits client is created from config and made injectable.

- **Uses shared OCI SDK authentication and client configuration**

  Limits participates in the same SDK auth and client setup used by other OCI integrations.

- **Supports endpoint and timeout configuration**

  Teams can tune routing and request behavior without creating the client manually.

## Limits Configuration

- **Config root is `oci.limits`**

  Limits settings live under this configuration subtree.

- **Client settings use the common OCI SDK timeout shape**

  The module follows the same timeout configuration pattern used by other SDK-backed clients.

- **Endpoint can be overridden for explicit routing**

  Services can specify an endpoint directly when the default derived endpoint is not appropriate.

## Limits Example

- **Inject the configured `LimitsDPClient`**

  Application code receives the configured client through injection.

- **Use it from service code to check limits and quotas**

  The client can be called from normal service logic to consult Limits behavior.

## Metering

- **Reports OCI service usage through native metering libraries**

  Helidon Talon integrates the existing OCI metering libraries rather than inventing a new metering model.

- **DP is high volume and must avoid per-request metering calls**

  Data-plane services can handle very high request volume, so metering cannot be modeled as a start/stop API call around every request or resource use.

- **Accumulates usage efficiently and reports emitter-dp batches**

  DP services should gather usage data efficiently in service code and submit batches through the native emitter-dp API.

- **Uses Object Storage for durable backup**

  DP metering follows the native emitter-dp storage model, where Object Storage can be used to reduce loss of metering data.

- **CP is lower traffic and matches emitter-cp start/stop style**

  Control-plane services generally have lower traffic, and the native emitter-cp API naturally maps to starting and ending metered work.

- **Supports annotations for points, timers, and metered regions**

  CP metering can be declared with annotations that generate Helidon method interceptors at compile time.

- **Uses Kiev through a `MappedDataStore` supplier**

  CP metering follows the native metering-agent storage model, which relies on Kiev-backed log stores supplied through `MappedDataStore`.

## Metering Configuration (DP)

- **Config root is `oci.metering`**

  Metering settings are grouped under this configuration subtree.

- **Required values come from the native emitter-dp builder**

  The config mirrors the native emitter-dp library's required setup.

- **Object Storage backup can be configured separately when enabled**

  Metering can optionally back up emitted usage data to Object Storage.

## Metering Example (DP)

- **DP services inject the native `LogFileUsageRecorder`**

  Data-plane code uses the native recorder abstraction directly.

- **Service code records native emitter-dp meter batches**

  Usage records are written as the native emitter-dp library expects.

## Metering Configuration (CP)

- **Uses metering-agent with Kiev-backed log stores**

  CP metering relies on metering-agent and Kiev storage for metering logs.

- **Metered calls run inside a Kiev transaction**

  Metering operations participate in transactional Kiev-backed workflows.

## Metering Example (CP)

- **CP metering annotations are processed at compile time**

  The annotations generate the integration code needed for CP metering.

- **Parameters identify compartment, resource, amount, and tags**

  Method parameters provide the fields needed to construct metering records.

## Metrics

- **Publishes Helidon neutral metrics to OCI metrics**

  Helidon Talon sends metrics recorded through Helidon APIs to OCI Monitoring.

- **Works with normal Helidon metrics APIs and annotations**

  Services can use familiar Helidon metrics annotations and APIs.

- **Supports counters, timers, distribution summaries, gauges, and functional counters**

  The integration supports the main metric types teams typically need.

- **Adds automatic HTTP request metrics and optional JVM gauges**

  Services can get request-level and runtime metrics with minimal custom code.

## Metrics Configuration

- **Configured as a Helidon metrics publisher of type `oci`**

  OCI metrics emission plugs into Helidon's metrics publisher model.

- **Project and fleet identify the emitted metric stream**

  These fields help route and group emitted metrics in OCI.

- **Region can be explicit or supplied through `oci-env`**

  Region can be configured directly or resolved through environment configuration.

- **Endpoint can be derived from OCI Monitoring or explicitly overridden**

  The module can use the regional Monitoring endpoint or a configured endpoint.

## Metrics Example

- **Service code uses Helidon metrics annotations**

  Application code records metrics with normal Helidon annotation patterns.

- **Helidon Talon publishes metric updates to OCI metrics**

  Helidon Talon handles the publication path from Helidon metrics to OCI.

[object-storage-notes-001]: # "## Object Storage"
[object-storage-notes-002]: # ""
[object-storage-notes-003]: # "- **Provides a service registry binding for the OCI SDK Object Storage client**"
[object-storage-notes-004]: # ""
[object-storage-notes-005]: # "  Helidon Talon creates and registers the SDK client so applications can inject it."
[object-storage-notes-006]: # ""
[object-storage-notes-007]: # "- **Uses the shared OCI SDK authentication provider**"
[object-storage-notes-008]: # ""
[object-storage-notes-009]: # "  Object Storage uses the same OCI auth setup as other SDK integrations."
[object-storage-notes-010]: # ""
[object-storage-notes-011]: # "- **Supports configured endpoint and region routing**"
[object-storage-notes-012]: # ""
[object-storage-notes-013]: # "  The client can target an endpoint derived from region or explicitly configured."
[object-storage-notes-014]: # ""
[object-storage-notes-015]: # "- **Lets services inject the client instead of constructing it directly**"
[object-storage-notes-016]: # ""
[object-storage-notes-017]: # "  Application code should depend on the injected SDK interface rather than creating the client manually."
[object-storage-notes-018]: # ""
[object-storage-notes-019]: # "## Object Storage Configuration"
[object-storage-notes-020]: # ""
[object-storage-notes-021]: # "- **Config root is `oci.object-storage-client`**"
[object-storage-notes-022]: # ""
[object-storage-notes-023]: # "  Object Storage client settings live under this configuration subtree."
[object-storage-notes-024]: # ""
[object-storage-notes-025]: # "- **Region derives the service endpoint when no endpoint is set**"
[object-storage-notes-026]: # ""
[object-storage-notes-027]: # "  If no endpoint is configured, the module can derive one from region."
[object-storage-notes-028]: # ""
[object-storage-notes-029]: # "- **Client settings follow the common OCI SDK configuration shape**"
[object-storage-notes-030]: # ""
[object-storage-notes-031]: # "  Timeouts and related client settings are consistent with other SDK integrations."
[object-storage-notes-032]: # ""
[object-storage-notes-033]: # "## Object Storage Example"
[object-storage-notes-034]: # ""
[object-storage-notes-035]: # "- **Inject the OCI SDK `ObjectStorage` interface**"
[object-storage-notes-036]: # ""
[object-storage-notes-037]: # "  The example shows code receiving the native SDK client through injection."
[object-storage-notes-038]: # ""
[object-storage-notes-039]: # "- **Build normal OCI SDK requests in service code**"
[object-storage-notes-040]: # ""
[object-storage-notes-041]: # "  Once injected, the client is used with normal OCI SDK request builders."

## Secret Service V2

No bullet points.

The diagram frames SSv2 as two related capabilities: secret values exposed through Helidon Config and certificate material rotation through Helidon TLS.

## Secret Service V2 Secrets

- **Exposes Secret Service V2 values through Helidon Config**

  Application code can consume Secret Service V2 values through the standard Helidon `Config` API instead of calling Secret Service directly.

- **Maps a configured key prefix to SSv2 secret paths**

  The configured prefix, such as `oci.ssv2`, decides which config keys are handled by this source and how the remaining key suffix maps to an SSv2 path.

- **Resolves secrets lazily when application code reads the key**

  SSv2 is not scanned up front. A secret is fetched when the application first asks for the corresponding config value.

- **Caches direct reads and polls only previously requested keys**

  Direct reads use the configured cache TTL, and background polling only checks keys that have already been requested and tracked.

- **Can derive endpoints from environment configuration**

  Secret Service endpoint construction can use values from `helidon-oci-envconfig`.

## Secret Service V2 Secrets Configuration

- **Configured under `helidon.oci-secret-service`**

  This does not mean Secret Service V2 comes from Helidon core. The setting is under `helidon` because this integration plugs into Helidon's bootstrap and config infrastructure. In this case, the subtree configures the Helidon-managed SSv2 config source supplied by Helidon Talon.

- **Prefix maps config lookups to Secret Service paths**

  A local config prefix is mapped to an SSv2 secret path structure.

- **Endpoint can use `${oci.env.iaas-domain-name}`**

  The endpoint can be built from environment-derived domain information.

## Secret Service V2 Secrets Example

- **Inject Helidon `Config`**

  Application code reads secrets through the standard Helidon Config API.

- **Read SSv2-backed values using the configured prefix**

  The configured prefix decides which Secret Service path is used for a given config lookup.

## Secret Service V2 mTLS

- **Provides the `oci-ssv2` TLS manager**

  Helidon Talon adds a TLS manager implementation that can be selected from Helidon TLS configuration.

- **Loads PKI JSON material from SSv2 or a Helidon resource**

  The manager can read certificate and key material from a Secret Service path or from a file, classpath resource, or inline resource.

- **Rotates server and client mTLS material on a reload schedule**

  The reload schedule lets servers and clients pick up fresh PKI material without rebuilding the application.

- **Supports Helidon clients and raw `SSLContext` use**

  The same TLS manager can be used by Helidon-managed clients or by lower-level clients that accept a Java `SSLContext`.

## Secret Service V2 mTLS Rotation

- **Select `oci-ssv2` under Helidon TLS manager config**

  The TLS manager is activated from normal Helidon TLS configuration by choosing the `oci-ssv2` manager implementation.

- **`pki.secret-path` points to PKI JSON material in SSv2**

  The secret path identifies the PKI-produced JSON bundle containing the private key, leaf certificate, and intermediates.

- **`reload.expression` controls the refresh cadence**

  The cron expression tells Helidon Talon how often to read fresh TLS material and reload the active key and trust managers when material changes.

- **The same manager shape works for server and client TLS**

  The example shows server TLS, but the same `manager.oci-ssv2` structure can be used under client TLS configuration for outbound mTLS.

## Splat

- **Adds SPLAT mTLS validation support for Helidon endpoints**

  Helidon Talon wires SPLAT mTLS validation into generated Helidon endpoint handling.

- **Bridges SPLAT-authenticated requests into Helidon request handling**

  The integration lets the upstream SPLAT JAX-RS filter run in the Helidon request pipeline.

- **Supports OCI service front-door integration patterns**

  Services can participate in the normal OCI front-door/SPLAT request path.

- **Works with Identity for SPLAT-aware authentication flows**

  Splat validation and Identity's SPLAT-aware handling complement each other for forwarded principal scenarios.

## Splat Configuration

- **Config root is `oci.splat`**

  SPLAT settings are grouped under this configuration subtree.

- **Validation runs on generated endpoint handlers**

  The SPLAT interceptor applies to generated Helidon endpoint entry points.

- **Region can be explicit or resolved from the OCI environment**

  SPLAT validation can use a configured region or a region supplied by `oci-env`.

## Tagging

- **Contributes the internal tagging client to the Helidon service registry**

  Helidon Talon makes the tagging client injectable.

- **Converts tag sets to and from persisted tag slugs**

  The integration helps services transform request tags into the persisted slug form used by OCI internals.

- **Supports freeform, defined, and system tags**

  The client handles the tag categories services commonly need to persist and inspect.

- **Helps connect resource write paths with authorization flows**

  Tag data often affects authorization and resource lifecycle behavior, so the module supports write-path integration.

## Tagging Configuration

- **Config root is `oci.tagging`**

  Tagging settings live under this configuration subtree.

- **Client is available from the Helidon service registry**

  Service code gets the client from injection or registry lookup.

- **Metrics emission can be enabled or disabled**

  The tagging client can be configured to emit or suppress its own metrics.

## Tagging Example

- **Inject `TaggingClient` from the service registry**

  Application code receives the internal tagging client through injection.

- **Convert request tags into a persisted tag slug**

  The example shows turning tag maps into the binary slug used for persistence.

## Workflow

- **Integrates OCI Workflow-as-a-Service with Helidon Talon services**

  Helidon Talon provides Helidon wiring for WFaaS client usage.

- **Inject the native WFaaS `WorkflowClient` directly**

  Application code can use the native client rather than a Helidon Talon-specific wrapper.

- **Use worker and poller clients through Helidon service registry names**

  The module exposes differently named clients for launching work and polling status.

- **Configure endpoint, domain, timeouts, retry policy, and worker identity**

  The Workflow client can be shaped through configuration for the target WFaaS environment.

## Workflow Configuration

- **Config root is `oci.workflow`**

  Workflow settings live under this configuration subtree.

- **Endpoint settings configure WFaaS routing and timeouts**

  Endpoint and timeout fields control how the client reaches WFaaS.

- **Worker and poller clients are exposed through service registry names**

  Named registry bindings let code select the worker or poller role.

## Workflow Example

- **Inject default worker `WorkflowClient`**

  The default unqualified client is intended for launching workflow work.

- **Serialize JSON request with Helidon `JsonBinding`**

  The workflow payload is application-specific JSON serialized to bytes before launch.

## Integrated Client Versions

No bullet points.

## Heliport

- **Migrates Dropwizard apps to Helidon Talon apps**

  Heliport helps teams move existing Dropwizard 3 Maven applications toward Helidon 4 SE and Helidon Talon.

- **Combines OpenRewrite recipes with Codex-guided planning and validation**

  The migration combines automated recipes with Codex analysis and proof steps.

- **Produces a migrated tree, `.heliport/` report, action plan, and proof evidence**

  The output is not just code changes; it includes a handoff report and validation evidence.

- **Still improving and can benefit from a Codex analysis after migration**

  Heliport is useful today, but migrated applications should still be reviewed by Codex and application owners.

  The loop on the Codex box represents iterative analysis, cleanup, and validation before the migrated candidate becomes a final Helidon application.

## Data Plane Reference App

- **Runnable Helidon Talon reference service under `examples/data-plane`**

  Teams can inspect and run a concrete Helidon Talon data-plane application in the repository.

- **Combines request ID, OCI error responses, Identity, Kiev, and Audit**

  The example demonstrates multiple modules working together in one service.

- **Demonstrates signed robot create/update/delete APIs with `@AuthorizationPermission`**

  The app shows signed requests and authorization annotations on resource operations.

- **Uses in-memory Kiev by default so teams can run and test without external infrastructure**

  The default setup is intentionally easy to run locally.

## What Teams Should Take Away

- **Consider Heliport + Codex for Dropwizard-to-Helidon migration work**

  Teams migrating existing Dropwizard services should evaluate the guided migration workflow.

- **Consult the Data Plane Reference App before wiring a new DP service**

  The reference app should be the first stop for data-plane service patterns.

- **Let `helidon-oci-envconfig` resolve region, AD, FD, and domains**

  Teams should avoid custom environment-discovery code when Helidon Talon already provides it.

- **Add only the Helidon Talon integrations your service needs**

  Helidon Talon is modular; services should adopt the relevant pieces without pulling in unnecessary integrations.

- **Stay tuned for additional features and integrations in upcoming releases!**

  Helidon Talon is a foundation, and the integration set is expected to grow.

## Resources

- **Helidon User's Channel: `#helidon-users`**

  Direct people to the Slack channel for questions and discussion.

- **Helidon Talon docs: `https://helidon.oraclecorp.com/docs/2.0.0-RC1/`**

  This is the published documentation site for the Helidon Talon 2.0.0 RC1 release.

- **Helidon Talon: `https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon`**

  This is the main repository for the Helidon Talon code and examples.

- **Heliport: `https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/heliport`**

  This is the repository for the Dropwizard-to-Helidon migration tooling.

- **Helidon Talon reference app: `examples/data-plane`**

  This points teams to the local reference app inside the Helidon Talon repository.

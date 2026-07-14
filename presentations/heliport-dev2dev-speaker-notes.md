# Heliport Dev2Dev Speaker Notes

## Slide 1: Heliport

- Open with the audience's problem: a framework migration is not an import rewrite; it changes runtime responsibilities across build, bootstrap, routing, injection, configuration, OCI integrations, and tests.
- The goal today is to make a Heliport run predictable: where to get it, what it changes, what it refuses to guess, and how to finish the work from its reports.

## Slide 2: OCI will be mandating Dropwizard migration to Helidon and Talon

- Frame the OCI decision using the target platform described in the Talon presentation: Helidon 4, JDK 25, virtual threads, declarative HTTP, compile-time injection, and standardized OCI integrations.
- Talon is the OCI-native service framework built on Helidon, providing standardized integrations and operational patterns for OCI control-plane and data-plane services.
- Use ownership as shorthand for runtime responsibility: who creates and injects a client, registers and serves a route, starts and stops a lifecycle component, supplies configuration, or applies an OCI integration.
- Heliport removes old wiring only after the corresponding Helidon or Talon behavior is confirmed. Otherwise it preserves the code or reports a developer action.

## Slide 3: 450+ OCI teams need a faster path to Helidon

- Use this slide to establish why Heliport exists, then move directly into what the mandated target changes. The workflow is covered later in the presentation.
- The 450+ figure is the number of affected OCI teams. The seven-to-nine-month manual estimate comes from SPLAT fleet sizing.
- Separate the two automation milestones: the benchmark migration converted the code in roughly two hours; the service reached production in four months after application review, follow-up work, validation, and normal delivery processes.
- Benchmark proof: 175 files changed, with 9,737 insertions and 3,034 deletions; compile, unit, and install gates passed. Throughput increased from 8,117 to 16,221 requests per second per core, a 99% improvement.
- Treat the performance result as proof from this benchmark, not as a guaranteed result for every service.

## Slide 4: Modernization works best as an ordered sequence

- Strongly recommend the JDK migration first. It separates compiler, dependency, and source compatibility changes from the framework migration.
- Actions are the developer-facing list of remaining migration issues. The service team resolves application work and routes Heliport defects or environment blockers through the support paths covered later.
- Heliport never pushes. Even with branch and commit options, the application team reviews the changes, pushes the branch, and creates the PR.

## Slide 5: Heliport combines OpenRewrite recipes with Codex guidance

- Explain why both parts are needed. OpenRewrite recipes perform known, repeatable code and build changes; Codex analyzes the repository, guides the workflow, runs validation, and interprets the results.
- Heliport is a Codex skill, not a separate one-shot binary that blindly changes a tree.

## Slide 6: Heliport uses repository facts instead of guessing

- Repository facts are concrete source symbols, dependencies, configuration keys, generated outputs, and tests. Heliport uses those facts to select a known transformation instead of inferring behavior from a compile failure alone.
- If no safe, known target exists, Heliport preserves the code and reports a developer action rather than inventing an application-specific migration.

## Slide 7: Install Heliport as a global Codex skill

- The full Oracle DevOps URL is shown here and repeated on the closing resources slide. A symlink lets the installed skill follow updates in the checkout.
- The direct .agents skill directory can also be linked, but the repository-root link is the simplest documented path.

## Slide 8: Run Codex from the application workspace and start Heliport

- The skill must run from the target repository. Positional project-path arguments are not supported.
- If the Maven settings file selects another localRepository, grant that path instead of ~/.m2. Keep normal corporate Maven settings and proxy behavior intact.
- After Codex starts, verify the intended model and reasoning level, then invoke $heliport-migrate. If time permits, this is enough context to begin a live demonstration without a separate transition slide.

## Slide 9: Heliport migrates the entire service, not only Java source

- Use this as the pivot into the technical bulk of the talk. The next slides walk these areas in the order developers will encounter them.

## Slide 10: Establish the Helidon Maven model before runtime migration

- The initial build phase creates a usable Helidon target before runtime recipes run. It also avoids two common mistakes: adding every Talon module and deleting every old dependency by name.
- Maven install remains last so enforcer, dependency analysis, style, coverage, and packaging checks remain visible.

## Slide 11: BOM imports centralize version convergence

- Show the separation of responsibilities: dependencyManagement chooses versions; child dependencies express actual module use.
- The application keeps declaring SLF4J and OCI SDK modules where they are used, but their versions move to the reactor's imported BOMs.

## Slide 12: The root POM selects versions; modules declare dependencies

- The root chooses a compatible platform; modules state only what they actually compile or run against.
- Heliport reads the effective parent and reactor structure. External-parent modules may correctly retain a local version, and aggregator-only POMs should not gain runtime dependencies.
- BOMs and JPMS serve different layers: Maven selects artifact versions; module-info.java expresses readability and exports. Both should reflect the features the module actually uses.

## Slide 13: Child modules declare only the platform dependencies they use

- A child POM contains direct dependencies for its source and runtime providers without repeating managed versions.
- When the service has a module descriptor, Heliport aligns requires entries with the same repository usage facts used for Maven dependencies.
- Each dependency is added, managed, kept, removed, or reported as a developer action. A clean tree results from checking every use before removal.

## Slide 14: Keep generated OpenAPI and handwritten code aligned

- Heliport checks generated contracts before runtime migration and again afterward. Generated resources can define statuses, headers, response types, and method signatures that handwritten implementations must preserve.
- Examples of service-side legacy output include generated AbstractFooBaseResource contracts, javax.ws.rs or jakarta.ws.rs server types, JAX-RS Response and HttpHeaders carriers, Servlet request or response types, and old generated response wrappers.
- Remote and out-of-project OpenAPI references are never fetched implicitly; the service team must make those specifications available as explicit build inputs or retain the compatibility path.

## Slide 15: OpenAPI generation moves from JAX-RS bases to Helidon contracts

- The Helidon OpenAPI generator extension is a separately versioned toolchain. Heliport selects that version from its generator policy or explicit overrides; it is not derived from ${helidon.version}.
- Heliport preserves each execution's input specification and base package when a module generates multiple service contracts.
- When another module in the same Maven reactor produces the specification, Heliport must update the generator input path and preserve the build order. Moving dependency:unpack without updating that path can run generation before the specification exists.
- Remote specifications are never fetched implicitly. The service team must provide a remote specification as an explicit build input or retain the existing compatibility path.

## Slide 16: Handwritten resources implement the generated Helidon API

- The exact generated signature varies by contract; do not claim one universal method shape.
- Heliport adapts one resource family at a time, preserving status, body, declared headers, auth inputs, model builders, enum shapes, temporal types, and handwritten implementation behavior.
- Generated endpoints are overwritten on each build, so business logic must remain in stable application classes. Do not hand-edit target/generated-sources/openapi.

## Slide 17: Server-side JAX-RS resources become declarative Helidon endpoints

- The annotations look intentionally familiar, but the runtime is different. Resources become discovered Helidon services and endpoints; Heliport removes environment.jersey().register calls only after those endpoints exist.
- This slide covers server-side JAX-RS. JAX-RS client APIs retained at OCI SDK or provider compatibility boundaries are classified and reported separately.
- Heliport also normalizes class and method paths when trailing-slash behavior would otherwise change.

## Slide 18: JAX-RS request context becomes explicit Helidon input

- This target assumes that Heliport selected the Talon request-ID webserver and OCI identity integrations. Otherwise Heliport retains ServerRequest or a compatibility adapter and reports the remaining work.
- The injected Principal is com.oracle.pic.identity.authentication.Principal, not java.security.Principal. Direct resolution requires @Identity.Authenticated or a supported Auth SDK authorization annotation.
- Prefer typed route parameters when they preserve the contract. Use ServerRequest only when the endpoint genuinely needs broader request metadata.
- Principal-only SecurityContext use can move to OCI identity interception. Role checks and principal enrichment remain application policy.
- UriInfo, Servlet wrappers, body replay, proxy behavior, async timeouts, and SSE broadcasters require an equivalent Helidon implementation or an explicit developer action.

## Slide 19: Response migration keeps observable HTTP behavior

- Simple 200 responses can return the entity directly. ServerResponse is used when response metadata or stream behavior is observable.
- Use the fluent ServerResponse API so status, headers, and the terminal send remain one visible response operation.
- Downloads also preserve content-md5, content-encoding, downstream header replay, and whether the original InputStream is sent without buffering.
- Complex exception mappers, gzip wrappers, proxy context, response wrappers, and body replay remain developer work until an equivalent Helidon implementation is identified.

## Slide 20: Dropwizard bootstrap becomes discovered Helidon services

- Walk through the old Environment as the central Dropwizard registration point. Every line must move to an endpoint, lifecycle service, health feature, task endpoint, JSON/config service, or action item.
- Custom Jetty connector, TLS, proxy, JMX, and listener behavior prevents broad bootstrap deletion until an equivalent WebServer implementation is identified.

## Slide 21: Dropwizard lifecycle hooks become service lifecycle

- This direct mapping applies when the lifecycle is event-independent and ordering is already represented by service dependencies.
- ServerLifecycleListener moves to ServerLifecycle.afterStart(WebServer) only when it does not depend on Jetty connector, session, servlet, or custom listener state.
- Conditional startup, custom TLS, JMX, SNI, mTLS, HTTP/2, and proxy-protocol behavior remain developer work until an equivalent Helidon implementation is identified.

## Slide 22: Health checks and admin tasks keep their behavior

- Health tests migrate with the production health check. Status and diagnostic content must remain equivalent, not only the class name.
- Task naming, authorization, and servlet-coupled output policy can require a service-team decision.

## Slide 23: Dropwizard configuration moves to the service that uses it

- Configuration moves to the production service that uses it and, for Talon integrations, into the integration's documented configuration root.
- Review computed Java configuration, Dropwizard DataSize conversion, custom TLS or connector settings, secret reload, and operational override precedence carefully.

## Slide 24: Guice provider methods become Helidon supplier services

- A Supplier is the closest target for a Guice @Provides method whose product is not itself a discoverable service class. The cached field makes the original singleton scope explicit.
- Lifecycle providers, runtime injector lookups, multibindings, and application-specific factory behavior may need specialized migration or developer action.

## Slide 25: Guice constructor injection moves to Helidon Service Registry

- The target architecture expresses composition on the service and constructor. Helidon code generation creates the service metadata instead of relying on a central Guice module binding.
- Service.Named preserves the Guice qualifier while Service.Inject marks the injection constructor.
- Custom scopes, assisted injection, AOP, listeners, and side-effect injection may still require developer attention.

## Slide 26: A Guice module disappears only after every binding moves

- Removing AbstractModule too early can lose runtime registrations even when compilation succeeds.
- Heliport removes the module only after safe bindings have target services and anything it cannot safely migrate is listed as remaining developer work.

## Slide 27: Heliport adds only the Talon integrations an application uses

- This is the feature-selection rule. A module appears because concrete repository usage shows the application needs it, not because every migrated service gets the same platform bundle.
- Some repositories implement shared integration code used by other services. Heliport must recognize that provider role instead of automatically adding the consumer-side Talon module.
- Repositories that use custom transports may retain compatibility code or receive developer actions instead of a standard Talon integration.

## Slide 28: Heliport migrates each OCI integration as a complete feature

- This slide introduces feature family as a useful Heliport term: the complete set of build, source, configuration, runtime, and test elements associated with one integration.
- Heliport also distinguishes an application that uses the integration from a repository that provides its implementation, so client artifacts are not added to the wrong module.
- Use the Talon RC2 presentation as the integration reference: this talk explains how Heliport reaches those target code and configuration forms from Dropwizard, JAX-RS, and Guice code.
- The same family contract applies to Workflow/WFaaS, Limits, Secret Service, Request ID, SPLAT, Audit, Object Storage, Identity Client, Tagging, Error Code, and the other Talon integrations. They are intentionally not expanded in this talk.

## Slide 29: Identity: Jersey filters become generated Talon interceptors

- This target deliberately mirrors the Talon RC2 Identity example: @RestServer.Endpoint, @Http.Path, @Http.POST, @AuthorizationPermission, @Http.Entity, and direct Principal injection.
- Principal is com.oracle.pic.identity.authentication.Principal. The same business call now receives that type directly instead of reading and casting a JAX-RS SecurityContext.
- AuthorizationPermission uses the authorization path, which includes authentication. Use @Identity.Authenticated when the endpoint needs authentication without service-side authorization.
- Heliport collects the identity dependency, Dropwizard/Jersey registration, Guice filter or binder, auth configuration, request parameters, and endpoint annotations as one feature family.
- Simple authentication-only name bindings and unused filter shells can migrate automatically. Custom route authorization, SAML, principal enrichment, local/mock behavior, and auth-provider construction remain explicit developer work.
- Heliport preserves an existing permission contract; it does not invent ORDER_CREATE or choose a service name for the application.

## Slide 30: Kiev: Guice-built stores become named Talon services

- This target mirrors the Talon RC2 Kiev examples: a named MappedDataStore, an oci.kiev.data-stores entry, and @KievTransaction around a clearly identified service method.
- Kiev is the clearest feature-family example because the POM, Guice provider, Dropwizard configuration, named store, transaction boundary, runtime consumer, and tests must agree.
- IN_MEMORY is the RC2 demonstration backend. Heliport preserves the backend, locality, authentication, app name, store name, and read/write limits found in the application rather than selecting demonstration values.
- Application-specific DataStoreConfigBase construction and transaction-provider wrappers remain developer work until their behavior is understood. Heliport does not infer commit, abort, retry, or failure policy.

## Slide 31: Metrics: Codahale and T2 configuration become Helidon + OCI publishing

- The target code and configuration deliberately mirror the Talon RC2 Metrics example and publisher configuration.
- Heliport can rewrite safe Codahale @Timed, @Metered, @Gauge, monotonic @Counted, and common MetricRegistry operations, then add the Helidon metrics API dependency when required.
- Recognized legacy T2 and metricsConfig keys map to metrics.publishers entries, including endpoint, project, fleet, region, scope name, include/exclude filters, and detailed timing settings.
- This example has no explicit sample interval, so the target omits it and Talon's default applies.
- Non-equivalent Counted behavior, custom reporters, trust-store behavior without a Talon equivalent, metric naming, dimensions, and retention or fleet policy remain explicit service-team decisions.

## Slide 32: A feature family is complete only when every part has migrated

- This is the completion rule Heliport applies: the target integration is active, and legacy construction is either removed or explicitly retained for compatibility.
- A green compile is not enough if the application can still run through the legacy provider or test harness.

## Slide 33: Panga route tests move to Helidon @RoutingTest

- Heliport replaces the obsolete Dropwizard JUnit extension with Helidon @RoutingTest.
- The @SetUpRoute method, DirectClient constructor injection, request path, and behavioral assertions remain unchanged. @RoutingTest exercises the route without opening a server socket.

## Slide 34: Heliport runs migration work in five ordered stages

- The full 13-phase sequence is in the appendix. For the main talk, teach the five jobs: understand, prepare, migrate, converge, and validate.
- A phase tells you where Heliport is working or stopped. The final status tells you the outcome and whether the report contains required actions or continuation instructions.
- A phase completes only when its assigned work is migrated, intentionally retained for compatibility, or reported as a remaining action. --resume continues from the saved .heliport state.

## Slide 35: Every run records its state and reports under .heliport

- Application teams start with migration-report.md and are not expected to interpret the JSON manually. The other files support Codex continuation and Heliport diagnosis.
- Before sharing .heliport, follow normal source and log handling policy. Diagnostic files can contain source paths, coordinates, repository names, build output, and environment details; they must not contain credentials or private keys.

## Slide 36: Start with migration-report.md for results and next steps

- Do not reduce the report to complete versus failed. It separates migration outcome from final validation and gives a continuation brief.
- The report should be sufficient for the application team. Ask for deeper phase JSON only when Codex or a Heliport maintainer needs it.

## Slide 37: Each remaining action identifies who should handle it

- The responsible-party label is exact application-action-plan.json vocabulary. It identifies long-term responsibility, while repair_authority identifies the immediate actor.
- A heliport_product action can be a Heliport enhancement candidate with codex_app_repair_authorized set to true. In that case, the customer can ask Codex to apply the evidence-backed application repair now, retain the enhancement evidence, and resume Heliport.
- When local_repair_disposition is no-safe-local-repair-proven, the customer should not improvise a workaround. Share the branch and .heliport with Heliport maintainers or use an updated Heliport version that resolves the gap.

## Slide 38: Migration status describes the current outcome and next step

- Status describes the current migration outcome and next step; it should not be reduced to a simple pass or fail.
- MIGRATION_SUCCEEDED_PENDING_VALIDATION means requested proof gates have not all run, so final validation is not yet determined.
- Generated-source, compile, unit-test, and install checks retain their actual results and are reported separately from migration outcome.

## Slide 39: Validation increases in scope from generated sources to Maven install

- Current Heliport reporting can represent substantial success with required actions. Avoid teaching a single pass/fail interpretation.
- Install runs last so enforcer, dependency analysis, PMD, Checkstyle, SpotBugs, JaCoCo, and packaging remain visible rather than being skipped.

## Slide 40: Resume continues from the saved .heliport state

- Resume is the normal continuation mechanism. It is not only for crashes; it is also how teams continue after resolving application or environment work.
- The report identifies the stopping phase and responsible party. Do not delete .heliport state unless a Heliport maintainer specifically asks for a clean restart.
- Common blockers include the wrong workspace, dirty worktree, Java/toolchain gaps, an unwritable Maven cache, missing network/proxy settings, and Git metadata permissions.
- The appendix expands the rerun contract: workflow state, current repository findings, repeatable transformations, and validation status must agree.

## Slide 41: Start with the migration guide; use Slack and JIRA when needed

- Point customers to the Heliport Migration Guide first. Use #helidon-users for questions and early triage. Use the HLDN JIRA project for bugs, concerns, and incidents, and always select the Heliport component.
- Ask the reporter for a source-control reference to a branch containing both the migrated files and .heliport. That gives maintainers the application state, report, actions, phase results, and diagnostic details needed to reproduce the issue.
- The branch must follow normal access controls and secret-handling policy. Credentials, private keys, tokens, and unrelated sensitive configuration must never be committed for support.

## Slide 42: A good migration ends with clear next steps and current validation results

- Close by returning to runtime responsibility. Heliport automates repeated transformations, but a trustworthy migration keeps service policy and unsupported behavior visible.
- Invite questions around a specific migration family, report action, or planned service.

## Slide 43: Resources

- This slide mirrors the Talon RC2 resources close and gives developers the exact documentation, repositories, and support paths used by both presentations.
- Lead with the Heliport Migration Guide. Use #helidon-users for Heliport installation, migration, and report questions. The Talon repository and RC2 documentation remain the detailed reference for migrated OCI integrations.

## Slide 44: One command starts the guided migration

- This is an appendix reference for command options. The core presentation shows the basic invocation on the Codex workspace slide.
- Repeat that --commit is local and Heliport never pushes.

## Slide 45: Compile-time service discovery requires processor wiring

- Heliport treats compiler-plugin migration as runtime wiring, not build cleanup.
- The canonical processor bundle is io.helidon.bundles:helidon-bundles-apt at the Helidon version.
- Heliport checks Maven configuration twice: an early pass makes source transformations parseable, and a final pass sees the actual dependencies after runtime and test migration.

## Slide 46: Heliport migrates Servlet, Jetty, and Netty code based on behavior

- This slide should reduce anxiety around hard runtime integrations. Heliport does not promise that every container-specific customization has a generic Helidon equivalent.
- An unused Netty dependency and an application-defined protocol pipeline are different migration problems and receive different results.

## Slide 47: Tests migrate after the production behavior they exercise

- Production behavior migrates before its tests so the target endpoint, health check, task, generated contract, or service wiring is known before Heliport changes the test harness.
- The main Panga example shows the supported route-test migration in detail. Use this appendix slide to emphasize migration order and preservation of behavioral assertions.

## Slide 48: The full workflow has thirteen ordered phases

- Use this when someone asks where a reported phase sits in the overall workflow.

## Slide 49: Runtime migration waves run in dependency order

- Runtime owner wave is Heliport's report term for a related group of framework transformations and their completion checks.
- Not every wave changes every service. Heliport selects applicable work from the migration manifest, then preserves this dependency order so later transformations do not invalidate earlier prerequisites.

## Slide 50: A safe rerun uses saved state and current application files

- Idempotence is useful Heliport vocabulary here: it means rerunning a completed recipe against unchanged migrated code should not produce the same edits again.
- A prior passing validation is not silently reused after relevant source changes; final status follows the current rerun results.

## Slide 51: An owner frontier marks behavior Heliport cannot safely infer

- Use this as a checklist when a team asks whether Heliport will fully automate a particular service. The slide defines owner frontier because that exact term can appear in Heliport diagnostics.

## Slide 52: Command and support reference

- Keep this slide available during Q&A. The main Resources slide contains the full repository URLs.

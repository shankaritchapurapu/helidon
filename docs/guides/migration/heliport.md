# Heliport

Heliport is a Codex skill for migrating Dropwizard 3-based OCI services to Helidon 4.5 SE declarative applications using the Helidon OCI integration library, Talon.

It combines OpenRewrite recipe execution with Codex-guided planning, phase-by-phase validation, and customer-facing handoff reports. The intended output is a migrated application tree, validation evidence, and a clear action plan for any work that still needs application-owner input or future Heliport support.

## Overview

Heliport runs a Dropwizard-to-Helidon migration workflow for Maven repositories that contain Dropwizard 3-based OCI services. It is intended for end-to-end migrations where the output includes application changes, validation evidence, and a final migration report under the target application's _.heliport/_ directory.

The workflow does the following:

- validates that Codex is running from the target application workspace
- checks Git/worktree state and optional branch settings
- inventories the Maven reactor and application source shape
- plans the migration from supported Dropwizard, JAX-RS, Guice, OpenAPI, test, build, OCI, and Talon integration patterns
- reconciles the Helidon/OpenRewrite toolchain and Maven build shape
- prepares generated OpenAPI contract owners when applicable
- applies runtime-owner migration waves using Heliport's OpenRewrite recipes
- aligns generated sources, handwritten resources, and test fixtures
- runs generated-source, compile, unit, and Maven install proof gates
- writes _.heliport/migration-report.md_, validation evidence, and a structured application action plan
- optionally creates one local commit after final reporting

Heliport is _evidence-first_. If the current recipe set cannot safely migrate a repeated pattern, the run records the evidence and the remaining action instead of hiding the problem behind ad-hoc source edits.

## Heliport Migration Guide

For a deeper explanation of what Heliport changes during a migration, see the [Heliport Migration Guide](heliport-migration-guide.md). It covers the Maven build, generated OpenAPI/model sources, runtime family waves, OCI integrations, configuration, metrics, logging, and the known owner-action frontiers that may remain after an automated run.

## Use The JDK Migration Skill First

Heliport targets Helidon 4.5 SE declarative applications and the Talon integration library on Java 25. Application teams should strongly prefer to complete the Java 25 migration before running Heliport, so that JDK, toolchain, dependency, and source-compatibility work is separated from the Dropwizard-to-Helidon-and-Talon migration.

Recommended order:

1. Use the JDK migration skill to migrate the application to Java 25.
2. Verify the Java 25 branch.
3. Run Heliport from the Java-25-ready application workspace.

The JDK migration skill is documented here: [Autonomous JDK Migration with Codex](https://confluence.oraclecorp.com/confluence/display/JavaPM/Autonomous+JDK+Migration+with+Codex).

If an application has not moved to Java 25 first, Heliport will make a best effort to migrate the Java and toolchain pieces as part of the run. Starting from a Java-25-ready branch is still the preferred path because it keeps JDK compatibility work separate from the Dropwizard-to-Helidon-and-Talon migration.

## Where To Get The Skill

The Heliport repository landing page is available in Oracle DevOps: [Heliport repository](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/heliport)

After cloning, install the Codex skill by symlinking the Heliport checkout into your global Codex skills directory:

```shell
mkdir -p ~/.codex/skills
ln -s /abs/path/to/heliport ~/.codex/skills/heliport-migrate
```

The direct skill directory can also be symlinked:

```shell
mkdir -p ~/.codex/skills
ln -s /abs/path/to/heliport/.agents/skills/heliport-migrate ~/.codex/skills/heliport-migrate
```

If the skill cannot resolve the support checkout from the symlink, set HELIPORT_SUPPORT_ROOT to the Heliport checkout path before launching Codex:

```shell
export HELIPORT_SUPPORT_ROOT=/abs/path/to/heliport
```

## Prepare The Application Workspace

Start Codex from the directory of the application you want to migrate.

Before invoking the skill, confirm:

- the application is a Dropwizard 3-based OCI service in a Maven repository
- the application has preferably already been migrated to Java 25 using the JDK migration skill, or you are ready for Heliport to make a best-effort Java 25 migration as part of the run
- the worktree is clean enough for a migration branch or you are prepared to review local changes carefully
- Maven can resolve the application's normal dependencies
- the configured Maven local repository is writable by Codex, usually ~/.m2
- Codex has network access when Maven or OpenRewrite need artifacts that are not already local
- you know the Maven settings file path, if the project needs one
- Codex can write the application's `.git` directory if you plan to use --branch or --commit

Recommended Codex launch command:

```shell
cd /abs/path/to/dropwizard-app
codex -s workspace-write --add-dir ~/.m2 -c sandbox_workspace_write.network_access=true
```

If Maven uses a custom `<localRepository>`, replace `/absolute/path/to/maven-repository` below with that configured path:

```shell
cd /abs/path/to/dropwizard-app
codex -s workspace-write --add-dir /absolute/path/to/maven-repository -c sandbox_workspace_write.network_access=true
```

If you plan to use --branch or --commit, Codex must also be allowed to write the application's Git metadata:

```shell
cd /abs/path/to/dropwizard-app
codex -s workspace-write --add-dir ~/.m2 --add-dir /abs/path/to/dropwizard-app/.git -c sandbox_workspace_write.network_access=true
```

## Run The Skill

Command format:

```text
$heliport-migrate [--resume]
                  [--branch <name>] [--commit]
                  [--maven-settings <path>]
```

Common Codex prompt examples:

```text
$heliport-migrate
$heliport-migrate --branch heliport-migration
$heliport-migrate --branch heliport-migration --commit
$heliport-migrate --maven-settings /abs/path/settings.xml
$heliport-migrate --resume
```

Flags:

| Flag | Meaning |
| --- | --- |
| `--resume` | Resume from the current `.heliport/` state after an interrupted or blocked run. |
| `--branch <name>` | Create or switch to the requested local Git branch before Heliport writes migration artifacts. |
| `--commit` | Create one local commit after final reporting. Must be paired with `--branch <name>`. Heliport never pushes. |
| `--maven-settings <path>` | Use an explicit Maven settings file when the project needs one. |
| `--dry-run` | Advanced preview mode. Runs setup and owner-wave dry-run evidence without applying recipe changes or terminal proof gates. |
| `--validate-artifacts` | Advanced support mode. Validates existing Heliport artifacts and refreshes reports without running migration phases. |

Heliport does not push changes to a remote repository. Review the migration report and application changes before pushing or opening a pull request.

## Workflow Phases

The migration runs in strict phase order.

```
preflight
  -> inventory
  -> manifest-planning
  -> harvest
  -> toolchain-prime
  -> generated-contract-prelude
  -> pre-runtime-preparation
  -> runtime-owner-waves
  -> post-runtime-generated-alignment
  -> test-fixture-alignment
  -> pre-validation-convergence
  -> validation
  -> validation-closeout
```

| Phase | Main output | Exit gate |
| --- | --- | --- |
| Preflight | Application root, Git/worktree state, environment checks, initial `.heliport/` state | Target workspace and prerequisites are valid, or a blocker is reported. |
| Inventory | Maven reactor and source-shape inventory | Required inventory evidence exists. |
| Manifest planning | Migration plan for supported source families | Planned migration surface is recorded. |
| Harvest | Source-evidenced migration facts and supported pattern selection | Harvest evidence is available for rewrite planning. |
| Toolchain prime | OpenRewrite/Helidon toolchain and Maven reconciliation | Migration toolchain is ready for recipe execution. |
| Generated contract prelude | Generated OpenAPI contract-owner preparation when applicable | Generated contract ownership is prepared or a precise blocker is reported. |
| Pre-runtime preparation | Build, config, and compatibility preparation before runtime migration | Runtime-owner waves can start safely. |
| Runtime owner waves | Dropwizard, JAX-RS, Guice, OCI, Talon, generated-resource, and related migrations | OpenRewrite-owned runtime migration waves complete or report exact residual work. |
| Post-runtime generated alignment | Generated and handwritten resource alignment | Generated/source boundary evidence is reconciled. |
| Test fixture alignment | Test fixture and test API migration | Test-specific migration evidence is complete. |
| Pre-validation convergence | Bounded cleanup before proof gates | Validation can run against the migrated tree. |
| Validation | Generated-source, compile, unit, and install proof gates | Validation evidence is recorded. |
| Validation closeout | Bounded source-evidenced terminal repairs and final report refresh | Final customer handoff artifacts exist. |

## Artifacts And Reports

Artifacts are written under the target application:

```text
<app-root>/.heliport/
  workflow-state.json
  preflight.json
  validation-report.json
  application-action-plan.json
  migration-report.md
  evidence/
    agent-migration-status.json
    structural-migration-harvest-packets.json
  phase-results/
  rewrite/
```

Start with:

```text
<app-root>/.heliport/migration-report.md
```

That report summarizes what changed, whether the migration is complete, what the application team should do next, and which validation gates were attempted as evidence. Use application-action-plan.json for structured follow-up and automation-friendly handoff details.

Root-level reports from earlier tools, such as _migration-report-jdk25.md_, are historical context. Current Heliport status is authoritative only under _.heliport/_.

## Migration Status Values

The customer report includes a high-level migration status:

| Status | Meaning |
| --- | --- |
| `MIGRATION_COMPLETE` | Heliport found no customer-visible remaining work. |
| `MIGRATION_INCOMPLETE` | Heliport made substantial progress, but structural migration work remains. Complete the action plan and rerun validation. |
| `MIGRATION_BLOCKED` | Heliport stopped before completing the phase contract. Fix the reported blocker or prerequisite, then rerun or resume. |
| `VALIDATION_FAILED` | Structural migration is not the primary remaining queue, but generated-source, compile, unit, or install validation failed. Fix validation failures before handoff. |

Generated-source, compile, unit, and install failures are not cosmetic. They remain failed validation gates until rerun evidence proves otherwise.

## FAQ

### What applications should use Heliport?

Use Heliport for Dropwizard 3 based OCI services that need to migrate toward Helidon 4.5 SE declarative applications using Talon. It is best suited for real application repositories with normal Maven metadata, tests, dependency settings, and Git history.

### Should I run the Java 25 migration first?

Yes. Heliport assumes the target platform is Helidon 4.5 SE declarative applications using Talon on Java 25. Run the JDK migration skill first whenever possible, verify that branch, then run Heliport.

### Can I run Heliport from outside the application repository?

No. Open Codex from the application workspace and invoke Heliport Migrate there. Positional project-path arguments are not supported.

### Does Heliport push changes?

No. Heliport never pushes. With `--branch <name> --commit`, it can create one local handoff commit after final reporting. The application team must still review, push, and open pull requests through the normal process.

### What does Heliport leave to application owners?

Heliport tries to make deterministic framework migration choices when they preserve application behavior. It reports application-owner actions only when a safe migration choice depends on business functionality, public API/schema semantics, security or authorization policy, persistence/data ownership, deployment policy, or source-approved dependency/version ownership.

### What if Heliport reports `MIGRATION_INCOMPLETE`?

Read _.heliport/migration-report.md_ first, then use _.heliport/application-action-plan.json_ for structured follow-up. The report should explain what Heliport migrated, what remains, and whether the remaining work is application-owned or a Heliport-supported migration frontier.

### Can I use a custom Maven settings file?

Yes. Use:

```text
$heliport-migrate --maven-settings /abs/path/settings.xml
```

Project-local _settings.xml_, _maven-settings.xml_, and _.mvn/settings.xml_ are considered when no explicit settings path is supplied.

## Troubleshooting

### Skill Is Not Discovered

Check that the skill is installed in the active Codex skills directory:

```text
~/.codex/skills/heliport-migrate/SKILL.md
```

If needed, recreate the symlink to the Heliport checkout and restart Codex from the application workspace.

### Heliport Cannot Resolve The Support Checkout

The installed skill must be able to find the Heliport checkout containing .codex/bin/heliport-core, openrewrite-recipes/pom.xml, and rewrite/rewrite.yml.

If resolution fails, set:

```shell
export HELIPORT_SUPPORT_ROOT=/abs/path/to/heliport
```

Then restart Codex from the application workspace.

### Maven Local Repository Is Not Writable

Heliport uses the project's normal configured Maven local repository for ordinary dependency resolution. If preflight reports that the repository is not writable, restart Codex with write access to that repository:

```shell
codex -s workspace-write --add-dir ~/.m2 -c sandbox_workspace_write.network_access=true
```

If Maven uses a custom `<localRepository>`, pass that path with `--add-dir` instead of ~/.m2.

### Maven Or OpenRewrite Cannot Download Artifacts

If dependencies are not already present locally, Codex needs sandbox network access and the same proxy/settings configuration Maven normally uses. Confirm network access, Maven settings, and proxy values, then rerun or resume.

### Branch Or Commit Fails

When Codex runs with _-s workspace-write_, Git branch creation and commit creation require write access to the application's _`.git`_ directory. Restart Codex with:

```shell
codex -s workspace-write --add-dir ~/.m2 --add-dir /abs/path/to/dropwizard-app/.git -c sandbox_workspace_write.network_access=true
```

Then resume:

```text
$heliport-migrate --resume
```

### Preflight Fails

Common causes:

- Codex was started outside the application repository.
- The repository is not a Dropwizard 3 based OCI service in a Maven repository.
- The worktree has unexpected local changes.
- Java 25 migration or the required Maven toolchain needs follow-up.
- Maven cannot resolve the normal project dependencies.
- The Maven local repository is not writable.
- Network or proxy configuration is missing.

Resolve the reported issue and rerun the same command. Use _--resume_ if _.heliport/_ state was created.

### A Phase Fails

Start with:

```text
<app-root>/.heliport/phase-results/<phase>.json
<app-root>/.heliport/evidence/agent-migration-status.json
<app-root>/.heliport/migration-report.md
```

Do not skip ahead manually. Fix the reported blocker or complete the reported action, then resume from the saved state:

```text
$heliport-migrate --resume
```

# Environment Configuration

---

## Overview

The `env-config` module provides the lazy `oci-env` Helidon config source and companion
declarative region providers for public OCI SDK `com.oracle.bmc.Region` and PIC commons
`com.oracle.pic.commons.util.Region`. It mirrors the OCI environment semantics used by
[EnvironmentTypeSafeReader](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/shepherd/shepherd-tips-and-tricks/reducing-configuration-files-for-dropwizard-services.htm?Highlight=EnvironmentTypeSafe#using-environmenttypesafereader),
publishes derived `oci.env.*` values into Helidon configuration, and can import dynamic
core-regions metadata before resolving location-dependent values.

---

## Maven Coordinates

Add the module to your application:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.envconfig</groupId>
    <artifactId>helidon-oci-envconfig</artifactId>
</dependency>
```

`helidon-oci-envconfig` uses the OCI SDK support from this repository, and that stack still expects
a JAX-RS client implementation at runtime. In the simple case, the single dependency above is
enough because this module already declares the runtime JAX-RS API and Jersey client dependencies
it needs.

Additional client dependencies may still be needed if your application:

* excludes transitive runtime dependencies
* assembles OCI SDK service clients directly
* wants to make the HTTP client stack explicit in its own POM

Jersey example:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.envconfig</groupId>
    <artifactId>helidon-oci-envconfig</artifactId>
</dependency>
<dependency>
    <groupId>javax.ws.rs</groupId>
    <artifactId>javax.ws.rs-api</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.glassfish.jersey.core</groupId>
    <artifactId>jersey-client</artifactId>
    <scope>runtime</scope>
</dependency>
```

If your application uses a different OCI SDK HTTP client/provider stack, include the matching
provider-specific dependencies instead of Jersey.

---

## Config Source

The `oci-env` source is exposed in two ways:

* For explicit Helidon bootstrap, enable it from `meta-config.*` with `type: "oci-env"`.
* For `Services.get(Config.class)` and other declarative bootstrap paths that use Helidon's
  default bootstrap, it is registered automatically from the service registry when
  `helidon-oci-envconfig` is on the classpath and no explicit `meta-config.*` was found.

If you build config directly with `Config.create()` or `Config.builder()`, add the `oci-env`
source explicitly through meta-config or by wiring the source yourself. `oci-config.yaml` by
itself does not activate `oci-env` for direct builder usage.

### Bootstrap Files and Precedence

`oci-env` interacts with two different bootstrap files:

* `meta-config.*`
  * This is Helidon bootstrap input, not an `oci-env`-specific file.
  * Helidon looks for `meta-config.*` in the current working directory first and then on the
    classpath. The suffix depends on the parsers on the classpath, for example
    `meta-config.yaml`.
  * When present, it controls whether `oci-env` is enabled at all.
* `oci-config.yaml`
  * This is `oci-env` convenience input used by both the automatic source path and the explicit
    provider path.
  * This fallback path is loaded through Helidon's default `Config.create(...)` source ordering, so
    environment variables and system properties are also consulted before the YAML files.
  * `oci-env` looks for a filesystem file named `oci-config.yaml` first and then for a classpath
    resource with the same name.
  * Only the `helidon.oci-env` subtree is read from this file.

Precedence:

1. Explicit `meta-config.*` has higher priority for `oci-env` bootstrap.
2. If `meta-config.*` contains a source entry with `type: "oci-env"`, `oci-env` reads its
   explicit configuration from that entry's `properties` block and then fills missing keys from
   `helidon.oci-env` in `oci-config.yaml`. Provider properties win per key.
3. If explicit `meta-config.*` exists but does not list `oci-env`, the source is not auto-added,
   so `oci-config.yaml` does not activate it.
4. When no explicit `meta-config.*` was found and `oci-env` is instantiated without explicit
   meta-config properties, such as the default `Services.get(Config.class)` bootstrap path, it
   reads `helidon.oci-env` through `Config.create(...)`.
5. In that fallback path, environment variables have the highest priority, then system properties,
   then filesystem `oci-config.yaml`, and finally classpath `oci-config.yaml`.
6. If none of those sources provides `helidon.oci-env`, the source still works with its defaults
   and resolves location from runtime files such as `/etc/region`. On the default service-registry
   bootstrap path, it can fall back to IMDS metadata when runtime files are not available.

The source stays lazy on both paths. It does not read `/etc/*` files, request IMDS metadata,
or import dynamic core-regions metadata until Helidon requests a key under the configured prefix.
Placeholder resolution in other config sources can trigger that lazy lookup during `Config.build()`.

Example placeholder usage:

```yaml
oci:
  workflow:
    endpoint-details:
      server-endpoint: "https://wfaas-overlay.${oci.env.ad-number}.${oci.env.iaas-domain-name}"
```

```java
Config config = Services.get(Config.class);
String realm = config.get("oci.env.realm").asString().orElseThrow();
String region = config.get("oci.env.region").asString().orElseThrow();
```

This lets services remove many per-region config files and derive endpoints from runtime location.

---

### Published Keys

The source publishes the following keys under the configured prefix. The examples below use the
default prefix `oci.env`. Optional values such as `fault-domain` and ODO variables are omitted when
they are not available.

#### Location and Realm

| Key | Description |
|-----|-------------|
| `oci.env.realm` | Realm name such as `oc1`. |
| `oci.env.region` | Public region identifier such as `us-ashburn-1`. |
| `oci.env.region-name` | `Region.getName()` value from `core-regions`, for example `iad` or `eu-frankfurt-1`. |
| `oci.env.region-internal-name` | Internal region name from `core-regions`. |
| `oci.env.availability-domain` | `AvailabilityDomain.getName()` value, for example `iad-ad-1` or `eu-frankfurt-1-ad-1`. |
| `oci.env.fault-domain` | Fault domain number, when present. |
| `oci.env.ad-number` | Availability-domain number name such as `ad1`. |
| `oci.env.number-for-ad` | Numeric availability-domain value such as `1`. |

#### Domain Names

| Key | Description |
|-----|-------------|
| `oci.env.public-domain-name` | Public domain for the current region. |
| `oci.env.realm-public-domain-name` | Public domain for the current realm without the region prefix. |
| `oci.env.oci-public-domain-name` | OCI public domain for the current region. |
| `oci.env.iaas-domain-name` | IaaS domain for the current region. |
| `oci.env.realm-iaas-domain-name` | IaaS domain for the current realm without the region prefix. |
| `oci.env.oci-iaas-domain-name` | OCI IaaS domain for the current region. |

#### Additional Derived Values

| Key | Description |
|-----|-------------|
| `oci.env.first-region-in-realm-public-name` | First bootstrapped public region in the current realm. |
| `oci.env.airport-code` | Region airport code such as `IAD`. |
| `oci.env.db-tns-name` | Database TNS-safe region token, for example `usashburn1`. |

#### ODO Metadata

| Key | Description |
|-----|-------------|
| `oci.env.odo-application-alias` | `ODO_APPLICATION_ALIAS` when present, otherwise `ODO_APPLICATION_ID`. |
| `oci.env.odo-application-resource-id` | `ODO_APPLICATION_RESOURCE_ID` when present. |
| `oci.env.odo-pool-alias` | `ODO_POOL_ALIAS` when present. |
| `oci.env.odo-pool-resource-id` | `ODO_POOL_RESOURCE_ID` when present. |
| `oci.env.odo-build-tag` | `ODO_BUILD_TAG` when present. |

## Dynamic Core Regions

Dynamic core-regions import is shared by both features exposed by this module:

* the `oci-env` config source, which uses the imported metadata when publishing derived
  `oci.env.*` values
* the companion region providers, which use the same imported metadata when resolving the active
  region and, when needed, registering an OCI SDK region from commons metadata

Import is triggered lazily the first time either path needs region metadata:

* when the config source first resolves an `oci.env.*` key
* when either declarative region provider first resolves the active region

In practice, that usually happens during service startup, but placeholder interpolation in other
config sources can trigger config-source resolution during `Config.build()`.

Default runtime paths:

* `/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata.json`
* `/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata_override.json`

Behavior:

* If `dynamic-core-regions.enabled` is `false`, the source skips dynamic import and validation
  entirely.
* `dynamic-core-regions.import-path` defaults to the standard RPM metadata file and import runs
  only when that file exists.
* `dynamic-core-regions.import-override-path` defaults to the standard RPM override metadata file.
* If the override file exists, the source calls
  `CoreRegionsRegistrarHelper.importDataFromJsonPathWithOverride(...)`.
* If the override file does not exist, the source falls back to
  `CoreRegionsRegistrarHelper.importDataFromJsonPath(...)`.
* If import occurs and `dynamic-core-regions.validate-import` is `true`, the source validates the
  import by resolving `dynamic-core-regions.validation-region`. The default validation region is
  `me-dcc-doha-1`.

The underlying `core-regions` import is additive only. Metadata updates for already imported
regions still require a service restart.

Because the import is lazy and shared, operations such as `Config.asMap()`, `Config.asNodeList()`,
and `Config.traverse()` may not reflect `oci-env` values unless those nodes were requested
directly first, and a region provider may be the first consumer that triggers the import.

---

## Region Providers

In addition to the config source, the module also contributes declarative service-registry region
providers backed by the same lazy environment resolution:

* A higher-priority `io.helidon.integrations.oci.spi.OciRegion` provider for public OCI SDK
  `com.oracle.bmc.Region` injection and lookup.
* A `com.oracle.pic.commons.util.Region` provider for code that needs the PIC commons/core region
  type directly.

Behavior:

* If no `oci-env` region is configured or detected from runtime files, the providers return no
  value. For SDK `Region` lookup, this allows lower-priority Helidon OCI SDK region providers,
  such as `helidon.oci.region`, authentication, or IMDS providers, to answer instead.
* If an `oci-env` region value is present but invalid, such as an invalid
  `location-override.region` or invalid `/etc/region` value, resolution fails fast instead of
  falling back to lower-priority providers.
* If the OCI SDK already knows the resolved region, the SDK provider returns it directly.
* If the region exists only in commons `core-regions`, the SDK provider lazily registers a
  matching SDK realm and SDK region from commons metadata and then returns the registered SDK
  region.
* If the commons realm does not expose a public or IaaS domain, the SDK provider still registers
  a synthetic SDK realm using a reserved invalid second-level domain so injected `Region` stays
  aligned with `oci-env` without targeting a real OCI realm.

This means `oci.env.*` config values, injected SDK `Region`, and injected PIC commons `Region`
stay aligned when `oci-env` resolves the location, even for DEV-style or other non-public commons
realms.

---

## Configuration

Choose one declarative configuration path:

* If you already manage Helidon bootstrap explicitly, enable `oci-env` from `meta-config.*` with
  a source entry of `type: "oci-env"`. Put the settings that must be explicit in that entry's
  `properties` block; missing keys can still come from `helidon.oci-env` in `oci-config.yaml`.
* If you are using the default `Services.get(Config.class)` bootstrap path with no explicit
  `meta-config.*`, put `oci-env` settings in `oci-config.yaml` under `helidon.oci-env`.

The keys are the same on both paths. The parent path differs:

* `oci-config.yaml` uses `helidon.oci-env`
* `meta-config.*` uses `sources[].properties`

When both inputs are present for an explicit `oci-env` source, `sources[].properties` overrides
`helidon.oci-env` from `oci-config.yaml` per key. Values from `oci-config.yaml` fill only keys
missing from provider properties.

Primary `oci-config.yaml` example:

```yaml
helidon:
  oci-env:
    prefix: "oci.env"
    use-physical-availability-domain: false
    location-override-dev: false
    location-override:
      region: "us-ashburn-1"
      availability-domain: "iad-ad-1"
      fault-domain: 5
    dynamic-core-regions:
      enabled: true
      import-path: "/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata.json"
      import-override-path: "/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata_override.json"
      validation-region: "me-dcc-doha-1"
      validate-import: true
```

Meta-config variant:

```yaml
sources:
  - type: "oci-env"
    properties:
      prefix: "oci.env"
      location-override:
        region: "us-ashburn-1"
```

Configuration keys:

| Key | Default Value | Description |
|-----|---------------|-------------|
| `prefix` | `oci.env` | Prefix used for published keys. |
| `location-override.region` | | Overrides the region lookup. |
| `location-override.availability-domain` | | Overrides the availability-domain lookup. |
| `location-override.fault-domain` | | Overrides the fault-domain lookup with an integer value. |
| `use-physical-availability-domain` | `false` | When `true`, reads `/etc/physical-availability-domain` instead of `/etc/availability-domain`. |
| `location-override-dev` | `false` | When `true`, overrides the location to `Region.DEV` / `AvailabilityDomain.DEV_1`. |
| `dynamic-core-regions.enabled` | `true` | When `false`, disables dynamic core-regions import and validation entirely. |
| `dynamic-core-regions.import-path` | `/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata.json` | Metadata import path. Import is skipped when this file does not exist. |
| `dynamic-core-regions.import-override-path` | `/etc/rbcp_core_regions_artifacts/rbcp_core_regions_metadata_override.json` | Override metadata path. When this file does not exist, import proceeds without it. |
| `dynamic-core-regions.validation-region` | `me-dcc-doha-1` | Region name used to validate imported metadata after import. |
| `dynamic-core-regions.validate-import` | `true` | When `true`, validates the imported metadata after import. |

### Location Resolution and Overrides

The location resolution follows the same design as `EnvironmentConfig`:

* `/etc/region` is treated as an internal region name and is resolved through
  `Region.optionalFromInternalName(...)`.
* `/etc/availability-domain` is treated as an AD number name such as `ad1` and is resolved through
  `AvailabilityDomain.fromRegionAndAdNumberName(...)`.
* `use-physical-availability-domain=true` switches availability-domain lookup to
  `/etc/physical-availability-domain`.
* `/etc/fault-domain` is treated as an integer value.
* If runtime location files are not available, the default service-registry config-source path
  falls back to IMDS instance metadata. It maps `canonicalRegionName` or `region` to the region,
  `ociAdName` to the availability domain, and `faultDomain` to the fault-domain number when present.
* IMDS fallback uses the standard `helidon.oci.imds-*` settings, including `imds-base-uri`,
  `imds-timeout`, and `imds-detect-retries`. This is useful for local SSH-tunnel profiles that
  point IMDS at `localhost`.

Override behavior:

* Region overrides accept `Region.getName()`, `Region.getPublicRegionName()`,
  `Region.getInternalName()`, and enum-style names such as `AF_JOHANNESBURG_1`.
* Availability-domain overrides accept `AvailabilityDomain.getName()` and enum-style names such as
  `AF_JOHANNESBURG_1_AD_1`.
* Fault-domain overrides accept integer values
* `location-override-dev=true` forces `Region.DEV` and `AvailabilityDomain.DEV_1` and takes
  precedence over the normal `location-override.*` settings.

---

## References

* [Reducing Configuration Files for Dropwizard Services](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/shepherd/shepherd-tips-and-tricks/reducing-configuration-files-for-dropwizard-services.htm?Highlight=EnvironmentTypeSafeReader)

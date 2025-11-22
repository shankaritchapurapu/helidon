# Environment Configuration

---

## Contents

* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)

---

## Overview

The environment-config module populates the Helidon Config with common OCI environment specific properties similar to the feature provided by the [EnvironmentTypeSafeReader](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/shepherd/shepherd-tips-and-tricks/reducing-configuration-files-for-dropwizard-services.htm?Highlight=EnvironmentTypeSafe#using-environmenttypesafereader). This will help in reducing configuration files and can eliminate per-region setup by automatically providing parts of the value of a config property.

---

## Maven Coordinates

To enable the environment-config feature, add the following dependency to your project’s pom.xml:

Helidon SE:
```xml
<dependency>
    <groupId>com.oracle.helidon.oci.common.envconfig</groupId>
    <artifactId>helidon-oci-common-env-config</artifactId>
    <scope>runtime</scope>
</dependency>
```
or 

Helidon MP:
```xml
<dependency>
    <groupId>com.oracle.helidon.oci.common.envconfig</groupId>
    <artifactId>helidon-oci-common-env-config-microprofile</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

When this module is added into a project, the following OCI environment specific properties will be made available from the Helidon Configuration.

 | Configuration Key                         | Description                                                                                                                                                                                                                              |
 |-------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
 | oci.env.realm                             | The realm name (e.g. `oc1`).                                                                                                                                                                                                             |
 | oci.env.region                            | The public region id (e.g. `us-ashburn-1`).                                                                                                                                                                                              |
 | oci.env.region-name                       | The public region name (e.g. `iad`).                                                                                                                                                                                                     |
 | oci.env.region-internal-name              | The internal region name (e.g. `us-ashburn-1`).                                                                                                                                                                                          |
 | oci.env.availability-domain               | The availability domain name (e.g. `iad-ad-1`)                                                                                                                                                                                           |
 | oci.env.fault-domain                      | The fault domain name (e.g. `3` for fault domain 3)                                                                                                                                                                                      |
 | oci.env.public-domain-name                | The public domain name (e.g. `us-ashburn-1.oraclecloud.com`).                                                                                                                                                                            |
 | oci.env.realm-public-domain-name          | the public domain name without the region prefix (e.g. `oraclecloud.com`).                                                                                                                                                               |
 | oci.env.oci-public-domain-name            | The oci public domain name (eg. `us-ashburn-1.oci.oraclecloud.com`).                                                                                                                                                                     |
 | oci.env.iaas-domain-name                  | The iaas (Infrastructure as a Service) domain name (eg. `us-ashburn-1.oracleiaas.com`).                                                                                                                                                  |
 | oci.env.realm-iaas-domain-name            | The iaas domain name without the region prefix (e.g. `oracleiaas.com`).                                                                                                                                                                  |
 | oci.env.oci-iaas-domain-name              | The oci iaas domain name (e.g. `us-ashburn-1.oci.oracleiaas.com`).                                                                                                                                                                       |
 | oci.env.first-region-in-realm-public-name | The public region identifier of the first region that was bootstrapped in a realm (e.g. `us-phoenix-1"`).                                                                                                                                |
 | oci.env.ad-dumber                         | The availability domain number (e.g. `ad1`).                                                                                                                                                                                             |
 | oci.env.number-for-ad                     | The number for the availability domain (e.g. `1`).                                                                                                                                                                                       |
 | oci.env.airport-code                      | The airport code (eg. `IAD`).                                                                                                                                                                                                            |
 | oci.env.db-tns-name                       | The database TNS name used to build jdbc urls. Tns names do not support dashes (e.g. `usashburn1`).                                                                                                                                      |
 | oci.env.odo-application-alias             | The content of ODO_APPLICATION_ID environment variable, or null if not available. This is only available on an on an ODO environment (e.g. `oci-helidon-reference-customplane-api-demo`).                                                |
 | oci.env.odo-application-resource-id       | The content of ODO_APPLICATION_RESOURCE_ID environment variable, or null if not available. This is only available when running on hostagent v2 on an ODO environment (e.g. `application:iad-ad-1:b1690fe2-fa10-4644-9d9d-2265f3c84f68`). |
 | oci.env.odo-pool-alias                    | The content of ODO_POOL_ALIAS environment variable, or null if not available. This is only available when running on hostagent v2 on an ODO environment (e.g. `oci-helidon-reference-customplane-api-demo`).                             |
 | oci.env.odo-pool-resource-id              | The content of ODO_POOL_RESOURCE_ID environment variable, or null if not available. This is only available when running on hostagent v2 on an ODO environment (e.g. `pool:iad-ad-1:8e8ae262-3c5e-4a6c-ba7b-c5c1f5aa906d`).               |
 | oci.env.odo-build-tag                     | The content of ODO_BUILD_TAG environment variable, or null if not available. This is only available when running on hostagent v2 on an ODO environment, e.g. (`1.2.1-61`).                                                               |

Below are examples of how the environment config properties can be used:

1. Eliminate per-region configuration files by deriving the value of a config property. This can be done by adding one or more of the environment-config properties as substring components of the value. For example, the following Helidon configuration property (`oci.workflow.server-endpoint`):
    ```
    oci:
      workflow:
         server-endpoint: https://wfaas-overlay.${oci.env.ad-number}.${oci.env.iaas-domain-name}
    ```
   when deployed in `us-ashburn-1` region will have a final value of `https://wfaas-overlay.ad1.us-ashburn-1.oracleiaas.com` with `${oci.env.ad-number}` substituted with a value of `ad1` and `${oci.env.iaas-domain-name}` with `us-ashburn-1.oracleiaas.com`.
2. In Helidon MP using CDI, the environment config properties can also be directly retrieve. For example, `realm` and `region` values can be injected via `@ConfigProperty` annotation.
   ```java
    @Inject
    public ReferenceServiceResource(@ConfigProperty(name = "oci.env.realm") String realm,
                                    @ConfigProperty(name = "oci.env.region") String region) {
    
        LOG.info("oci.env.realm: " + realm );
        LOG.info("oci.env.region: " + region);
    }
    ```
3. The environment config properties can also be retrieved via the Service registry in either Helidon MP or Helidon SE:
    ```java
    Config config = Services.get(Config.class);
    LOG.info("oci.env.realm: " + config.get(PREFIX + "oci.env.realm" ).asString().get());
    LOG.info("oci.env.region: " + config.get(PREFIX + "oci.env.region" ).asString().get());
    ```
---

## Configuration

To enable the Environment Config feature, this module uses `Meta Configuration` with type `oci-env` for both [Helidon MP](https://helidon.io/docs/v4/mp/config/advanced-configuration#_creating_microprofile_config_sources_from_meta_config) and [Helidon SE](https://helidon.io/docs/latest/apidocs/io.helidon.config/io/helidon/config/MetaConfig.html). The Meta Configuration has the following config properties:

| Configuration key                     | Default Value | Description                                                                                                             |
|---------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------|
| prefix                                | oci.env       | The prefix for the Environment Config.                                                                                  |
| location-override.region              |               | Overrides the region from the environment. If not set, will use the value from `/etc/region`.                           |
| location-override.availability-domain |               | Overrides the availability domain from the environment. If not set, will use the value from `/etc/availability-domain`. |
| location-override.fault-domain        |               | Overrides the fault domain from the environment. If not set, will use the value from `/etc/fault-domain`.               |

Below are examples of how the Meta Configuration can be customized if needd:
1. Helidon MP via `mp-meta-config.yaml`:
   ```yaml
   sources:
     - type: "oci-env"
       prefix: custom.prefix
       location-override:
         region: sol-mars-1
         availability-domain: sol-mars-1-ad-1
         fault-domain: 5
   ```
2. Helidon SE via `meta-config.yaml`:
   ```yaml
   sources:
     - type: "oci-env"
       properties:
       prefix: custom.prefix
       location-override:
         region: sol-mars-1
         availability-domain: sol-mars-1-ad-1
         fault-domain: 5
   ```

## References

* [Reducing Configuration Files for Dropwizard Services](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/shepherd/shepherd-tips-and-tricks/reducing-configuration-files-for-dropwizard-services.htm?Highlight=EnvironmentTypeSafeReader)

# Splat

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

---

## Overview

The Splat module provides support for OCI SplatMtlsFilter integration, which is required for setting up mTLS between Splat and a Helidon application. The SplatMtlsFilter is used to validate whether authorization has been performed by Splat.

---

## Maven Coordinates

To enable SplatMtlsFilter, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-splat</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

The SplatMtlsFilter will be registered on the WebServer using [SplatMtlsFeature](../splat/src/main/java/com/oracle/helidon/oci/splat/SplatMtlsFeature.java), which is a [Helidon Server Feature](https://helidon.io/docs/v4/se/webserver/webserver#_server_features). When the [Service Registry](https://helidon.io/docs/v4/se/injection/injection#generate-binding) is started on an application as shown below:
```java
ServiceRegistryManager.start(ApplicationBinding.create());
```
the SplatMtlsFeature will be configured into the WebServer automatically. Otherwise, you need to configure it explicitly in code by registering it with the WebServer:
```java
WebServer.builder()
         .addFeature(Services.get(SplatMtlsFeature.class))
         .build();
```

---

## Configuration

Configure SplatMtlsFilter behavior using the application.yaml file.


| Config Key                            | Default Value                  | Description                                                                                                              | Notes                                                                                                                                                                                                                  |
|---------------------------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| oci.splat.enabled                     | true                           | Whether to enable the SplatMtlsFilter.                                                                                   |                                                                                                                                                                                                                        |
| oci.splat.skip-authz-validation-check | false                          | Whether to bypass AuthZ validation from SplatMtlsFilter.                                                                 | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.reject-x-region-calls       | false                          | Whether SplatMtlsFilter will reject cross-region client certificates.                                                    | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.region                      | none                           | The region name. If not specified, the value will be automatically retrieved from the Oci environment.                   |                                                                                                                                                                                                                        |

It is important to note that mTLS must be set up on the Helidon WebServer.

---

## References

* [Splat User Guide](https://confluence.oci.oraclecorp.com/display/PLAT/Splat+-+User+Guide)
* [Splat Concepts](https://confluence.oci.oraclecorp.com/display/PLAT/1.+Splat+Concepts)
* [Splat Onboarding](https://confluence.oci.oraclecorp.com/display/PLAT/2.+Splat+Onboarding)
* [Splat Features](https://confluence.oci.oraclecorp.com/display/PLAT/3.+Splat+Features)

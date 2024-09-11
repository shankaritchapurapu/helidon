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

The Splat module provides support for OCI SplatMtlsFilter integration as a requirement for setting up mTLS between Splat and the Helidon application. SplatMtlsFilter is a JaxRs ContainerRequestFilter written by the Splat team and is used to validate whether authorization has been performed at Splat.

---

## Maven Coordinates

To enable SplatMtlsFiler add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-splat</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

Once the module is added as a dependency in the application's pom.xml, the SplatMtlsFilter will be automatically triggered for every https request. Non-https request on the other hand will be skipped, i.e. the filter will not validate them.

---

## Configuration

Configure SplatMtlsFilter using the Helidon microprofile configuration framework by which the ConfigSource defaults to
`microprofile-config.properties`. Alternatively, you can also use other ConfigSources such as `application.yaml`.

| config key                                               | Default Value                  | Description                                                                                                              | See                                                                                                                                                                                                                    |
|----------------------------------------------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| oci.splat.mtls-filter-config.enabled                     | true                           | Flag indicating whether to enable the SplatMtlsFilter.                                                                   |                                                                                                                                                                                                                        |
| oci.splat.mtls-filter-config.skip-authz-validation-check | false                          | Flag indicating whether to bypass AuthZ validation from SplatMtlsFilter.                                                 | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.splat.mtls-filter-config.reject-x-region-calls       | false                          | Flag indicating whether SplatMtlsFilter will reject cross region client certificates.                                    | [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS) |
| oci.instance-metadata-uri                                | http://169.254.169.254/opc/v2/ | The Instance Metadata Service uri. This can be used to override the default value, such as testing using SSH tunnelling. |                                                                                                                                                                                                                        |
| oci.region                                               | none                           | The region name. If not specified, the value will be automatically retrieved from the Instance Metadata Service.         |                                                                                                                                                                                                                        |

Additionally, mTLS must be set up on the Helidon application. Below is an example configuration:

```properties
# Client CA Trust bundle
server.tls.client-auth=REQUIRE
server.tls.trust.pem.certificates.resource.resource-path=ca-bundle.pem

# Private key and server certificate chain
server.tls.private-key.pem.key.resource.resource-path=cert86/key.pem
server.tls.private-key.pem.cert-chain.resource.resource-path=cert86/chain.pem
```

---

## References

* [Splat User Guide](https://confluence.oci.oraclecorp.com/display/PLAT/Splat+-+User+Guide)
* [Splat Concepts](https://confluence.oci.oraclecorp.com/display/PLAT/1.+Splat+Concepts)
* [Splat Onboarding](https://confluence.oci.oraclecorp.com/display/PLAT/2.+Splat+Onboarding)
* [Splat Features](https://confluence.oci.oraclecorp.com/display/PLAT/3.+Splat+Features)

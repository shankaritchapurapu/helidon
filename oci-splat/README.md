# Splat

## Overview
This module provides support for OCI SplatMtlsFilter integration as a requirement for setting up mTLS between Splat and the Helidon application.

## Configuration
Start by including a dependency to this module in your pom file as shown below. All relevant
providers will be automatically loaded into your application.

```
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-splat</artifactId>
    <version>...</version>
</depenency>
```

Next, optionally configure the provider. This typically is placed in your <i>microprofile-config.properties</i>.

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


## Usage
The SplatMtlsFilter will be automatically triggered for every https request. Non-https request on the other hand will be skipped, i.e. the filter will not validate them.


## Build and run example
### Build & Run

```shell
mvn clean install
```

## References
* [3. Introduce a Splat-only port protected by mTLS](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=PLAT&title=2.+Splat+Onboarding#id-2.SplatOnboarding-3.IntroduceaSplat-onlyportprotectedbymTLS)

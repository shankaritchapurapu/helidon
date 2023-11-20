# Welcome to Helidon!

## OCI Native Library Integrations

## Overview
These libraries are additions to what is found in our public [gitHub repository](https://github.com/helidon-io/helidon).
If you are building a service "built on public OCI SDK and public-facing services" then you can use
our [public quickstart for building OCI applications](https://helidon.io/starter/2.6.3?step=2&flavor=mp) instead of this repo.

If, however, you are part of the customer or service enclave, and you need to integrate to the native/private
libraries that OCI teams produce (that are not part of the public OCI SDK), then you are at the right place here.

## Prerequisites
- JDK 17 or higher (not that while Helidon 2.x only requires Java 11 or higher, the OCI SDK requires Java 17 or higher)
- Maven 3.6.1 or higher
- Helidon 2.6.3 or higher

## Usage
Most users should be generating their service using the [Helidon Service Generator](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-service-generator).
This repo provides the supporting libraries for services generated from that generator.

## Build & Run

```shell
mvn clean package
```

## Links
* [Helidon Service Generator](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-service-generator)
* Oracle Slack Channel: #helidon-users
* Public Site: https://helidon.io/
* Oracle Jira Issues (used for any/all Oracle Internal issue tracking): https://jira.oci.oraclecorp.com/projects/HLDN/summary

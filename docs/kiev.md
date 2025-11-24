# Kiev Client Library

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [References](#references)

---

## Overview

[Kiev](https://confluence.oraclecorp.com/confluence/display/KIEV/Kiev+Home) is a persistent transactional key-value data store that implements serialized snapshot isolation, is scalable by sharding, and provides several features for recovery-oriented computing. Kiev can be configured as in-memory datastore, direct Oracle Database connection datastore or KaaS (Kiev as a Service). 

[Kiev as a Service - KaaS](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/kiev/landing-kiev.htm) is a fully managed data platform service used primarily by Control Plane services at OCI. KaaS uses a REST API, however, its recommend that you use the Kiev client library for the best performance and compatibility.

---

## Maven Coordinates

To be able to integrate with Kiev client library, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-repackaged-oci-kiev-library</artifactId>
</dependency>
```

This module packages all the dependencies required to make the Kiev client library work without having to define them individually in your project. Moreover, the generated java client is adjusted so that the library will be compatible with OCI Java SDK 3 which is the version used by the oci-helidon project.

To use the in-memory Kiev, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-repackaged-oci-kiev-in-memory-library</artifactId>
</dependency>
```

**&#9432;** Kiev streaming client is currently not included in the repackaged library.

---

## Usage

Once the module is added as a dependency in the application's pom.xml, you can conveniently store, retrieve, index, and decorate objects stored by Kiev. Out [integration tests](../tests/integration/kiev/README.md) showcases, how to use the various Kiev configuration and APIs.

---

## References

* [Kiev User Guide](https://confluence.oraclecorp.com/confluence/display/KIEV/Kiev+User+Documentation)
* [Getting Started with KaaS](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/kiev/landing-kiev.htm)
* [KaaS User Guide](https://confluence.oraclecorp.com/confluence/display/KIEV/KaaS+User+Documentation)

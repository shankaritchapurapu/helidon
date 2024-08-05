# Request ID

## Overview

This module provides support for handling of the opc-request-id header as required in OCI.

## Configuration

Just include a dependency to this module in your pom file as shown below. All relevant
providers will be automatically loaded into your application.

Helidon SE:

```
<dependency>
    <groupId>com.oracle.helidon.oci.common.requestid</groupId>
    <artifactId>helidon-oci-common-request-id-webserver</artifactId>
    <version>...</version>
</depenency>
```

Helidon MP:

```
<dependency>
    <groupId>com.oracle.helidon.oci.common.requestid</groupId>
    <artifactId>helidon-oci-common-request-id-microprofile</artifactId>
    <version>...</version>
</depenency>
```

## Usage

## Build and run example

### Build & Run

```shell
mvn clean package
```
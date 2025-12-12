# request-id

---

## Contents

* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Example](#example)
* [Reference](#reference)

---

## Overview

This module provides support for handling of the `opc-request-id` header as required in OCI to serve to uniquely identify API requests exchanges and help associate exchanges. This ability to identify individual exchanges and search for relevant data related to a specific request-id would greatly aid in diagnosing operational issues.



---

## Maven Coordinates

To enable request-id support add the following dependency to your Helidon SE project’s pom.xml:

Helidon SE:
```
<dependency>
    <groupId>com.oracle.helidon.oci.common.requestid</groupId>
    <artifactId>helidon-oci-common-request-id-webserver</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

Just include a dependency to this module in your pom file and all relevant providers will be automatically loaded into your application.

---

## Example

Below demonstrates an example of a request to a service that returns a response with an `opc-request-id` header with its generated value included:

```shell
$ curl -v -X PUT -d '{"pluginId" : "pluginId", "compartmentId": "ocid1.compartment.oc1..dummycompartment", "hostName" : "hostname"}' -H "Content-Type: application/json"  http://localhost:8080/20231121/managedInstances/ocid1.instance.oc1.iad.dummyinstance/status
* Host localhost:8080 was resolved.
* Connected to localhost (::1) port 8080
> PUT /20231121/managedInstances/ocid1.instance.oc1.iad.dummyinstance/status HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.6.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 154
> 
< HTTP/1.1 204 No Content
< Date: Wed, 31 Jul 2024 17:45:23 -0700
< Connection: keep-alive
< Content-Length: 0
< opc-request-id: /D9C34373C47EC15A5F85740A14898B98/D7D339B1429A3E27AA189FA6E8262321

```

---

## Reference

* [Request IDs](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEX&title=Request+IDs)

# Kiev

---

## Contents

* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

---

## Overview

The Kiev integration provides Helidon-friendly configuration and service registry bindings for the Kiev client libraries.
It supports three backends:

1. `IN_MEMORY` for local development and unit tests
2. `DIRECT_DB` for a Kiev store reached through a direct Oracle database connection
3. `SERVICE` for Kiev as a service

When this module is on the classpath, Helidon can create and inject:

* `com.oracle.pic.kiev.DataStore`
* `com.oracle.pic.kiev.mapping.MappedDataStore`
* `com.oracle.helidon.oci.kiev.KievTransactionSupport`

The module also provides the `@KievTransaction` annotation for declarative transaction handling on service methods.

---

## Maven Coordinates

Add the Kiev integration dependency to your application:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.kiev</groupId>
    <artifactId>helidon-oci-kiev</artifactId>
</dependency>
```

---

## Usage

### Configure a backend

At minimum, configure the backend, store name, and application name:

```yaml
oci:
  kiev:
    backend: "IN_MEMORY"
    store-name: helidon-store-example
    app-name: helidon-store-example
```

For a direct database connection:

```yaml
oci:
  kiev:
    backend: "DIRECT_DB"
    store-name: pdbdev
    app-name: KievTest
    direct-db:
      jdbc-url: jdbc:oracle:thin:@//localhost:1521/pdbdev
      user-name: helidon
      password: changeit
      schema-name: helidon
```

For Kiev as a service with instance auth:

```yaml
oci:
  kiev:
    backend: "SERVICE"
    store-name: your-store
    app-name: your-app
    service:
      compartment-id: ocid1.compartment.oc1...
      frontend-endpoint: https://your-kiev-endpoint
      locality: "REGIONAL"
      auth:
        type: "INSTANCE"
        root-cert-pem-path: /etc/oci-pki/ca-bundle.pem
```

For Kiev as a service with S2S auth:

```yaml
oci:
  kiev:
    backend: "SERVICE"
    store-name: your-store
    app-name: your-app
    service:
      compartment-id: ocid1.compartment.oc1...
      frontend-endpoint: https://your-kiev-endpoint
      locality: "AD1"
      auth:
        type: "S2S"
        auth-endpoint: https://auth.example
        root-cert-pem-path: /etc/oci-pki/ca-bundle.pem
        leaf-cert-path: /path/to/leaf.pem
        leaf-cert-key-path: /path/to/leaf.key
        intermediate-cert-path: /path/to/intermediate.pem
        tenant-id: ocid1.tenancy.oc1...
        key-passphrase: secret
        cert-reload-duration: PT15M
        cert-ssl-algorithm: SunX509
```

For local KIAB KaaS testing:

```yaml
oci:
  kiev:
    backend: "SERVICE"
    store-name: kaaspdb
    app-name: KievTest
    service:
      compartment-id: ocid1.compartment.dev...
      frontend-endpoint: http://localhost:16666
      locality: "REGIONAL"
      auth:
        type: "KIAB_LOCAL"
```

### Inject the Kiev services

You can inject `MappedDataStore` and create or open mapped buckets in your service layer:

```java
@Service.Singleton
class StoreService {
    private final MappedHashBucket<String, StoreItem> bucket;

    @Service.Inject
    StoreService(MappedDataStore mappedDataStore) {
        this.bucket = mappedDataStore.getOrCreateBucket("store_example_items",
                                                        "Helidon OCI Kiev store example bucket",
                                                        String.class,
                                                        StoreItem.class);
    }
}
```

### Use declarative transactions

Annotate service methods with `@KievTransaction`. If the method declares a
`com.oracle.pic.kiev.Transaction` parameter, the active transaction is supplied automatically.

```java
@KievTransaction("store-put")
String put(Transaction tx, String id, String value) {
    StoreItem item = bucket.put(tx, new StoreItem(id, value));
    return item.value;
}

@KievTransaction(value = "store-get", readOnly = true)
Optional<String> get(Transaction tx, String id) {
    return bucket.get(tx, id).map(item -> item.value);
}
```

### Access the current transaction from context

If a request `io.helidon.common.context.Context` is available, the active transaction can also be
retrieved through `KievTransactions`:

```java
Transaction tx = KievTransactions.require(context);
```

---

## Configuration

### Root configuration

| Key                             | Default value | Description |
|---------------------------------|---------------|-------------|
| `oci.kiev.backend`              | `IN_MEMORY`   | Kiev backend to use: `IN_MEMORY`, `DIRECT_DB`, or `SERVICE`. |
| `oci.kiev.store-name`           |               | Kiev store name. |
| `oci.kiev.app-name`             |               | Application name passed to the Kiev client. |
| `oci.kiev.transaction-max-reads`  | `100`       | Maximum reads per transaction. |
| `oci.kiev.transaction-max-writes` | `100`       | Maximum writes per transaction. |

### Direct DB configuration

Required when `oci.kiev.backend=DIRECT_DB`.

| Key                            | Default value | Description |
|--------------------------------|---------------|-------------|
| `oci.kiev.direct-db.jdbc-url`  |               | Oracle JDBC URL. |
| `oci.kiev.direct-db.user-name` |               | Database user name. |
| `oci.kiev.direct-db.password`  |               | Database password. |
| `oci.kiev.direct-db.schema-name` |             | Optional schema name. If omitted, the Kiev client default is used. |

### Service configuration

Required when `oci.kiev.backend=SERVICE`.

| Key                                  | Default value | Description |
|--------------------------------------|---------------|-------------|
| `oci.kiev.service.compartment-id`    |               | Compartment containing the Kiev store. |
| `oci.kiev.service.frontend-endpoint` |               | Kiev frontend endpoint. |
| `oci.kiev.service.locality`          | `REGIONAL`    | Store locality such as `REGIONAL`, `AD1`, `AD2`, or `AD3`. |
| `oci.kiev.service.auth.type`         | `INSTANCE`    | Auth type: `INSTANCE`, `S2S`, or `KIAB_LOCAL`. |

### Service auth configuration

`INSTANCE` auth requires:

| Key                                        | Default value | Description |
|--------------------------------------------|---------------|-------------|
| `oci.kiev.service.auth.root-cert-pem-path` |               | Root certificate PEM path. |
| `oci.kiev.service.auth.auth-endpoint`      |               | Optional auth endpoint override. |
| `oci.kiev.service.auth.cert-reload-duration` |             | Optional certificate reload interval. |
| `oci.kiev.service.auth.cert-ssl-algorithm` |               | Optional SSL algorithm override. |

`S2S` auth requires:

| Key                                            | Default value | Description |
|------------------------------------------------|---------------|-------------|
| `oci.kiev.service.auth.auth-endpoint`          |               | Identity auth endpoint. |
| `oci.kiev.service.auth.root-cert-pem-path`     |               | Root certificate PEM path. |
| `oci.kiev.service.auth.leaf-cert-path`         |               | Leaf certificate path. |
| `oci.kiev.service.auth.leaf-cert-key-path`     |               | Leaf private key path. |
| `oci.kiev.service.auth.intermediate-cert-path` |               | Intermediate certificate path. |
| `oci.kiev.service.auth.tenant-id`              |               | Tenant OCID. |
| `oci.kiev.service.auth.key-passphrase`         |               | Optional private key passphrase. |
| `oci.kiev.service.auth.cert-reload-duration`   |               | Optional certificate reload interval. |
| `oci.kiev.service.auth.cert-ssl-algorithm`     |               | Optional SSL algorithm override. |

`KIAB_LOCAL` auth is intended for local KIAB KaaS testing and does not require extra auth properties.

---

## References

* [Store example](../examples/store/README.md)
* [Kiev library configuration in the store example](../examples/store/src/main/resources/application.yaml)

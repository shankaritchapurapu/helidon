# Kiev

---

## Overview

The Kiev integration registers configured Kiev data stores with the Helidon service registry and provides
declarative transaction support for Helidon Talon applications that use Kiev.

The configuration root is `oci.kiev`. Each configured data store uses one of three backends:

1. `IN_MEMORY` for local development and unit tests
2. `DIRECT_DB` for a Kiev store reached through a direct Oracle database connection
3. `SERVICE` for Kiev as a service

When this module is on the classpath, Helidon creates one Kiev data store per `oci.kiev.data-stores` entry and
registers named access to:

* `com.oracle.pic.kiev.DataStore`
* `com.oracle.pic.kiev.mapping.MappedDataStore`
* `com.oracle.helidon.oci.kiev.KievTransactionSupport`
* `com.oracle.pic.kiev.streams.service.client.core.Stream` for `SERVICE` stores

Applications can inject the named services directly or use the `@Kiev.Transaction` annotation for declarative
transaction handling on service methods.

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
    data-stores:
      - backend: "IN_MEMORY"
        store-name: helidon-store-example
        app-name: helidon-store-example
```

For a direct database connection:

```yaml
oci:
  kiev:
    data-stores:
      - backend: "DIRECT_DB"
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
    data-stores:
      - backend: "SERVICE"
        store-name: your-store
        app-name: your-app
        service:
          compartment-id: ocid1.compartment.oc1...
          frontend-endpoint: https://your-kiev-endpoint
          locality: "REGIONAL"
          auth:
            type: "INSTANCE"
            tls:
              root-cert-pem-path: /etc/oci-pki/ca-bundle.pem
        stream-deleted-column-values: false
```

For Kiev as a service with S2S auth:

```yaml
oci:
  kiev:
    data-stores:
      - backend: "SERVICE"
        store-name: your-store
        app-name: your-app
        service:
          compartment-id: ocid1.compartment.oc1...
          frontend-endpoint: https://your-kiev-endpoint
          locality: "AD1"
          auth:
            type: "S2S"
            tls:
              root-cert-pem-path: /etc/oci-pki/ca-bundle.pem
              cert-reload-duration: PT15M
              cert-ssl-algorithm: SunX509
            service-principal:
              federation-endpoint: https://auth.example
              tenant-id: ocid1.tenancy.oc1...
              certificates:
                - certificate: /path/to/leaf.pem
                  private-key: /path/to/leaf.key
                  passphrase: secret
                - certificate: /path/to/intermediate.pem
```

For Kiev as a service with a shared OCI SDK auth provider and Kiev-managed TLS:

`dynamic-ssl-context-provider-name` resolves a named `DynamicSslContextProviderConfig` service. The service can be
created from `oci.dynamic-ssl-context-providers` or supplied by the application.

```yaml
helidon:
  oci:
    authentication-method: instance-principal

oci:
  dynamic-ssl-context-providers:
    - name: kiev-service-auth
      root-cert-pem-path: /etc/oci-pki/ca-bundle.pem
      cert-reload-duration: PT5M
      cert-ssl-algorithm: SunX509

  kiev:
    data-stores:
      - backend: "SERVICE"
        store-name: your-store
        app-name: your-app
        service:
          compartment-id: ocid1.compartment.oc1...
          frontend-endpoint: https://your-kiev-endpoint
          locality: "REGIONAL"
          auth:
            type: "OVERRIDDEN"
            tls:
              dynamic-ssl-context-provider-name: kiev-service-auth
```

For local KIAB KaaS testing:

```yaml
oci:
  kiev:
    data-stores:
      - backend: "SERVICE"
        store-name: kaaspdb
        app-name: KievTest
        service:
          compartment-id: ocid1.compartment.dev...
          frontend-endpoint: http://localhost:16666
          locality: "REGIONAL"
          auth:
            type: "KIAB_LOCAL"
```

To configure more than one data store, add more `data-stores` entries. Store access is always by
`store-name`:

```yaml
oci:
  kiev:
    data-stores:
      - backend: "IN_MEMORY"
        store-name: primary-store
        app-name: primary-app
      - backend: "DIRECT_DB"
        store-name: reporting-store
        app-name: reporting-app
        direct-db:
          jdbc-url: jdbc:oracle:thin:@//localhost:1521/reporting
          user-name: helidon
          password: changeit
```

`DataStore`, `MappedDataStore`, `KievTransactionSupport`, and `Stream` injection points must use
`@Service.Named`. Unqualified injection fails and reports the registered store names so the
injection point can be qualified with one of the configured `data-stores[].store-name` values.
`Stream` is available only for stores using the `SERVICE` backend.

### Inject the Kiev services

Inject a named `MappedDataStore` and create or open mapped buckets in your service layer:

```java
@Service.Singleton
class StoreService {
    private static final String DATA_STORE_NAME = "helidon-store-example";

    private final MappedHashBucket<String, StoreItem> bucket;

    @Service.Inject
    StoreService(@Service.Named(DATA_STORE_NAME) MappedDataStore mappedDataStore) {
        this.bucket = mappedDataStore.getOrCreateBucket("store_example_items",
                                                        "Helidon Talon Kiev store example bucket",
                                                        String.class,
                                                        StoreItem.class);
    }
}
```

For another store, qualify the injection point with its `store-name`:

```java
@Service.Singleton
class ReportingService {
    private final MappedDataStore reportingStore;

    @Service.Inject
    ReportingService(@Service.Named("reporting-store") MappedDataStore reportingStore) {
        this.reportingStore = reportingStore;
    }
}
```

### Inject a Streaming Client

Inject `com.oracle.pic.kiev.streams.service.client.core.Stream` to read Kiev service stream records for a named
`SERVICE` store. The streaming client reuses the store's `service` auth, endpoint, compartment, TLS, and locality
configuration.

```java
import com.oracle.pic.kiev.streams.service.client.core.Stream;
import com.oracle.pic.kiev.streams.service.client.core.StreamResult;

@Service.Singleton
class StoreStreamReader {
    private final Stream stream;

    @Service.Inject
    StoreStreamReader(@Service.Named("your-store") Stream stream) {
        this.stream = stream;
    }

    StreamResult next(String cursor) {
        return stream.getRecords(cursor, 100);
    }
}
```

Set `stream-deleted-column-values` to `true` when delete records should include the deleted column values.

### Poll Stream Records for Key Updates

`Stream` is a cursor-based polling API. To be notified when a key changes, keep the last processed cursor, poll
`getRecords`, inspect each transaction record, and persist the returned `nextCursor` after successful processing.
Use `getNewestCursor()` when only future updates matter, or a previously persisted cursor when resuming a consumer.

```java
import com.oracle.pic.kiev.streams.service.client.core.Record;
import com.oracle.pic.kiev.streams.service.client.core.Stream;
import com.oracle.pic.kiev.streams.service.client.core.StreamResult;
import com.oracle.pic.kiev.streams.service.client.core.TxCommitContent;
import com.oracle.pic.kiev.streams.service.client.core.TxCommitRecord;
import java.util.List;

@Service.Singleton
class StoreKeyChangePoller {
    private final Stream stream;

    @Service.Inject
    StoreKeyChangePoller(@Service.Named("your-store") Stream stream) {
        this.stream = stream;
    }

    String poll(String cursor, String watchedBucket, String watchedKey) {
        StreamResult page = stream.getRecords(cursor, 100, List.of(watchedBucket));

        for (Record record : page.getRecords()) {
            if (record instanceof TxCommitRecord txRecord) {
                notifyMatchingUpdates(txRecord, watchedBucket, watchedKey);
            }
        }

        return page.getNextCursor();
    }

    private void notifyMatchingUpdates(TxCommitRecord txRecord, String watchedBucket, String watchedKey) {
        for (TxCommitContent content : txRecord.getContents()) {
            if (watchedBucket.equals(content.getBucketName())
                    && watchedKey.equals(content.getKeys().getValueMap().get("id"))) {
                // React to UPSERT or DELETE: invalidate a cache, enqueue work, or publish a notification.
            }
        }
    }
}
```

Stream pages can be empty and still include a `nextCursor`; keep advancing the cursor. Use
`StreamResult.getSecondsFromTip()` as a consumer-lag signal.

### Access a Transaction

Application code obtains a `com.oracle.pic.kiev.Transaction` from a transaction boundary. Use either a
`Transaction` parameter on an annotated service method, or the callback parameter passed by
`KievTransactionSupport.execute`. Pass that `Transaction` to Kiev bucket operations and to any helper methods
that need to participate in the same transaction.

```java
@Kiev.Transaction(value = DATA_STORE_NAME, name = "store-put")
String put(Transaction tx, String id, String value) {
    return writeItem(tx, id, value);
}
```

```java
String value = transactionSupport.execute("store-get", true, tx -> readItem(tx, id));
```

### Use declarative transactions

Annotate service methods with `@Kiev.Transaction`. If the method declares a
`com.oracle.pic.kiev.Transaction` parameter, the active transaction is supplied automatically.
Each transaction annotation value must use the configured `store-name`. Use `name` to set the transaction base name
used for diagnostics; otherwise Helidon generates one from the intercepted method.

Helidon appends a `-<System.nanoTime()>` suffix to the base name before opening the Kiev transaction. Generated
transaction base names use the form `kt-<method-name>-<hash>`, where non-alphanumeric characters are replaced with
`-`, long readable portions are truncated, and the hash is derived from the fully qualified method signature so
overloaded methods still get distinct names. Transaction base names are capped at 58 characters to leave room for the
runtime suffix and keep the final name below Kiev's 80-character transaction-name limit. Explicit `name` values longer
than 58 characters are rejected during code generation, and programmatic `KievTransactionSupport.execute` calls reject
longer base names before opening a transaction.

```java
@Kiev.Transaction(value = DATA_STORE_NAME, name = "store-put")
String put(Transaction tx, String id, String value) {
    StoreItem item = bucket.put(tx, new StoreItem(id, value));
    return item.value;
}

@Kiev.Transaction(value = DATA_STORE_NAME, name = "store-get", readOnly = true)
Optional<String> get(Transaction tx, String id) {
    return bucket.get(tx, id).map(item -> item.value);
}
```

For programmatic transaction handling, inject the named `KievTransactionSupport`:

```java
ReportingService(@Service.Named("reporting-store") KievTransactionSupport transactionSupport) {
    this.transactionSupport = transactionSupport;
}

String value = transactionSupport.execute("reporting-read", true, tx -> bucket.get(tx, id).orElseThrow().value());
```

---

## Configuration

The enum values shown in these examples use the Java enum constant names, such as `IN_MEMORY`, `DIRECT_DB`,
`REGIONAL`, and `KIAB_LOCAL`. Helidon 4 also accepts case-insensitive enum values and treats hyphens as underscores,
so `in-memory` maps to the same backend as `IN_MEMORY`.

### Root configuration

| Key                             | Default value | Description |
|---------------------------------|---------------|-------------|
| `oci.kiev.data-stores`          |               | List of Kiev data store configurations. |

At least one `data-stores` entry is required. All store access uses the configured `store-name`.

### Data store configuration

Each `oci.kiev.data-stores` entry defines one Kiev data store.

| Key                                      | Default value | Description |
|------------------------------------------|---------------|-------------|
| `oci.kiev.data-stores[].backend`         | `IN_MEMORY`   | Kiev backend to use: `IN_MEMORY`, `DIRECT_DB`, or `SERVICE`. |
| `oci.kiev.data-stores[].store-name`      |               | Kiev store name used with `@Service.Named` and `@Kiev.Transaction`. For `SERVICE` backends, this is the KaaS `kiev_name`/data store name and must follow KaaS limits, including the 30-character maximum. |
| `oci.kiev.data-stores[].app-name`        |               | Application name passed to the Kiev client. |
| `oci.kiev.data-stores[].transaction-max-reads` | `100` | Maximum reads per transaction. |
| `oci.kiev.data-stores[].transaction-max-writes` | `100` | Maximum writes per transaction. |
| `oci.kiev.data-stores[].stream-deleted-column-values` | `false` | Whether delete stream records should include deleted column values for `SERVICE` stores. |

### Direct DB configuration

Required when `backend=DIRECT_DB`.

| Key                            | Default value | Description |
|--------------------------------|---------------|-------------|
| `oci.kiev.data-stores[].direct-db.jdbc-url`  |               | Oracle JDBC URL. |
| `oci.kiev.data-stores[].direct-db.user-name` |               | Database user name. |
| `oci.kiev.data-stores[].direct-db.password`  |               | Database password. |
| `oci.kiev.data-stores[].direct-db.schema-name` |             | Optional schema name. If omitted, the Kiev client default is used. |

### Service configuration

Required when `backend=SERVICE`.
When `service.locality` is omitted, Kiev maps `oci.env.ad-number` values `ad1`, `ad2`, and `ad3` to the matching
locality. Unknown `oci.env.ad-number` values are logged as warnings and fall back to `REGIONAL`.

| Key                                  | Default value | Description |
|--------------------------------------|---------------|-------------|
| `oci.kiev.data-stores[].service.compartment-id`    |               | Compartment containing the Kiev store. |
| `oci.kiev.data-stores[].service.frontend-endpoint` |               | Kiev frontend endpoint. |
| `oci.kiev.data-stores[].service.locality`          | `oci.env.ad-number`, then `REGIONAL` | Store locality such as `REGIONAL`, `AD1`, `AD2`, or `AD3`. |
| `oci.kiev.data-stores[].service.auth.type`         | `INSTANCE`    | Auth type: `INSTANCE`, `S2S`, `OVERRIDDEN`, or `KIAB_LOCAL`. |

### Service auth configuration

`INSTANCE` auth requires:

| Key                                        | Default value | Description |
|--------------------------------------------|---------------|-------------|
| `oci.kiev.data-stores[].service.auth.auth-endpoint`      |               | Optional auth endpoint override. |
| `oci.kiev.data-stores[].service.auth.tls.root-cert-pem-path` |            | Root certificate PEM path. |
| `oci.kiev.data-stores[].service.auth.tls.root-cert-path` |               | Alias for `root-cert-pem-path`; configure only one of the two keys. |
| `oci.kiev.data-stores[].service.auth.tls.cert-reload-duration` |          | Optional certificate reload interval. |
| `oci.kiev.data-stores[].service.auth.tls.cert-ssl-algorithm` |            | Optional SSL algorithm override. |

`S2S` auth requires:

For `S2S`, configure the identity auth endpoint with
`oci.kiev.data-stores[].service.auth.service-principal.federation-endpoint`; do not combine `S2S` with the
top-level `oci.kiev.data-stores[].service.auth.auth-endpoint` key used by `INSTANCE`.
Kiev S2S requires explicit certificate configuration. Although the shared service-principal setting
`use-platform-provided` defaults to `true`, Kiev defaults it to `false` when the setting is omitted from an S2S auth
configuration loaded from application configuration. This contextual default is applied by the Kiev configuration
decorator. When constructing `KievServiceAuthConfig` programmatically without a configuration node, set
`usePlatformProvided(false)` on the nested `ServicePrincipalAuthConfig` builder. Kiev rejects `true` because
platform-provided service-principal material is not supported by the Kiev client configuration path.

```java
KievServiceAuthConfig authConfig = KievServiceAuthConfig.builder()
        .type(KievAuthType.S2S)
        .servicePrincipal(servicePrincipal -> servicePrincipal
                .usePlatformProvided(false)
                // Configure the federation endpoint, tenant ID, and certificates.
        )
        .build();
```

| Key                                            | Default value | Description |
|------------------------------------------------|---------------|-------------|
| `oci.kiev.data-stores[].service.auth.tls.root-cert-pem-path` |              | Root certificate PEM path. |
| `oci.kiev.data-stores[].service.auth.tls.root-cert-path`     |              | Alias for `root-cert-pem-path`; configure only one of the two keys. |
| `oci.kiev.data-stores[].service.auth.tls.cert-reload-duration` |            | Optional certificate reload interval. |
| `oci.kiev.data-stores[].service.auth.tls.cert-ssl-algorithm` |              | Optional SSL algorithm override. |
| `oci.kiev.data-stores[].service.auth.service-principal.use-platform-provided` | `false` | Effective default when loaded from application configuration. Programmatic builders must set `false`; `true` is not supported. |
| `oci.kiev.data-stores[].service.auth.service-principal.federation-endpoint` | | Identity auth endpoint. |
| `oci.kiev.data-stores[].service.auth.service-principal.tenant-id`          |              | Tenant OCID. |
| `oci.kiev.data-stores[].service.auth.service-principal.certificates.0.certificate`     |              | Leaf certificate path used for S2S credentials. |
| `oci.kiev.data-stores[].service.auth.service-principal.certificates.0.private-key` |              | Leaf private key path used for S2S credentials. |
| `oci.kiev.data-stores[].service.auth.service-principal.certificates.1.certificate` |          | Intermediate certificate path used for S2S credentials. |
| `oci.kiev.data-stores[].service.auth.service-principal.certificates.0.passphrase`     |              | Optional private key passphrase for S2S credentials. |

`OVERRIDDEN` auth requires a `BasicAuthenticationDetailsProvider` to be available from the Helidon service registry,
such as one created by the public OCI SDK integration under `helidon.oci.*`. It can also use the same
`oci.kiev.data-stores[].service.auth.tls.*` settings when Kiev-specific TLS handling is still needed, or it can reference
a reusable dynamic SSL context provider by name. The named provider is resolved from the Helidon service registry, so
applications can supply custom named `DynamicSslContextProviderConfig` services.

| Key                                            | Default value | Description |
|------------------------------------------------|---------------|-------------|
| `oci.kiev.data-stores[].service.auth.tls.dynamic-ssl-context-provider-name` | | Reusable dynamic SSL context provider name. |
| `oci.kiev.data-stores[].service.auth.tls.root-cert-pem-path` |       | Inline root certificate PEM path when no provider name is configured. |
| `oci.kiev.data-stores[].service.auth.tls.root-cert-path`     |       | Alias for `root-cert-pem-path`; configure only one of the two keys. |
| `oci.kiev.data-stores[].service.auth.tls.cert-reload-duration` |     | Optional inline certificate reload interval. |
| `oci.kiev.data-stores[].service.auth.tls.cert-ssl-algorithm` |       | Optional inline SSL algorithm override. |

Aliases are provided for user convenience, either to align with native OCI parameter names or with similar settings
in other Helidon Talon modules. Specify at most one name for an aliased setting, not both.

`KIAB_LOCAL` auth is intended for local KIAB KaaS testing and does not require extra auth properties.

---

## References

* [Store example](../../examples/store/README.md)
* [Kiev library configuration in the store example](../../examples/store/src/main/resources/application.yaml)

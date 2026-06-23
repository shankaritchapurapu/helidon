# Helidon Store Example

---

This example shows how to use the Helidon OCI Kiev integration through a simple HTTP store service.

## Prerequisites

- JDK 25
- Maven
- Docker

## Build

From `examples/store`:

```shell
mvn package
```

## Run

The bundled `application.yaml` uses the in-memory Kiev backend:

From `examples/store`:

```shell
java -jar ./target/helidon-oci-examples-store.jar
```

The sample `application.yaml` also includes a commented Kiev-as-a-service configuration that uses
`oci.kiev.data-stores[].service.auth.type=OVERRIDDEN`. In that mode, the example reuses the shared
OCI SDK auth provider from `helidon.oci.*` and keeps Kiev-specific TLS settings under
`oci.kiev.data-stores[].service.auth.tls.*`.

The service exposes plain-text item routes:

- `POST /store/items` with a `key=value` body to create an item
- `PUT /store/items/{key}` with a plain-text value body to update an existing item
- `GET /store/items/{key}` to read one item
- `DELETE /store/items/{key}` to remove one item
- `GET /store/items?pageSize=10&pageToken=...` to list items in ascending key order

It also exposes JSON stream routes:

- `GET /store/stream/cursors` to read the oldest and newest Kiev stream cursors
- `GET /store/stream?cursor=...&limit=10` to read Kiev stream records for the example item bucket

Call `/store/stream/cursors` first, then pass `oldest`, `newest`, or a previous response's `nextCursor` as the
required `cursor` query parameter.

The stream routes require the store to use the `SERVICE` backend. With the default in-memory backend or the
direct DB integration-test backend, they return `404` with a message explaining that Kiev streams require the
service backend. Set `oci.kiev.data-stores[].stream-deleted-column-values: true` if delete stream records
should include deleted column values.

## Test

Run unit tests:

From `examples/store`:

```shell
mvn test
```

## KIAB Direct DB Integration Test

The integration test profile in `src/test/resources/application-it.yaml` is configured for a local KIAB
Oracle-backed Kiev store using the direct DB client with:

- `data-stores[0].backend: "DIRECT_DB"`
- `data-stores[0].store-name: helidon-store-example`
- `data-stores[0].app-name: helidon-store-example`
- `data-stores[0].direct-db.jdbc-url: jdbc:oracle:thin:@//localhost:1521/pdbdev`
- `data-stores[0].direct-db.user-name: helidon`
- `data-stores[0].direct-db.password: changeit`
- `data-stores[0].direct-db.schema-name: helidon`

This path uses the stock Kiev direct DB client against a plain KIAB-created store. It does
not require starting the local KIAB KaaS services.

### 1. Install KIAB

Download `kiab-cli` as a tarball from the internal Artifactory under the `kiab-cli` releases. Prefer the latest
release and ignore snapshot artifacts, which are development builds.

Extract the tarball somewhere on your machine:

```shell
tar xvf kiab-cli-*.tgz
```

This creates a `kiab-cli` directory with the CLI files.

Add the `kiab` executable to your `PATH`. For example, in `.bashrc` or `.bash_profile`:

```shell
export PATH=$PATH:/path/to/kiab-cli/bin
```

Verify the installation from any directory:

```shell
kiab
```

It should print the top-level help menu.

### 2. macOS note

On macOS, especially Apple Silicon, Docker may need Colima with an `x86_64` VM to run the KIAB Oracle image:

```shell
brew install colima lima-additional-guestagents
colima start --memory 8 --arch x86_64
docker context use colima
```

If Docker already runs the KIAB image successfully on your machine, this step is not needed.

### 3. Start the local KIAB database

Create the KIAB CDB container. This provisions the default PDB named `pdbdev`:

```shell
kiab cdb create
```

Create a Kiev store in `pdbdev` using the same store name, user, password, and schema that the integration test is
configured to use:

```shell
kiab kiev create -d pdbdev -n helidon-store-example -u helidon -p changeit
```

For more details on installing and using KIAB, see
[`kiab-cli/readme.md`](https://bitbucket.oci.oraclecorp.com/projects/KIEV/repos/kiab-cli/browse/readme.md).

### 4. Run the integration test

From the repository root:

```shell
mvn -pl examples/store -am verify -DskipITs=false -Dit.test=StoreEndpointIT
```

This command was verified locally against the direct DB store created above.

### 5. Optional cleanup

If you also start local KIAB KaaS services for other experiments, stop them when finished:

```shell
kiab kaas teardown
```

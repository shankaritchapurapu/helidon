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

By default the example uses the in-memory Kiev backend:

From `examples/store`:

```shell
java -jar ./target/helidon-oci-examples-store.jar
```

The service exposes five plain-text routes:

- `POST /store/items` with a `key=value` body to create an item
- `PUT /store/items/{key}` with a plain-text value body to update an existing item
- `GET /store/items/{key}` to read one item
- `DELETE /store/items/{key}` to remove one item
- `GET /store/items?pageSize=10&pageToken=...` to list items in ascending key order

## Test

Run unit tests:

From `examples/store`:

```shell
mvn test
```

## KIAB Direct DB Integration Test

The integration test profile in `src/test/resources/application-it.yaml` is configured for a local KIAB
Oracle-backed Kiev store using the direct DB client with:

- `backend: "DIRECT_DB"`
- `store-name: pdbdev`
- `app-name: KievTest`
- `direct-db.jdbc-url: jdbc:oracle:thin:@//localhost:1521/pdbdev`
- `direct-db.user-name: helidon`
- `direct-db.password: changeit`
- `direct-db.schema-name: helidon`

This path uses the stock Kiev direct DB client against a plain KIAB-created store in `pdbdev`. It does
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

Create a Kiev store in `pdbdev` using the same user, password, and schema that the integration test is configured to use:

```shell
kiab kiev create -d pdbdev -n pdbdev -u helidon -p changeit
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

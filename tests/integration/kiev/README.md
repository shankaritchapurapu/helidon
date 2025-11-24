# Kiev Integration Test

## Overview

This module contains tests related to integration with Kiev.

Kiev offers an in-memory implementation of our own store, which is quite useful for testing. The In-Memory store is transient, meaning, it's only good for the duration of the process that is running it. 

If you require the store to be longer lasting, or to be used by multiple services at a time; then Kiev recommends using [Kiev-in-a-Box (Kiab)](https://bitbucket.oci.oraclecorp.com/projects/KIEV/repos/kiab-cli/browse). Kiab is a containerized Oracle store with some a pretty handy CLI on top of it. 

**&#9432;** On M1 Mac, you would need [to use Colima (and not Rancher) for running Oracle DB in Docker](https://confluence.oraclecorp.com/confluence/display/CSIS/Steps+to+Setup+Docker+DB+Locally+on+Mac+M1+using+colima)

## Configuration

### In-memory Setup

To use the in-memory configuration, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-repackaged-oci-kiev-in-memory-library</artifactId>
</dependency>
```
You then need to configure

```properties
kiev.clientType=in-memory
kiev.storeName=UnitTestStore
kiev.appName=helidon
```

**&#9432;** UnitTestStore is the important value here, as it's coded in in-memory library.

### Direct Database Setup

To use the direct database configuration, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-repackaged-oci-kiev-library</artifactId>
</dependency>
```

You then need to configure

```properties
kiev.clientType=directDb
kiev.jdbcURL=jdbc:oracle:thin:@//localhost:1521/pdbdev
kiev.storeName=pdbdev
kiev.appName=helidon
kiev.userName=helidon
kiev.password=helidon
```

**&#9432;** directDb is the important value here, as it's coded in library. Other values are based on your Kiab or local Kiev setup.

## Steps

1. Make sure your have configured Kiev for the test. By default, its setup for in-memory mode.
2. Run the test.
   ```shell
   $ mvn clean install
   ```

## References

* [Getting Started with Kiev SDK](https://confluence.oraclecorp.com/confluence/display/KIEV/Getting+Started+with+Kiev+SDK)
* [Using Kiev In-Memory for Unit Testing](https://confluence.oraclecorp.com/confluence/display/KIEV/Using+Kiev+In-Memory+for+Unit+Testing)
* [Kiev-in-a-Box (Kiab)](https://bitbucket.oci.oraclecorp.com/projects/KIEV/repos/kiab-cli/browse)
* [Oracle DB on M1 Mac using Colima](https://confluence.oraclecorp.com/confluence/display/CSIS/Steps+to+Setup+Docker+DB+Locally+on+Mac+M1+using+colima)
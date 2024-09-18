# Welcome to Helidon!

## OCI Native Library Integrations

## Overview

These project contains a compilation of different modules that extend the use of the
[Helidon framework]((https://github.com/helidon-io/helidon)) to allow integration with Native OCI services.

If you are building a service "built on public OCI SDK and public-facing services" then you can use
our [public quickstart for building OCI applications](https://helidon.io/starter/2.6.3?step=2&flavor=mp).

If, however, you are part of the customer or service enclave, and you need to integrate to the native/private
libraries that OCI teams produce (that are not part of the public OCI SDK), then you are at the right place.

## Prerequisites

- JDK 21 or higher
- Maven 3.6.1 or higher
- Helidon 4.1.1 or higher

## Usage

Please refer to the [Helidon-OCI Native Services Integration Guide](docs/README.md) for more details about the various features offered by this project.

## Build & Run

**Full build**

```bash
$ mvn clean package
```

**Checkstyle**

```bash
# cd to the component you want to check
$ mvn validate  -Pcheckstyle
```

**Copyright**

```bash
# cd to the component you want to check
$ mvn validate  -Pcopyright
```

**Spotbugs**

```bash
# cd to the component you want to check
$ mvn validate  -Pspotbugs
```

## How to locally test your OCI SDK Integration

Almost all of OCI native service integration requires _Instance Principal Authentication_. Normally, you would have to
deploy and run this integration in an OCI instance as it needs the _Instance Metadata Service_. However, Instance Principal
provider that is used to create an _Instance Principal Authentication_ can be set to point to a different _Instance Metadata Service_
base url.  

By Creating an SSH tunnel to forward connection from the chosen local endpoint on local host to the actual 
_Instance Metadata Service_ on remote host, then Instance Metadata Service can now be accessed locally. With this
approach, the test application does not need to be deployed to the remote host thereby promoting fast iteration to code changes.

As a prerequisite for making this approach successful, it is required to have a remote host with actual
_Instance Metadata Service_. Easiest way to get this is to use one of the instance created by our [reference-infra repository](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-reference-infra/browse/shepherd/README.md).

Below is example code to programmatically set the metadataBaseUrl and an example command to establish
an ssh tunnel to the remote Instance Metadata Service for testing the code locally.

1. Set Instance Metadata Service base URL (and some additional configuration) in `oci-config.yaml` either on classpath, or in the
   current directory:
    ```yaml
      authentication-method: "instance-principal" # hardcode to instance-principal authentication
      imds-timeout: "PT3S" # 3 seconds timeout, may be slower when tunneling
      imds-base-uri: "http://localhost:8000/opc/v2/" # tunneled
      imds-detect-retries: 1 # only try once, no need to try again if not available (unless you have bad connection)
    ```
2. Run an ssh tunnel to use the chosen local host and port to forward data to the Instance Metadata Service in the remote host:
   ```shell
   ssh -v -L 8000:169.254.169.254:80 oci-reference-service-ad1 -t watch -n 90 date
   ```

The [OCI T2 metrics test](tests/integration/t2-metrics) will show a complete example of this approach.


## Links

* Oracle Slack Channel: #helidon-users
* Public Site: https://helidon.io/
* Oracle Jira Issues (used for any/all Oracle Internal issue tracking): https://jira-sd.mc1.oracleiaas.com/projects/HLDN/queues

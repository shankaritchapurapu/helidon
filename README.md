# Welcome to Helidon!

## OCI Native Library Integrations

## Overview
These libraries are additions to what is found in our public [gitHub repository](https://github.com/helidon-io/helidon).
If you are building a service "built on public OCI SDK and public-facing services" then you can use
our [public quickstart for building OCI applications](https://helidon.io/starter/2.6.3?step=2&flavor=mp) instead of this repo.

If, however, you are part of the customer or service enclave, and you need to integrate to the native/private
libraries that OCI teams produce (that are not part of the public OCI SDK), then you are at the right place here.

## Prerequisites
- JDK 21 or higher
- Maven 3.6.1 or higher
- Helidon 4.1.0 or higher

## Usage
Most users should be generating their service using the [Helidon Service Generator](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-service-generator).
This repo provides the supporting libraries for services generated from that generator.

## Build & Run

```shell
mvn clean package
```

## How to locally test your OCI SDK Integration
Almost all calls to OCI native service integration requires _Instance Principal Authentication_. Normally, you would have to
deploy and run this integration in an OCI instance as it needs the _Instance Metadata Service_. However, Instance Principal
provider that is used to create an _Instance Principal Authentication_ can be set to point to a new _Instance Metadata Service_
base url. In combination with an ssh tunnel that can forward data from the chosen local endpoint to the actual
_Instance Metadata Service_ base url on the remote host, the application can now be run locally without the need to deploy it to
the remote instance. As a prerequisite for making this approach successful, all the necessary actions to onboard and provision
the OCI native service needs to be completed. Resource management is done using Shepherd, so the
[reference-infra repository](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/reference-infra/browse) can be modified to
add the needed resources. Below is example code to programmatically set the metadataBaseUrl and an example command to establish
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
   ssh -v -L 8000:169.254.169.254:80 host-api-ad1 -t watch -n 90 date
   ```

The [OCI T2 metrics test](integration-tests/oci-t2-metrics-test) will show a complete example of this approach

## Integration with Lumberjack for Application Logging
For more details on how to integrate with Lumberjack logging service, please check out [logging.md](logging.md).



## Links
* [Helidon Service Generator](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-service-generator)
* Oracle Slack Channel: #helidon-users
* Public Site: https://helidon.io/
* Oracle Jira Issues (used for any/all Oracle Internal issue tracking): https://jira.oci.oraclecorp.com/projects/HLDN/summary
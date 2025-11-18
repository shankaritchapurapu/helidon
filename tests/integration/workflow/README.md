# OCI Workflow Java Client Integration Test for Helidon SE

## Overview

This module contains tests related to integration with the internal OCI Workflow Java Client used by Helidon MP or SE-based applications. All test applications use the sample [DemoWorkflow.java](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/WOR/repositories/wfaas-client/files/661638087044aa97a39dd20042dcdb3cc514d52e?filePath=chast-javaclient%2Fsrc%2Fmain%2Fjava%2Fcom%2Foracle%2Fpic%2Fworkflow%2Fsample%2FDemoWorkflow.java&refName=refs%2Fheads%2Fmaster&fileName=DemoWorkflow.java&_ctx=us-phoenix-1%2Cdevops_scm_central&commitId=72164dbb9320936914f438ad000d83388fe09811) from the [wfaas-client devops repository](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/WOR/repositories/wfaas-client?_ctx=us-phoenix-1%2Cdevops_scm_central) to communicate with a Workflow In-Memory Docker-based Server. To facilitate bringing up the Workflow Server, [TestContainers](https://testcontainers.com) is used to manage the Docker container lifecycle. Currently, `TestContainers` is not supported in `Build Service`, so the test does not run by default and hence will not be triggered during the CI/CD pipeline. The test is triggered only by using the `WfaaSTest` Maven profile.


## Prerequisites

1. Install [Rancher Desktop](https://docs.rancherdesktop.io/getting-started/installation) and set it up based on your [Platform's Testcontainers requirements](https://docs.rancherdesktop.io/how-to-guides/using-testcontainers/).
2. Add the [Moby ryuk](https://github.com/testcontainers/moby-ryuk) v0.5.1 image to help manage Docker containers. This image is not available in any internal artifactory registry or on `container-registry.oracle.com`, so below are the steps that can be followed to build and push the image to your local Docker repository.
   1. Clone the `moby-ryuk` repository using Git.
      ```shell
      git clone https://github.com/testcontainers/moby-ryuk.git
      ```
   2. Navigate to the root of the cloned repository and check out the `0.5.1` branch.
      ```shell
      git checkout 0.5.1
      ```
   3. Copy the [./etc/Dockerfile.oel](./etc/Dockerfile.oel) file to the root of the cloned `moby-ryuk` repository. This is a modified version of the Dockerfile in the same repository and updated to use Oracle Linux v9 from the internal artifactory.
      ```shell
      # Assumption is that you are in the root of the moby-ruk repository
      cp <Cloned Repository of oci-helidon>/tests/integration/workflow/etc/Dockerfile.oel .
      ```
   4. Run `docker build` to create the image in the local Docker repository.
      ```shell
      docker build -t testcontainers/ryuk:0.5.1 -f Dockerfile.oel .
      ```


## Steps

1. Navigate to the root of the workflow integration test from the oci-helidon cloned repository.
   ```shell
   cd <Cloned Repository of oci-helidon>/tests/integration/workflow
   ```
2. Run the integration test, which is triggered only by using the `WfaaSTest` Maven profile. The following are the options:
   1. To run both Helidon SE and MP tests:
      ```shell
      $ mvn clean package -PWfaaSTest
      ```
   2. To run Helidon SE only test:
      ```shell
      mvn clean package -PWfaaSTest -pl se
      ```
   3. To run Helidon MP only test:
      ```shell
      mvn clean package -PWfaaSTest -pl microprofile
      ```

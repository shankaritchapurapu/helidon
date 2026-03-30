# OCI Helidon Pipeline Documentation

This document describes internal DevOps pipeline for this repository.

## Pipeline infrastructure

A pipeline is a group of stages executed sequentially or in parallel. OCI Helidon's pipeline contains a build and a test stage.
The pipeline resources are created and maintained through a Shepherd flock available under [oci-helidon-pipeline-infra repository](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-pipelines-infra?_ctx=us-phoenix-1%2Cdevops_scm_central). The main resources
are located in `helidon-pipeline` compartment in Helidon tenancy `helidonocidev` and *must never be deleted*. This specific compartment is whitelisted
by the DevOps pipeline team and gives access to internal OCI environment. Only one compartment can be whitelisted per tenancy.

### Build stage

The build stage uses internal build service configured by `ocibuild.conf`. If the branch is added to `releaseBranches`
attribute, the built artifacts can be accessed at `https://pipelines.artifactory.us-phoenix-1.oci.oracleiaas.com/` in later 
stages. 

### Test stage

The test stage runs integration tests on the built artifact during build stage. The `build_config/pipeline/test-spec.yaml` file is used to 
configure the integration tests process. Docker and Java are available in the test instance and can be used by the tests.

## Workflow

When working on a new branch, its name has to be added to `triggerOnCommitBranches` in order to trigger
a new pipeline job and publish the integration tests docker image. 

## Useful links

* [Internal Pipeline Documentation](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/pipelines/about-pipelines.htm)

<!-- Copyright (c) 2026 Oracle and/or its affiliates. -->

# Oracle Managed Kubernetes (OMK)

---

## Overview

[Oracle Managed Kubernetes (OMK)](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/omk/landing-omk.htm)
is OCI's managed Kubernetes environment for running service workloads. OMK uses cluster namespaces
to isolate a service's Kubernetes resources and supports managed networking, workload identity, and
OCI service integrations.

The Helidon Talon reference infrastructure demonstrates how to provision an OMK environment and
deploy a Helidon application to it. The implementation is maintained in the
[`oci-helidon-reference-infra-omk`](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-infra-omk/files?refName=refs%2Fheads%2Fmain&_ctx=us-phoenix-1%2Cdevops_scm_central)
repository as a customizable Shepherd flock backed by Terraform.

## Infrastructure Model

The Shepherd flock separates infrastructure provisioning from application deployment:

| Scope | Purpose |
|-------|---------|
| Tenancy | Configures IAM policies and access to artifacts in the Shepherd steward tenancy. |
| T2 telemetry | Handles telemetry allowlisting in the realm's T2 home region. |
| Regional infrastructure | Provisions the OMK cluster namespace, managed networking and reserved IP, DevOps deployment pipelines, Kiev, Lumberjack, Secret Service and PKI resources, and optional SPLAT resources. |
| Regional application | Publishes the selected container and Helm artifacts, injects infrastructure outputs into Helm values, and deploys the load balancer and application charts to the OMK cluster namespace. |

The application runs under a Kubernetes service account that supplies OMK workload identity. Runtime
configuration such as the cluster namespace, service account, application image, listener port,
Kiev endpoint, logging namespace, and Secret Service certificate path is passed to the Helm charts
by the application release.

## Infrastructure Configuration

The reference materials below describe customization parameters, release ordering, deployment steps,
and validation commands.

SPLAT is optional in the infrastructure flock. Provisioning SPLAT resources does not by itself enable
SPLAT request handling in the application; the application listener, certificate trust, API
specification, public endpoint, and deployment configuration must also be aligned.

## References

* [Helidon Talon OMK reference infrastructure overview](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-infra-omk/files?filePath=README.md&refName=refs%2Fheads%2Fmain&fileName=README.md&_ctx=us-phoenix-1%2Cdevops_scm_central)
* [OMK Shepherd flock documentation](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-infra-omk/files?filePath=shepherd%2FREADME.md&refName=refs%2Fheads%2Fmain&fileName=README.md&_ctx=us-phoenix-1%2Cdevops_scm_central)
* [Shepherd overview](https://confluence.oci.oraclecorp.com/display/DEVCENTRAL/Shepherd+Overview)

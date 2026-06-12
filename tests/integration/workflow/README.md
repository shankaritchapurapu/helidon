# Workflow Integration Test

This module verifies the Helidon OCI Workflow integration against WFaaS using the repository's configured `WorkflowClient`.

`WorkflowIT` covers two paths:

- `listsWorkflowInstances` creates a `WorkflowClient` and verifies the expected WFaaS domain. It calls
  `getWorkflowInstances` with a limit of one. It then asserts that WFaaS returns a non-null page with a non-null item
  list that respects the requested limit.
- `launchesWorkflow` creates a `WorkflowClient` and verifies the expected WFaaS domain. It creates or reuses a minimal
  workflow definition named `workflow-name`. It launches one workflow execution with a short unique tag, verifies the 
  launch response metadata, reads the launched workflow instance back by id, and cancels it before the test exits.

The workflow definition uses a single inert step named `workflow-step`. The short definition name, step name, and launch 
tag keep the WFaaS lease type id below the service length limit.

Together, these tests check that Helidon can construct the workflow client from configuration. They also verify that the
client can reach the WFaaS endpoint and list workflow instances. The launch test creates or reads a workflow definition,
launches a workflow instance, reads the launched instance, and cleans up the test-owned workflow execution.

## Prerequisites

The test uses instance principal authentication and must run from an OCI environment that can reach both the 
Instance Metadata Service and Workflow service.

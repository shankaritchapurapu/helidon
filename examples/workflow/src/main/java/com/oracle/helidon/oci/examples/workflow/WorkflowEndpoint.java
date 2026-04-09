/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.workflow;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

/**
 * HTTP endpoint that demonstrates using the workflow integration through an injected application service.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/workflow")
@Service.Singleton
class WorkflowEndpoint {
    private final ProvisioningWorkflowService workflowService;

    @Service.Inject
    WorkflowEndpoint(ProvisioningWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Http.POST
    @Http.Path("/instances")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    WorkflowSnapshot launch(@Http.Entity LaunchWorkflowRequest request) {
        if (request == null) {
            throw new HttpException("Request body is required", Status.BAD_REQUEST_400);
        }
        String resourceId = required(request.resourceId(), "resourceId");
        return workflowService.launch(resourceId, request.simulateFailure());
    }

    @Http.GET
    @Http.Path("/instances/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    WorkflowSnapshot get(@Http.PathParam("id") String workflowInstanceId) {
        String id = workflowInstanceId == null ? "" : workflowInstanceId.trim();
        if (id.isEmpty()) {
            throw new HttpException("Workflow instance id is required", Status.BAD_REQUEST_400);
        }
        return workflowService.get(id);
    }

    private static String required(String value, String key) {
        value = value == null ? null : value.trim();
        if (value == null || value.isBlank()) {
            throw new HttpException("Request must contain " + key, Status.BAD_REQUEST_400);
        }
        return value;
    }
}

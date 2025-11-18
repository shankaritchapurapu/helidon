/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.workflow.microprofile;

import com.oracle.tests.integration.workflow.sample.DemoWorkflow;

import com.oracle.pic.workflow.worker.WorkflowClient;
import com.oracle.pic.workflow.module.WorkflowClientModule;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/workflow")
@RequestScoped
public class WorkflowResource {
    private WorkflowClient workerClient;
    private WorkflowClient pollerCLient;

    @Inject
    public WorkflowResource(@Named(WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME) WorkflowClient workerClient,
                            @Named(WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME) WorkflowClient pollerClient) {
        this.workerClient = workerClient;
        this.pollerCLient = pollerClient;
    }

    @GET
    public Response getWorkflowTest() throws Exception {
        System.out.println("getWorkflowTest() is called");
        try {
            DemoWorkflow.demoWorkflow(workerClient, pollerCLient);
            System.out.println("Successfully called DemoWorkflow.main()");
        } catch (Throwable t) {
            System.out.println("DemoWorkflow failed!!!");
            t.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), t.getMessage()).build();
        }
        return Response.status(Response.Status.OK).build();
    }

}

/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.workflow.se;

import com.oracle.tests.integration.workflow.sample.DemoWorkflow;

import com.oracle.pic.workflow.worker.WorkflowClient;
import com.oracle.pic.workflow.module.WorkflowClientModule;

// import io.helidon.common.Default;
// import io.helidon.common.media.type.MediaTypes;
// import io.helidon.config.Configuration;
// import io.helidon.http.Http;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.service.registry.Service;
// import io.helidon.webserver.http.RestServer;

@Service.Singleton
public class WorkflowService implements HttpService {
    private WorkflowClient workerClient;
    private WorkflowClient pollerCLient;

    @Service.Inject
    public WorkflowService(@Service.Named(WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME) WorkflowClient workerClient,
                           @Service.Named(WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME) WorkflowClient pollerClient) {
        this.workerClient = workerClient;
        this.pollerCLient = pollerClient;
        System.out.println("Workerclient: " + workerClient);
        System.out.println("Pollerclient: " + pollerCLient);
    }

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void routing(HttpRules rules) {
        rules.get("/", this::getWorkflowTest);
    }

    public void getWorkflowTest(ServerRequest request, ServerResponse response) {
        System.out.println("getWorkflowTest() is called");
        try {
            DemoWorkflow.demoWorkflow(workerClient, pollerCLient);
            System.out.println("Successfully called DemoWorkflow.main()");
        } catch (Throwable t) {
            System.out.println("DemoWorkflow failed!!!");
            t.printStackTrace();
            response.status(Status.INTERNAL_SERVER_ERROR_500).send();
            return;
        }
        response.status(Status.OK_200).send();
    }
}

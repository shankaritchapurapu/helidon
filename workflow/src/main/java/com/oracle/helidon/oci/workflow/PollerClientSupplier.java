/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.pic.workflow.Utils.ClientRole;
import com.oracle.pic.workflow.module.WorkflowClientModule;
import com.oracle.pic.workflow.worker.WorkflowClient;

/**
 * Provides a named Workflow poller client compatible with the legacy WFaaS binding name.
 */
@Service.Singleton
@Service.Named(WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME)
@Weight(Weighted.DEFAULT_WEIGHT - 30)
public class PollerClientSupplier implements Supplier<WorkflowClient> {
    private final WorkflowClientFactory workflowClientFactory;

    @Service.Inject
    PollerClientSupplier(WorkflowClientFactory workflowClientFactory) {
        this.workflowClientFactory = workflowClientFactory;
    }

    @Override
    public WorkflowClient get() {
        return workflowClientFactory.createWorkflowClient(ClientRole.POLLER);
    }
}

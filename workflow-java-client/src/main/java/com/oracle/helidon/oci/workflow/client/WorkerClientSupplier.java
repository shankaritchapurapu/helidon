/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.pic.workflow.Utils.ClientRole;
import com.oracle.pic.workflow.module.WorkflowClientModule;
import com.oracle.pic.workflow.worker.WorkflowClient;

/**
 * Provides Workflow worker client via {@code @Named(WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME)}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 100)
@Service.Named(WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME)
public class WorkerClientSupplier implements Supplier<WorkflowClient> {
    private static final Logger LOGGER = Logger.getLogger(WorkerClientSupplier.class.getName());
    private final OciWorkflowClient ociWorkflowClient;

    @Service.Inject
    WorkerClientSupplier(OciWorkflowClient ociWorkflowClient){
        this.ociWorkflowClient = ociWorkflowClient;
    }

    @Override
    public WorkflowClient get() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "getWorkerClient() is invoked");
        }
        return ociWorkflowClient.createWorkflowClient(ClientRole.WORKER);
    }
}

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
 * Provides Workflow poller client via {@code @Named(WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME)}.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 100)
@Service.Named(WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME)
public class PollerClientSupplier implements Supplier<WorkflowClient> {
    private static final Logger LOGGER = Logger.getLogger(PollerClientSupplier.class.getName());
    private final OciWorkflowClient ociWorkflowClient;

    @Service.Inject
    PollerClientSupplier(OciWorkflowClient ociWorkflowClient) {
        this.ociWorkflowClient = ociWorkflowClient;
    }

    @Override
    public WorkflowClient get() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "getPollerClient() is invoked");
        }
        return ociWorkflowClient.createWorkflowClient(ClientRole.POLLER);
    }
}

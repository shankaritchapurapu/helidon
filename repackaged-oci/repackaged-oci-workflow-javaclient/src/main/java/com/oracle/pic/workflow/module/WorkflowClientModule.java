/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */
package com.oracle.pic.workflow.module;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.function.Consumer;

import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.pic.workflow.Utils.ClientRole;
import com.oracle.pic.workflow.Utils.ConnectionPoolStatsReporter;
import com.oracle.pic.workflow.Utils.Lifecycle;
import com.oracle.pic.workflow.Utils.dynamiccert.AuthDetailsConfig;
import com.oracle.pic.workflow.worker.ChastWorkflowClient;
import com.oracle.pic.workflow.worker.RetryPolicy;
import com.oracle.pic.workflow.worker.WorkflowClient;
import com.oracle.pic.workflow.worker.WorkflowEndpointConfiguration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import static java.lang.System.Logger.Level.INFO;

/**
 * Repackaged WorkflowClientModule that removes Google Guice.
 */
public class WorkflowClientModule {
    /**
     * Workflow worker client name.
     */
    public static final String WORKFLOW_WORKER_CLIENT_NAME = "workflow-worker-client";
    /**
     * Workflow poller client name.
     */
    public static final String WORKFLOW_POLLER_CLIENT_NAME = "workflow-poller-client";
    private static final System.Logger LOGGER = System.getLogger(WorkflowClientModule.class.getName());
    private ConnectionPoolStatsReporter poolStatsReporter = null;
    private ClientConfigurator clientConfigurator = null;
    private RetryPolicy retryPolicy = null;
    private WorkflowEndpointConfiguration workflowEndpointConfiguration = null;
    private String domainId = null;
    private String workerIdentifier = null;
    private AuthDetailsConfig authDetailsConfig = null;

    /**
     *  For In-Memory Usage.
     *
     * @param workflowEndpointConfiguration endpoint configuration for the Workflow client and value not be {@code null}
     * @param domainId workflow domain id and value must not be {@code null}
     * @param managedComponentsConsumer managed components consumer
     */
    public WorkflowClientModule(@NotNull WorkflowEndpointConfiguration workflowEndpointConfiguration,
                                @NotNull String domainId,
                                @NotNull Consumer<List<Lifecycle>> managedComponentsConsumer) {
        this(workflowEndpointConfiguration,
                domainId,
                managedComponentsConsumer,
                null,
                null,
                null,
                ManagementFactory.getRuntimeMXBean().getName());
    }

    /**
     * General Use Case Constructor w/ Dynamic Cert Reloading.
     *
     * @param workflowEndpointConfiguration endpoint configuration for the Workflow client and value not be {@code null}
     * @param domainId workflow domain id and value must not be {@code null}
     * @param managedComponentsConsumer managed components consumer
     * @param authDetailsConfig type of the gauge
     */
    public WorkflowClientModule(@NotNull WorkflowEndpointConfiguration workflowEndpointConfiguration,
                                @NotNull String domainId,
                                @NotNull Consumer<List<Lifecycle>> managedComponentsConsumer,
                                @Null AuthDetailsConfig authDetailsConfig) {
        this(workflowEndpointConfiguration,
                domainId,
                managedComponentsConsumer,
                authDetailsConfig,
                null,
                null,
                ManagementFactory.getRuntimeMXBean().getName());
    }

    /**
     * Catch all constructor for customers that want full power to customize.
     *
     * @param workflowEndpointConfiguration endpoint configuration for the Workflow client and value not be {@code null}
     * @param domainId workflow domain id and value must not be {@code null}
     * @param managedComponentsConsumer managed components consumer
     * @param authDetailsConfig type of the gauge
     * @param clientConfigurator client configurator
     * @param retryPolicy retry policy
     * @param workerName workflow worker name
     */
    public WorkflowClientModule(@NotNull WorkflowEndpointConfiguration workflowEndpointConfiguration,
                                @NotNull String domainId,
                                @NotNull Consumer<List<Lifecycle>> managedComponentsConsumer,
                                @Null AuthDetailsConfig authDetailsConfig,
                                @Null ClientConfigurator clientConfigurator,
                                @Null RetryPolicy retryPolicy,
                                @Null String workerName) {
        LOGGER.log(INFO, "Initializing workflow client module with endpoint configuration: {0}, domainId: {1}, "
                   + "AuthDetailsConfig: {2}, ClientConfigurator: {3}, retry policy: {4}, worker name: {5}",
                   workflowEndpointConfiguration, domainId, authDetailsConfig, clientConfigurator, retryPolicy, workerName);
        this.workflowEndpointConfiguration = workflowEndpointConfiguration;
        this.domainId = domainId;
        this.authDetailsConfig = authDetailsConfig;
        this.poolStatsReporter = new ConnectionPoolStatsReporter();
        this.clientConfigurator = clientConfigurator;
        this.retryPolicy = retryPolicy;
        this.workerIdentifier = workerName;
        managedComponentsConsumer.accept(List.of(this.poolStatsReporter));
    }

    /**
     * Provides Workflow worker client.
     *
     * @return new worker client
     */
    public WorkflowClient getWorkerClient() {
        return buildClient(ClientRole.WORKER);
    }

    /**
     * Provides Workflow poller client.
     *
     * @return new poller client
     */
    public WorkflowClient getPollerClient() {
        return buildClient(ClientRole.POLLER);
    }

    private WorkflowClient buildClient(ClientRole clientRole) {
        return ChastWorkflowClient.builder()
                .domainId(domainId)
                .workerIdentifier(workerIdentifier)
                .endpointConfig(workflowEndpointConfiguration)
                .clientRole(clientRole)
                .clientConfigurator(clientConfigurator)
                .authDetailsConfig(authDetailsConfig)
                .connectionPoolStatsReporter(poolStatsReporter)
                .retryPolicy(retryPolicy)
                .build();
    }
}

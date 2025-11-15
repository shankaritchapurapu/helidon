/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.workflow.Utils.ClientRole;
import com.oracle.pic.workflow.Utils.ConnectionPoolStatsReporter;
import com.oracle.pic.workflow.Utils.dynamiccert.AuthDetailsConfig;
import com.oracle.pic.workflow.worker.ChastWorkflowClient;
import com.oracle.pic.workflow.worker.WorkflowClient;
import com.oracle.pic.workflow.worker.WorkflowEndpointConfiguration;

import static com.oracle.pic.workflow.module.WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME;
import static com.oracle.pic.workflow.module.WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME;

/**
 * Workflow Client Bean that can provide worker and poller client.
 */
@Service.Singleton
public class OciWorkflowClient {
    private static final String WORKFLOW_CONFIG_PREFIX = "oci.workflow";
    private static final String DYNAMIC_SSL_CONTEXT_PROVIDER_PREFIX = "oci.dynamic-ssl-context-provider";
    private static final Logger LOGGER = Logger.getLogger(OciWorkflowClient.class.getName());
    private ConnectionPoolStatsReporter poolStatsReporter;
    private WorkflowClientConfig workflowClientConfig;
    private final Supplier<WorkflowClient> workerClient;
    private final Supplier<WorkflowClient> pollerClient;
    // Lazy loading of BasicAuthenticationDetailsProvider so it will only be retrieved from registry when really needed
    private final Supplier<BasicAuthenticationDetailsProvider> authenticationDetailsProvider;
    private Config rootConfig;

    @Service.Inject
    OciWorkflowClient(Config rootConfig,
                      Supplier<BasicAuthenticationDetailsProvider> authenticationDetailsProvider,
                      @Service.Named(WORKFLOW_POLLER_CLIENT_NAME)
                       Supplier<WorkflowClient> workerClient,
                      @Service.Named(WORKFLOW_WORKER_CLIENT_NAME)
                       Supplier<WorkflowClient> pollerClient) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("OciWorkflowClient() is instantiated");
        }
        this.rootConfig = rootConfig;
        this.authenticationDetailsProvider = authenticationDetailsProvider;
        workflowClientConfig = WorkflowClientConfig.builder()
                .config(rootConfig.get(WORKFLOW_CONFIG_PREFIX))
                .build();
        this.workerClient = workerClient;
        this.pollerClient = pollerClient;
    }

    @Service.PostConstruct
    void onStartup() {
        this.poolStatsReporter = new ConnectionPoolStatsReporter();
        poolStatsReporter.start();
    }

    @Service.PreDestroy
    void onShutdown() {
        poolStatsReporter.stop();
    }

    /**
     * Provides Workflow worker client.
     *
     * @return a worker client
     */
    public WorkflowClient workerClient(){
        return workerClient.get();
    }

    /**
     * Provides Workflow poller client.
     *
     * @return a poller client
     */
    public WorkflowClient pollerClient(){
        return pollerClient.get();
    }

    private Optional<AuthDetailsConfig> getAuthDetailsConfig() {
        var helidonDynamicSslContextProviderConfig = rootConfig.get(DYNAMIC_SSL_CONTEXT_PROVIDER_PREFIX);
        if (helidonDynamicSslContextProviderConfig.exists() && !workflowClientConfig.domainId().equals("localhost")) {
            DynamicSslCtxProviderConfig dynamicSslCtxProviderConfig =
                    DynamicSslCtxProviderConfig.create(helidonDynamicSslContextProviderConfig);
            String rootCertPath = dynamicSslCtxProviderConfig.rootCertPath();
            var dynSslCtxConfig = new DynamicSslContextProviderConfig(null, null, null, rootCertPath);
            dynamicSslCtxProviderConfig.duration().ifPresent(dynSslCtxConfig::setDuration);
            return Optional.of(new AuthDetailsConfig(dynSslCtxConfig, authenticationDetailsProvider.get()));
        }
        return Optional.empty();
    }

    private WorkflowEndpointConfiguration buildWorkflowEndpointConfig() {
        EndpointConfig endpointConfig = workflowClientConfig.endpointDetails();
        return WorkflowEndpointConfiguration.builder()
                .workflowServerEndpoint(endpointConfig.serverEndpoint())
                .connectTimeoutMillis(endpointConfig.connectTimeoutMillis())
                .workerReadTimeoutMillis(endpointConfig.workerReadTimeoutMillis())
                .pollerReadTimeoutMillis(endpointConfig.pollerReadTimeoutMillis())
                .build();
    }

    ChastWorkflowClient createWorkflowClient(ClientRole clientRole) {
        var clientBuilder =  ChastWorkflowClient.builder()
                .clientRole(clientRole)
                .domainId(workflowClientConfig.domainId())
                .workerIdentifier(ManagementFactory.getRuntimeMXBean().getName())
                .endpointConfig(buildWorkflowEndpointConfig())
                .clientConfigurator(httpClientBuilder -> {})
                .connectionPoolStatsReporter(poolStatsReporter);

        getAuthDetailsConfig().ifPresent(clientBuilder::authDetailsConfig);

        return clientBuilder.build();
    }

    WorkflowClientConfig workflowClientConfig() {
        return workflowClientConfig;
    }
}

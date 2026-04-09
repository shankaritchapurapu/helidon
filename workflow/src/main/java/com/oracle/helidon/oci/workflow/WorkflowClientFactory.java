/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.workflow.Utils.ClientRole;
import com.oracle.pic.workflow.Utils.ConnectionPoolStatsReporter;
import com.oracle.pic.workflow.Utils.dynamiccert.AuthDetailsConfig;
import com.oracle.pic.workflow.worker.ChastWorkflowClient;
import com.oracle.pic.workflow.worker.RetryPolicy;
import com.oracle.pic.workflow.worker.WorkflowClient;
import com.oracle.pic.workflow.worker.WorkflowEndpointConfiguration;

/**
 * Factory for creating configured {@link WorkflowClient} instances from generated workflow configuration.
 */
@Service.Singleton
public class WorkflowClientFactory implements Supplier<WorkflowClient> {
    private static final String DYNAMIC_SSL_CONTEXT_PROVIDER_PREFIX = "oci.dynamic-ssl-context-provider";

    private final Config rootConfig;
    private final WorkflowConfig config;
    private final BasicAuthenticationDetailsProvider authProvider;
    private final ConnectionPoolStatsReporter poolStatsReporter = new ConnectionPoolStatsReporter();

    /**
     * Create a new workflow client factory.
     *
     * @param rootConfig Helidon root configuration
     * @param config generated workflow configuration
     * @param authProvider OCI authentication details provider
     */
    @Service.Inject
    public WorkflowClientFactory(Config rootConfig,
                                 WorkflowConfig config,
                                 BasicAuthenticationDetailsProvider authProvider) {
        this.rootConfig = rootConfig;
        this.config = config;
        this.authProvider = authProvider;
        this.poolStatsReporter.start();
    }

    WorkflowClientFactory(WorkflowConfig config,
                          BasicAuthenticationDetailsProvider authProvider) {
        this(Config.empty(), config, authProvider);
    }

    @Override
    public WorkflowClient get() {
        return createWorkflowClient(ClientRole.WORKER);
    }

    WorkflowClient createWorkflowClient(ClientRole clientRole) {
        ChastWorkflowClient.ChastWorkflowClientBuilder builder = ChastWorkflowClient.builder()
                .clientRole(clientRole)
                .domainId(config.domainId())
                .workerIdentifier(config.workerIdentifier().orElseGet(WorkflowClientFactory::defaultWorkerIdentifier))
                .endpointConfig(workflowEndpointConfiguration())
                .connectionPoolStatsReporter(poolStatsReporter);
        authDetailsConfig().ifPresent(builder::authDetailsConfig);
        config.retryPolicy().map(WorkflowClientFactory::retryPolicy).ifPresent(builder::retryPolicy);
        return builder.build();
    }

    private WorkflowEndpointConfiguration workflowEndpointConfiguration() {
        EndpointConfig endpointConfig = config.endpointDetails();
        return WorkflowEndpointConfiguration.builder()
                .workflowServerEndpoint(endpointConfig.serverEndpoint())
                .connectTimeoutMillis(toIntMillis(endpointConfig.connectTimeout()))
                .workerReadTimeoutMillis(toIntMillis(endpointConfig.workerReadTimeout()))
                .pollerReadTimeoutMillis(toIntMillis(endpointConfig.pollerReadTimeout()))
                .build();
    }

    private Optional<AuthDetailsConfig> authDetailsConfig() {
        Config dynamicSslConfig = rootConfig.get(DYNAMIC_SSL_CONTEXT_PROVIDER_PREFIX);
        if (!dynamicSslConfig.exists() || "localhost".equals(config.domainId())) {
            return Optional.empty();
        }

        DynamicSslCtxProviderConfig sslConfig = DynamicSslCtxProviderConfig.create(dynamicSslConfig);
        if (sslConfig.rootCertPath().isEmpty()) {
            return Optional.empty();
        }

        DynamicSslContextProviderConfig providerConfig = new DynamicSslContextProviderConfig(
                sslConfig.leafCertPath().orElse(null),
                sslConfig.leafCertKeyPath().orElse(null),
                sslConfig.leafCertKeyPassphrase().orElse(null),
                sslConfig.rootCertPath().orElseThrow());
        sslConfig.intermediateCertPath().ifPresent(providerConfig::setIntermediateCertPath);
        sslConfig.duration().ifPresent(providerConfig::setDuration);
        return Optional.of(new AuthDetailsConfig(providerConfig, authProvider));
    }

    private static String defaultWorkerIdentifier() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }

    private static RetryPolicy retryPolicy(RetryPolicyConfig config) {
        return RetryPolicy.builder()
                .maxRetryCount(config.maxRetryCount())
                .delayBetweenRetryInMillis(Math.toIntExact(config.delayBetweenRetry().toMillis()))
                .jitterFactor(config.jitterFactor())
                .build();
    }

    private static int toIntMillis(java.time.Duration duration) {
        return Math.toIntExact(duration.toMillis());
    }
}

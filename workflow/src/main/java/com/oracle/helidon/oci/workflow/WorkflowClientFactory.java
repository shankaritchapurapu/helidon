/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistry;

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
@Weight(Weighted.DEFAULT_WEIGHT - 30)
public class WorkflowClientFactory implements Supplier<WorkflowClient> {
    private static final String LEGACY_DYNAMIC_SSL_CONTEXT_PROVIDER_PREFIX = "oci.dynamic-ssl-context-provider";
    private static final String DEFAULT_DYNAMIC_SSL_CONTEXT_PROVIDER_NAME = "workflow";

    private final Config rootConfig;
    private final WorkflowConfig config;
    private final Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider;
    private final ServiceRegistry serviceRegistry;
    private final ConnectionPoolStatsReporter poolStatsReporter = new ConnectionPoolStatsReporter();

    /**
     * Create a new workflow client factory.
     *
     * @param rootConfig Helidon root configuration
     * @param config generated workflow configuration
     * @param authProvider lazy optional OCI authentication details provider
     * @param serviceRegistry Helidon service registry
     */
    @Service.Inject
    public WorkflowClientFactory(Config rootConfig,
                                 WorkflowConfig config,
                                 Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider,
                                 ServiceRegistry serviceRegistry) {
        this.rootConfig = rootConfig;
        this.config = config;
        this.authProvider = authProvider;
        this.serviceRegistry = serviceRegistry;
        this.poolStatsReporter.start();
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

    Optional<AuthDetailsConfig> authDetailsConfig() {
        if ("localhost".equals(config.domainId())) {
            return Optional.empty();
        }

        Optional<DynamicSslContextProviderConfig> providerConfig = namedDynamicSslProviderConfig()
                .or(this::legacyDynamicSslProviderConfig);
        if (providerConfig.isEmpty()) {
            return Optional.empty();
        }

        BasicAuthenticationDetailsProvider provider = authProvider.get().orElseThrow(() -> new IllegalStateException(
                "A BasicAuthenticationDetailsProvider must be available in the service registry when workflow "
                        + "dynamic SSL context provider configuration includes a root certificate path."));
        return Optional.of(new AuthDetailsConfig(providerConfig.orElseThrow(), provider));
    }

    private Optional<DynamicSslContextProviderConfig> namedDynamicSslProviderConfig() {
        Optional<String> configuredName = config.dynamicSslContextProviderName();
        if (configuredName.isPresent()) {
            String name = configuredName.get();
            if (name.isBlank()) {
                throw new IllegalStateException("oci.workflow.dynamic-ssl-context-provider-name must not be blank");
            }
            return Optional.of(serviceRegistry.getNamed(DynamicSslContextProviderConfig.class, name));
        }

        return serviceRegistry.firstNamed(DynamicSslContextProviderConfig.class,
                                          DEFAULT_DYNAMIC_SSL_CONTEXT_PROVIDER_NAME);
    }

    private Optional<DynamicSslContextProviderConfig> legacyDynamicSslProviderConfig() {
        Config dynamicSslConfig = rootConfig.get(LEGACY_DYNAMIC_SSL_CONTEXT_PROVIDER_PREFIX);
        if (!dynamicSslConfig.exists()) {
            return Optional.empty();
        }

        Optional<String> rootCertPath = dynamicSslConfig.get("root-cert-path").asString().asOptional();
        if (rootCertPath.map(String::isBlank).orElse(true)) {
            return Optional.empty();
        }

        DynamicSslContextProviderConfig providerConfig = new DynamicSslContextProviderConfig();
        dynamicSslConfig.get("leaf-cert-path").asString().ifPresent(providerConfig::setLeafCertPath);
        dynamicSslConfig.get("leaf-cert-key-path").asString().ifPresent(providerConfig::setLeafCertKeyPath);
        dynamicSslConfig.get("leaf-cert-key-passphrase").asString().ifPresent(providerConfig::setLeafCertKeyPassphrase);
        dynamicSslConfig.get("intermediate-cert-path").asString().ifPresent(providerConfig::setIntermediateCertPath);
        providerConfig.setRootCertPath(rootCertPath.orElseThrow());
        dynamicSslConfig.get("duration").as(java.time.Duration.class).ifPresent(providerConfig::setDuration);
        dynamicSslConfig.get("ssl-algorithm").asString().ifPresent(providerConfig::setSslAlgorithm);
        return Optional.of(providerConfig);
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

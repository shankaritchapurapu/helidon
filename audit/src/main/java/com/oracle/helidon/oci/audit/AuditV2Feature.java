/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.spi.ServerFeature;

import com.oracle.pic.sherlock.collector.AuditLogger;
import com.oracle.pic.sherlock.collector.LogbackAuditLogger;

/**
 * Server features are automatically discovered by Helidon WebServer when running with
 * {@link io.helidon.service.registry.ServiceRegistryManager#start(io.helidon.service.registry.Binding)}.
 */
@RuntimeType.PrototypedBy(AuditV2Config.class)
public class AuditV2Feature implements ServerFeature, RuntimeType.Api<AuditV2Config> {

    static final String AUDIT_ID = "auditv2";

    private final AuditV2Config config;
    private final AuditLogger auditLogger;

    private AuditV2Feature(AuditV2Config config, AuditLogger auditLogger) {
        this.config = config;
        this.auditLogger = auditLogger;
    }

    /**
     * Create a new audit v2 feature based on its configuration.
     *
     * @param config configuration
     * @param auditLogger auditLogger
     * @return a new observe feature
     */
    public static AuditV2Feature create(AuditV2Config config, AuditLogger auditLogger) {
        return new AuditV2Feature(config, auditLogger);
    }

    /**
     * Create a new audit v2 feature based on its configuration.
     *
     * @param config configuration
     * @return a new observe feature
     */
    public static AuditV2Feature create(AuditV2Config config) {
        return new AuditV2Feature(config, LogbackAuditLogger.INSTANCE);
    }

    /**
     * Create a new audit v2 feature customizing its configuration.
     *
     * @param consumer configuration consumer
     * @return a new observe feature
     */
    public static AuditV2Feature create(Consumer<AuditV2Config.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * A new builder to customize audit v2 feature support.
     *
     * @return a new builder
     */
    public static AuditV2Config.Builder builder() {
        return AuditV2Config.builder();
    }

    @Override
    public AuditV2Config prototype() {
        return config;
    }

    @Override
    public String name() {
        return AUDIT_ID;
    }

    @Override
    public String type() {
        return AUDIT_ID;
    }

    @Override
    public void setup(ServerFeatureContext featureContext) {
        if (config.enabled()) {
            AuditV2Filter filter = new AuditV2Filter(config, auditLogger);
            featureContext.socket(WebServer.DEFAULT_SOCKET_NAME).httpRouting().addFilter(filter);
            for (String socket : featureContext.sockets()) {
                featureContext.socket(socket).httpRouting().addFilter(filter);
            }
        }
    }
}

/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.Errors;
import io.helidon.config.Config;
import io.helidon.integrations.oci.ImdsInstanceInfo;
import io.helidon.integrations.oci.metrics.OciMetricsSupport;
import io.helidon.integrations.oci.metrics.OciMetricsSupportFactory;
import io.helidon.microprofile.server.RoutingBuilders;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;
import io.helidon.service.registry.ServiceRegistryException;

import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporter;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporterBuilder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import static jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;

/**
 * Internal OCI metrics implementation of the integration set-up bean.
 * <p>
 * This bean expects internal OCI metrics configuration under {@code oci.metrics} with {@code project} and {@code fleet}
 * under {@code oci}.
 */
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
@Alternative
@Singleton
class InternalOciMetricsBean extends OciMetricsSupportFactory {
    private static final Logger LOGGER = Logger.getLogger(InternalOciMetricsBean.class.getName());

    private Monitoring monitoring;
    private String project;
    private String fleet;
    private ImdsInstanceInfo instanceInfo;

    // Make Priority higher than MetricsCdiExtension so this will only start after MetricsCdiExtension has completed.
    void registerOciMetrics(@Observes @Priority(LIBRARY_BEFORE + 20) @Initialized(ApplicationScoped.class) Object ignore,
                            Config rootConfig, Monitoring monitoringClient) {
        registerOciMetrics(rootConfig, monitoringClient);
    }

    @Override
    protected String configKey() {
        return "oci.metrics";
    }

    @Override
    protected OciMetricsSupport.Builder ociMetricsSupportBuilder(Config rootConfig,
                                                                 Config ociMetricsConfig,
                                                                 Monitoring monitoring) {
        // Adjust the endpoint according to the documentation if it has not already somehow been set that way.
        String originalEndpoint = monitoring.getEndpoint();
        if (!originalEndpoint.contains("telemetry-ingestion")) {
            String adjustedEndpoint = originalEndpoint.replaceFirst("telemetry\\.", "telemetry-ingestion.");
            monitoring.setEndpoint(adjustedEndpoint);
            LOGGER.log(Level.FINE, "Setting monitoring endpoint to '" + adjustedEndpoint + "'");
        }

        this.monitoring = monitoring;

        OciMetricsSupport.Builder result = super.ociMetricsSupportBuilder(rootConfig, ociMetricsConfig, monitoring);
        if (result.enabled()) {
            ServiceRegistry registry = GlobalServiceRegistry.registry();
            try {
                instanceInfo = registry.get(ImdsInstanceInfo.class);
            } catch (ServiceRegistryException t) {
                LOGGER.log(Level.WARNING, "Metrics will be disabled due to a failure in retrieving "
                        + "IMDS instance information with error: " + t);
                result.enabled(false);
                return result;
            }

            Errors.Collector collector = Errors.collector();

            // Use synonyms in config--project for namespace and fleet for resourceGroup.
            // Abort if either is missing.
            ociMetricsConfig().get("project").asString().ifPresentOrElse(project -> {
                                                                             result.namespace(project);
                                                                             this.project = project;
                                                                         },
                                                                         () -> collector.fatal(
                                                                                 "required OCI metrics config setting for "
                                                                                         + "project "
                                                                                         + "is missing"));

            ociMetricsConfig().get("fleet").asString().ifPresentOrElse(fleet -> {
                                                                           result.resourceGroup(fleet);
                                                                           this.fleet = fleet;
                                                                       },
                                                                       () -> collector.fatal(
                                                                               "required OCi metrics config setting for fleet is "
                                                                                       + "missing"));
            result.compartmentId(MetricsCompartmentHelper.t2CompartmentIdForRegion(instanceInfo.canonicalRegionName()));

            Errors errors = collector.collect();
            if (errors.hasFatal()) {
                throw new RuntimeException("Error preparing OCI metrics integration: " + errors);
            }
        }
        return result;
    }

    @Override
    protected void activateOciMetricsSupport(Config rootConfig, Config ociMetricsConfig, OciMetricsSupport.Builder builder) {
        if (builder.enabled()) {
            OciMetricsSupport ociMetricsSupport = builder.build();
            RoutingBuilders.create(ociMetricsConfig)
                    .routingBuilder()
                    .register(ociMetricsSupport);
            if (Metrics.isActive()) {
                LOGGER.log(Level.WARNING, "OCI metrics system is already initialized; unable to share the telemetry reporter");
            } else {
                TelemetryReporter reporter = new TelemetryReporterBuilder()
                        .monitoringClient(monitoring)
                        .project(project)
                        .fleet(fleet)
                        .region(instanceInfo.canonicalRegionName())
                        .hostname(instanceInfo.displayName())
                        .availabilityDomain(instanceInfo.ociAdName())
                        .faultDomain(instanceInfo.faultDomain())
                        .build();
                String hostName;
                try {
                    hostName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    hostName = "HelidonOCITestHost";
                }

                Metrics.init(reporter, Map.of("host", hostName));
            }
        }
    }
}

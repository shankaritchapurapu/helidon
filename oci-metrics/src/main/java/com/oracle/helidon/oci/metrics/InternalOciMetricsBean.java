/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oracle.helidon.oci.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.json.JsonObject;

import io.helidon.common.Errors;
import io.helidon.config.Config;
import io.helidon.integrations.oci.metrics.OciMetricsSupport;
import io.helidon.integrations.oci.metrics.cdi.OciMetricsBean;

import com.oracle.bmc.monitoring.Monitoring;
import com.oracle.pic.telemetry.commons.metrics.Metrics;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporter;
import com.oracle.pic.telemetry.commons.metrics.TelemetryReporterBuilder;

/**
 * Internal OCI metrics implementation of the integration set-up bean.
 * <p>
 * This bean expects internal OCI metrics configuration under {@code oci.metrics} with {@code project} and {@code fleet}
 * under {@code oci}.
 */
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
@Alternative
@Singleton
class InternalOciMetricsBean extends OciMetricsBean {

    // Instance metadata key names
    private static final String DISPLAY_NAME = "displayName";
    private static final String CANONICAL_REGION_NAME = "canonicalRegionName";
    private static final String OCI_AD_NAME = "ociAdName";
    private static final String FAULT_DOMAIN = "faultDomain";
    private static final String REGION = "region";
    private static final Logger LOGGER = Logger.getLogger(InternalOciMetricsBean.class.getName());

    private Monitoring monitoring;
    private String project;
    private String fleet;
    private JsonObject instanceMetadata;

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

        instanceMetadata = CDI.current().select(InstanceMetadataLoader.class).get().instanceMetadata();

        this.monitoring = monitoring;

        OciMetricsSupport.Builder result = super.ociMetricsSupportBuilder(rootConfig, ociMetricsConfig, monitoring);
        if (result.enabled()) {

            Errors.Collector collector = Errors.collector();

            // Use synonyms in config--project for namespace and fleet for resourceGroup.
            // Abort if either is missing.
            ociMetricsConfig().get("project").asString().ifPresentOrElse(project -> {
                                                                             result.namespace(project);
                                                                             this.project = project;
                                                                         },
                                                                         () -> collector.fatal(
                                                                                 "required OCI metrics config setting for project "
                                                                                         + "is missing"));

            ociMetricsConfig().get("fleet").asString().ifPresentOrElse(fleet -> {
                                                                           result.resourceGroup(fleet);
                                                                           this.fleet = fleet;
                                                                       },
                                                                       () -> collector.fatal(
                                                                               "required OCi metrics config setting for fleet is "
                                                                                       + "missing"));
            result.compartmentId(MetricsCompartmentHelper.t2CompartmentIdForRegion(instanceMetadata.getString(REGION)));

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
            if (Metrics.isActive()) {
                LOGGER.log(Level.WARNING, "OCI metrics system is already initialized; unable to share the telemetry reporter");
            } else {
                TelemetryReporter reporter = new TelemetryReporterBuilder()
                        .monitoringClient(monitoring)
                        .project(project)
                        .fleet(fleet)
                        .region(instanceMetadata.getString(CANONICAL_REGION_NAME))
                        .hostname(instanceMetadata.getString(DISPLAY_NAME))
                        .availabilityDomain(instanceMetadata.getString(OCI_AD_NAME))
                        .faultDomain(instanceMetadata.getString(FAULT_DOMAIN))
                        .build();
                String hostName;
                try {
                    hostName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    hostName = "HelidonOCITestHost";
                }

                Metrics.init(reporter, Map.of("host", hostName));
            }
            super.activateOciMetricsSupport(rootConfig, ociMetricsConfig, builder);
        }
    }
}
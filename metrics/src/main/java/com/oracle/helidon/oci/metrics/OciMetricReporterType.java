/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

/**
 * Configured OCI metrics reporter type.
 */
public enum OciMetricReporterType {
    /**
     * Overlay reporter using {@code TelemetryReporterBuilder}.
     */
    OVERLAY("overlay"),

    /**
     * Substrate reporter using {@code DianogaReporter.Builder}.
     */
    SUBSTRATE("substrate");

    private final String configKey;

    OciMetricReporterType(String configKey) {
        this.configKey = configKey;
    }

    /**
     * Config key used to select this reporter type.
     *
     * @return config key
     */
    public String configKey() {
        return configKey;
    }

    /**
     * Resolves a configured reporter type value.
     *
     * @param configValue configured value
     * @return matching reporter type
     */
    public static OciMetricReporterType fromConfig(String configValue) {
        for (OciMetricReporterType type : values()) {
            if (type.configKey.equals(configValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported OCI metrics reporter type: " + configValue
                                                   + ". Expected one of: "
                                                   + OVERLAY.configKey + ", "
                                                   + SUBSTRATE.configKey);
    }
}

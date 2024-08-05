/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.metrics;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * OciMetricsCdiExtension class provides integration with OCI internal metrics service.
 */
public class OciMetricsCdiExtension implements Extension {

    /**
     * Reserved for CDI.
     */
    @Deprecated
    public OciMetricsCdiExtension() {
    }

    protected void addAnnotatedTypes(@Observes BeforeBeanDiscovery bbd) {
        bbd.addAnnotatedType(InternalOciMetricsBean.class, InternalOciMetricsBean.class.getName());
    }
}

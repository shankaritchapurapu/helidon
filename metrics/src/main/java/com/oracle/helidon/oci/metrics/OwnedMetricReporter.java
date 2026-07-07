/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;

/**
 * Reporter wrapper for clients created by this integration.
 * <p>
 * This wrapper records that the associated client is owned by the integration and should be closed at shutdown, unlike
 * clients supplied programmatically by application code.
 */
final class OwnedMetricReporter implements MetricReporter, AutoCloseable {

    private final MetricReporter delegate;
    private final AutoCloseable closeable;

    private OwnedMetricReporter(MetricReporter delegate, AutoCloseable closeable) {
        this.delegate = delegate;
        this.closeable = closeable;
    }

    static MetricReporter create(MetricReporter delegate, AutoCloseable closeable) {
        return new OwnedMetricReporter(delegate, closeable);
    }

    @Override
    public void send(List<TimeSeries> timeSeries) {
        delegate.send(timeSeries);
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void close() throws Exception {
        closeable.close();
    }
}

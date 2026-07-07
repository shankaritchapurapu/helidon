/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.function.Supplier;

import com.oracle.pic.telemetry.commons.metrics.MetricReporter;
import com.oracle.pic.telemetry.commons.metrics.model.TimeSeries;

/**
 * Defers reporter creation until the OCI telemetry runtime initializes.
 * <p>
 * Building the concrete reporter may resolve services or create OCI clients, so config mapping keeps only this
 * lightweight wrapper and lets runtime initialization perform the side-effecting work.
 */
final class DeferredMetricReporter implements MetricReporter, AutoCloseable {

    private final Supplier<MetricReporter> reporterSupplier;
    private volatile MetricReporter delegate;

    private DeferredMetricReporter(Supplier<MetricReporter> reporterSupplier) {
        this.reporterSupplier = reporterSupplier;
    }

    static MetricReporter create(Supplier<MetricReporter> reporterSupplier) {
        return new DeferredMetricReporter(reporterSupplier);
    }

    MetricReporter initialize() {
        return delegate();
    }

    @Override
    public void send(List<TimeSeries> timeSeries) {
        delegate().send(timeSeries);
    }

    @Override
    public void stop() {
        MetricReporter reporter = delegate;
        if (reporter != null) {
            reporter.stop();
        }
    }

    @Override
    public void close() throws Exception {
        MetricReporter reporter = delegate;
        if (reporter instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    private MetricReporter delegate() {
        MetricReporter reporter = delegate;
        if (reporter == null) {
            synchronized (this) {
                reporter = delegate;
                if (reporter == null) {
                    reporter = reporterSupplier.get();
                    delegate = reporter;
                }
            }
        }
        return reporter;
    }
}

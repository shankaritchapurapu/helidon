/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.spi.MetersProvider;
import io.helidon.service.registry.Service;

import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Provider for built-in JVM gauges for the OCI metrics provider.
 */
@Service.Singleton
public class JvmMetersProvider implements MetersProvider {

    @Override
    public Collection<Meter.Builder<?, ?>> meterBuilders(MetricsFactory metricsFactory) {
        List<Meter.Builder<?, ?>> result = new ArrayList<>();
        JvmMetersConfig jvmMetersConfig = jvmMetersConfig(metricsFactory);
        if (jvmMetersConfig.memoryUsageEnabled()) {
            addMemoryUsageGauges(metricsFactory, result);
        }
        if (jvmMetersConfig.threadStateEnabled()) {
            addThreadStateGauges(metricsFactory, result);
        }
        if (jvmMetersConfig.fileDescriptorEnabled()) {
            addFileDescriptorGauges(metricsFactory, result);
        }
        if (jvmMetersConfig.gcEnabled()) {
            addGcGauges(metricsFactory, result);
        }
        return List.copyOf(result);
    }

    private static void addMemoryUsageGauges(MetricsFactory metricsFactory, List<Meter.Builder<?, ?>> builders) {
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        builders.add(gauge(metricsFactory,
                           "jvm.memory.used",
                           () -> memoryMxBean.getHeapMemoryUsage().getUsed(),
                           Tag.create("area", "heap"),
                           Tag.create("unit", "bytes")));
        builders.add(gauge(metricsFactory,
                           "jvm.memory.used",
                           () -> memoryMxBean.getNonHeapMemoryUsage().getUsed(),
                           Tag.create("area", "non-heap"),
                           Tag.create("unit", "bytes")));
    }

    private static void addThreadStateGauges(MetricsFactory metricsFactory, List<Meter.Builder<?, ?>> builders) {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        for (Thread.State state : Thread.State.values()) {
            builders.add(gauge(metricsFactory,
                               "jvm.thread.state",
                               () -> countThreadsInState(threadMxBean, state),
                               Tag.create("state", state.name().toLowerCase(Locale.ROOT))));
        }
    }

    private static void addFileDescriptorGauges(MetricsFactory metricsFactory, List<Meter.Builder<?, ?>> builders) {
        java.lang.management.OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        if (osMxBean instanceof UnixOperatingSystemMXBean unixMxBean) {
            builders.add(gauge(metricsFactory,
                               "jvm.file.descriptor.used",
                               unixMxBean::getOpenFileDescriptorCount));
            builders.add(gauge(metricsFactory,
                               "jvm.file.descriptor.max",
                               unixMxBean::getMaxFileDescriptorCount));
        }
    }

    private static void addGcGauges(MetricsFactory metricsFactory, List<Meter.Builder<?, ?>> builders) {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            builders.add(gauge(metricsFactory,
                               "jvm.gc.count",
                               () -> Math.max(0L, gcBean.getCollectionCount()),
                               Tag.create("name", gcBean.getName())));
            builders.add(gauge(metricsFactory,
                               "jvm.gc.time",
                               () -> Math.max(0L, gcBean.getCollectionTime()),
                               Tag.create("name", gcBean.getName()),
                               Tag.create("unit", "milliseconds")));
        }
    }

    private static Gauge.Builder<Long> gauge(MetricsFactory metricsFactory,
                                             String name,
                                             Supplier<Long> supplier,
                                             Tag... tags) {
        Gauge.Builder<Long> builder = metricsFactory.gaugeBuilder(name, supplier);
        for (Tag tag : tags) {
            builder.addTag(tag);
        }
        return builder;
    }

    private static long countThreadsInState(ThreadMXBean threadMxBean, Thread.State state) {
        long count = 0L;
        for (long threadId : threadMxBean.getAllThreadIds()) {
            Thread.State threadState = threadMxBean.getThreadInfo(threadId) == null
                    ? null
                    : threadMxBean.getThreadInfo(threadId).getThreadState();
            if (threadState == state) {
                count++;
            }
        }
        return count;
    }

    private static java.util.Optional<OciMetricsPublisherConfig> publisherConfig(MetricsFactory metricsFactory) {
        return metricsFactory instanceof OciMetricsFactory ociMetricsFactory
                ? java.util.Optional.of(ociMetricsFactory.publisherConfig())
                : java.util.Optional.empty();
    }

    private static JvmMetersConfig jvmMetersConfig(MetricsFactory metricsFactory) {
        return publisherConfig(metricsFactory)
                .flatMap(OciMetricsPublisherConfig::jvmMeters)
                .orElseGet(JvmMetersConfig::create);
    }
}

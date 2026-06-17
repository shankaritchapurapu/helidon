/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.spi.MetersProvider;
import io.helidon.service.registry.Service;

import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Provider for built-in JVM gauges for the OCI metrics provider.
 * Inspired by dropwizard-metrics/metrics-jvm/src/main/java/com/codahale/metrics/jvm/.
 */
@Service.Singleton
public class JvmMetersProvider implements MetersProvider {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final int THREAD_STACK_TRACE_DEPTH = 0;

    @Override
    public Collection<Meter.Builder<?, ?>> meterBuilders(MetricsFactory metricsFactory) {
        List<Meter.Builder<?, ?>> result = new ArrayList<>();
        String metricsScopeName = metricsScopeName(metricsFactory);
        addMemoryUsageGauges(metricsFactory, result, metricsScopeName);
        addGcGauges(metricsFactory, result, metricsScopeName);
        addThreadStateGauges(metricsFactory, result, metricsScopeName);
        addClassLoadingGauges(metricsFactory, result, metricsScopeName);
        addBufferPoolGauges(metricsFactory, result, metricsScopeName);
        addFileDescriptorGauge(metricsFactory, result, metricsScopeName);
        addJvmAttributeGauges(metricsFactory, result, metricsScopeName);
        return List.copyOf(result);
    }

    private static void addMemoryUsageGauges(MetricsFactory metricsFactory,
                                             List<Meter.Builder<?, ?>> builders,
                                             String metricsScopeName) {
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        String jvm = name(metricsScopeName, "jvm");

        builders.add(longGauge(metricsFactory,
                               name(jvm, "memory.total.init"),
                               () -> memoryMxBean.getHeapMemoryUsage().getInit()
                                       + memoryMxBean.getNonHeapMemoryUsage().getInit()));
        builders.add(longGauge(metricsFactory,
                               name(jvm, "memory.total.used"),
                               () -> memoryMxBean.getHeapMemoryUsage().getUsed()
                                       + memoryMxBean.getNonHeapMemoryUsage().getUsed()));
        builders.add(longGauge(metricsFactory,
                               name(jvm, "memory.total.max"),
                               () -> combinedMax(memoryMxBean.getHeapMemoryUsage(),
                                                 memoryMxBean.getNonHeapMemoryUsage())));
        builders.add(longGauge(metricsFactory,
                               name(jvm, "memory.total.committed"),
                               () -> memoryMxBean.getHeapMemoryUsage().getCommitted()
                                       + memoryMxBean.getNonHeapMemoryUsage().getCommitted()));

        addMemoryUsage(builders, metricsFactory, name(jvm, "memory.heap"), memoryMxBean::getHeapMemoryUsage);
        addMemoryUsage(builders, metricsFactory, name(jvm, "memory.non-heap"), memoryMxBean::getNonHeapMemoryUsage);

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            String poolName = name(jvm, "memory.pools", normalize(pool.getName()));
            addMemoryUsage(builders, metricsFactory, poolName, pool::getUsage);
            if (pool.getCollectionUsage() != null) {
                builders.add(longGauge(metricsFactory,
                                       name(poolName, "used-after-gc"),
                                       () -> pool.getCollectionUsage().getUsed()));
            }
        }
    }

    private static void addGcGauges(MetricsFactory metricsFactory,
                                    List<Meter.Builder<?, ?>> builders,
                                    String metricsScopeName) {
        String jvm = name(metricsScopeName, "jvm");
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String gcName = name(jvm, "gc", normalize(gcBean.getName()));
            builders.add(longGauge(metricsFactory, name(gcName, "count"), gcBean::getCollectionCount));
            builders.add(longGauge(metricsFactory, name(gcName, "time"), gcBean::getCollectionTime));
        }
    }

    private static void addThreadStateGauges(MetricsFactory metricsFactory,
                                             List<Meter.Builder<?, ?>> builders,
                                             String metricsScopeName) {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        String jvm = name(metricsScopeName, "jvm");
        for (Thread.State state : Thread.State.values()) {
            builders.add(intGauge(metricsFactory,
                                  name(jvm, "threads", state.toString().toLowerCase(Locale.ROOT), "count"),
                                  () -> countThreadsInState(threadMxBean, state)));
        }
        builders.add(intGauge(metricsFactory, name(jvm, "threads.count"), threadMxBean::getThreadCount));
        builders.add(intGauge(metricsFactory, name(jvm, "threads.daemon.count"), threadMxBean::getDaemonThreadCount));
        builders.add(intGauge(metricsFactory, name(jvm, "threads.peak.count"), threadMxBean::getPeakThreadCount));
        builders.add(longGauge(metricsFactory,
                               name(jvm, "threads.total_started.count"),
                               threadMxBean::getTotalStartedThreadCount));
        builders.add(intGauge(metricsFactory, name(jvm, "threads.deadlock.count"), () -> deadlockCount(threadMxBean)));
    }

    private static void addClassLoadingGauges(MetricsFactory metricsFactory,
                                              List<Meter.Builder<?, ?>> builders,
                                              String metricsScopeName) {
        ClassLoadingMXBean classLoadingMxBean = ManagementFactory.getClassLoadingMXBean();
        String jvm = name(metricsScopeName, "jvm");
        // Dropwizard's ClassLoadingGaugeSet exposes the cumulative loaded class count as "loaded".
        builders.add(longGauge(metricsFactory, name(jvm, "classes.loaded"), classLoadingMxBean::getTotalLoadedClassCount));
        builders.add(longGauge(metricsFactory, name(jvm, "classes.unloaded"), classLoadingMxBean::getUnloadedClassCount));
    }

    private static void addBufferPoolGauges(MetricsFactory metricsFactory,
                                            List<Meter.Builder<?, ?>> builders,
                                            String metricsScopeName) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        addBufferPoolGauges(metricsFactory, builders, mBeanServer, metricsScopeName, "direct");
        addBufferPoolGauges(metricsFactory, builders, mBeanServer, metricsScopeName, "mapped");
    }

    private static void addBufferPoolGauges(MetricsFactory metricsFactory,
                                            List<Meter.Builder<?, ?>> builders,
                                            MBeanServer mBeanServer,
                                            String metricsScopeName,
                                            String pool) {
        try {
            ObjectName objectName = new ObjectName("java.nio:type=BufferPool,name=" + pool);
            mBeanServer.getMBeanInfo(objectName);
            String prefix = name(metricsScopeName, "jvm.buffers", pool);
            builders.add(longGauge(metricsFactory,
                                   name(prefix, "count"),
                                   () -> jmxLongAttribute(mBeanServer, objectName, "Count")));
            builders.add(longGauge(metricsFactory,
                                   name(prefix, "used"),
                                   () -> jmxLongAttribute(mBeanServer, objectName, "MemoryUsed")));
            builders.add(longGauge(metricsFactory,
                                   name(prefix, "capacity"),
                                   () -> jmxLongAttribute(mBeanServer, objectName, "TotalCapacity")));
        } catch (JMException ignored) {
            // BufferPool MBeans are not available on every runtime.
        }
    }

    private static void addFileDescriptorGauge(MetricsFactory metricsFactory,
                                               List<Meter.Builder<?, ?>> builders,
                                               String metricsScopeName) {
        java.lang.management.OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        if (osMxBean instanceof UnixOperatingSystemMXBean unixMxBean) {
            builders.add(doubleGauge(metricsFactory,
                                     name(metricsScopeName, "jvm.fd.usage"),
                                     () -> fileDescriptorUsage(unixMxBean)));
        }
    }

    private static void addJvmAttributeGauges(MetricsFactory metricsFactory,
                                              List<Meter.Builder<?, ?>> builders,
                                              String metricsScopeName) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        builders.add(longGauge(metricsFactory, name(metricsScopeName, "jvm.attribute.uptime"), runtimeMxBean::getUptime));
    }

    private static void addMemoryUsage(List<Meter.Builder<?, ?>> builders,
                                       MetricsFactory metricsFactory,
                                       String prefix,
                                       Supplier<MemoryUsage> supplier) {
        builders.add(longGauge(metricsFactory, name(prefix, "init"), () -> supplier.get().getInit()));
        builders.add(longGauge(metricsFactory, name(prefix, "used"), () -> supplier.get().getUsed()));
        builders.add(longGauge(metricsFactory, name(prefix, "max"), () -> supplier.get().getMax()));
        builders.add(longGauge(metricsFactory, name(prefix, "committed"), () -> supplier.get().getCommitted()));
        builders.add(doubleGauge(metricsFactory, name(prefix, "usage"), () -> memoryUsageRatio(supplier.get())));
    }

    private static Gauge.Builder<Long> longGauge(MetricsFactory metricsFactory, String name, Supplier<Long> supplier) {
        return metricsFactory.gaugeBuilder(name, supplier);
    }

    private static Gauge.Builder<Integer> intGauge(MetricsFactory metricsFactory, String name, Supplier<Integer> supplier) {
        return metricsFactory.gaugeBuilder(name, supplier);
    }

    private static Gauge.Builder<Double> doubleGauge(MetricsFactory metricsFactory, String name, Supplier<Double> supplier) {
        return metricsFactory.gaugeBuilder(name, supplier);
    }

    private static int countThreadsInState(ThreadMXBean threadMxBean, Thread.State state) {
        int count = 0;
        ThreadInfo[] threadInfos = threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds(), THREAD_STACK_TRACE_DEPTH);
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null && threadInfo.getThreadState() == state) {
                count++;
            }
        }
        return count;
    }

    private static int deadlockCount(ThreadMXBean threadMxBean) {
        long[] deadlockedThreadIds = threadMxBean.findDeadlockedThreads();
        return deadlockedThreadIds == null ? 0 : deadlockedThreadIds.length;
    }

    private static double memoryUsageRatio(MemoryUsage usage) {
        long denominator = usage.getMax() == -1 ? usage.getCommitted() : usage.getMax();
        return ratio(usage.getUsed(), denominator);
    }

    static long combinedMax(MemoryUsage heapUsage, MemoryUsage nonHeapUsage) {
        long heapMax = heapUsage.getMax();
        long nonHeapMax = nonHeapUsage.getMax();
        return heapMax == -1 || nonHeapMax == -1 ? -1 : heapMax + nonHeapMax;
    }

    private static double fileDescriptorUsage(UnixOperatingSystemMXBean osMxBean) {
        return ratio(osMxBean.getOpenFileDescriptorCount(), osMxBean.getMaxFileDescriptorCount());
    }

    private static double ratio(long numerator, long denominator) {
        return denominator <= 0 ? Double.NaN : (double) numerator / denominator;
    }

    private static long jmxLongAttribute(MBeanServer mBeanServer, ObjectName objectName, String attributeName) {
        try {
            return ((Number) mBeanServer.getAttribute(objectName, attributeName)).longValue();
        } catch (JMException e) {
            throw new IllegalStateException("Unable to read JMX attribute " + objectName + "#" + attributeName, e);
        }
    }

    private static String metricsScopeName(MetricsFactory metricsFactory) {
        if (metricsFactory instanceof OciMetricsFactory ociMetricsFactory) {
            return ociMetricsFactory.publisherConfig().metricsScopeName();
        }
        return OciMetricsPublisherConfigSupport.DEFAULT_METRICS_SCOPE_NAME;
    }

    private static String name(String... names) {
        return String.join(".", names);
    }

    private static String normalize(String name) {
        return WHITESPACE.matcher(name).replaceAll("-");
    }
}

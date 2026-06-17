/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.JMException;
import javax.management.ObjectName;

import io.helidon.config.Config;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;

import com.sun.management.UnixOperatingSystemMXBean;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class JvmMetersProviderTest {

    @Test
    void providesDropwizardEquivalentNumericJvmMeters() {
        Set<String> names = meterNames();

        assertThat(names, hasItems("service.jvm.memory.total.init",
                                   "service.jvm.memory.total.used",
                                   "service.jvm.memory.total.max",
                                   "service.jvm.memory.total.committed",
                                   "service.jvm.memory.heap.used",
                                   "service.jvm.memory.heap.usage",
                                   "service.jvm.memory.non-heap.used",
                                   "service.jvm.memory.non-heap.usage",
                                   "service.jvm.threads.count",
                                   "service.jvm.threads.daemon.count",
                                   "service.jvm.threads.peak.count",
                                   "service.jvm.threads.total_started.count",
                                   "service.jvm.threads.deadlock.count",
                                   "service.jvm.classes.loaded",
                                   "service.jvm.classes.unloaded",
                                   "service.jvm.attribute.uptime"));
        assertFileDescriptorGaugeAvailability(names, "service.jvm.fd.usage");
        assertThat(names, hasItem("service.jvm.threads.runnable.count"));
        assertThat(names.stream().anyMatch(name -> name.startsWith("service.jvm.gc.") && name.endsWith(".count")), is(true));
        assertThat(names.stream().anyMatch(name -> name.startsWith("service.jvm.gc.") && name.endsWith(".time")), is(true));
        assertThat(names.stream().anyMatch(name -> name.startsWith("service.jvm.memory.pools.") && name.endsWith(".used")),
                   is(true));
    }

    @Test
    void usesConfiguredMetricsScopeNamePrefix() {
        Set<String> names = meterNames("custom");

        assertThat(names, hasItems("custom.jvm.memory.heap.used",
                                   "custom.jvm.threads.count",
                                   "custom.jvm.classes.loaded",
                                   "custom.jvm.attribute.uptime"));
        assertFileDescriptorGaugeAvailability(names, "custom.jvm.fd.usage");
        assertThat(names.stream().noneMatch(name -> name.startsWith("service.jvm.")), is(true));
    }

    @Test
    void combinesMemoryMaxOnlyWhenHeapAndNonHeapMaxAreDefined() {
        assertThat(JvmMetersProvider.combinedMax(memoryUsage(100), memoryUsage(200)), is(300L));
        assertThat(JvmMetersProvider.combinedMax(memoryUsage(-1), memoryUsage(200)), is(-1L));
        assertThat(JvmMetersProvider.combinedMax(memoryUsage(100), memoryUsage(-1)), is(-1L));
        assertThat(JvmMetersProvider.combinedMax(memoryUsage(-1), memoryUsage(-1)), is(-1L));
    }

    @Test
    void omitsPreviousCustomAndNonnumericDropwizardNames() {
        Set<String> names = meterNames();

        assertThat(Collections.disjoint(names,
                                        Set.of("jvm.memory.used",
                                               "service.jvm.memory.used",
                                               "jvm.thread.state",
                                               "service.jvm.thread.state",
                                               "jvm.file.descriptor.used",
                                               "service.jvm.file.descriptor.used",
                                               "jvm.file.descriptor.max",
                                               "service.jvm.file.descriptor.max",
                                               "jvm.gc.count",
                                               "service.jvm.gc.count",
                                               "jvm.gc.time",
                                               "service.jvm.gc.time",
                                               "jvm.attribute.name",
                                               "service.jvm.attribute.name",
                                               "jvm.attribute.vendor",
                                               "service.jvm.attribute.vendor",
                                               "jvm.threads.deadlocks",
                                               "service.jvm.threads.deadlocks")),
                   is(true));
    }

    @Test
    void providesBufferPoolMetersWhenBufferPoolMBeansAreAvailable() {
        Set<String> names = meterNames();

        if (bufferPoolMBeanExists("direct")) {
            assertThat(names, hasItems("service.jvm.buffers.direct.count",
                                       "service.jvm.buffers.direct.used",
                                       "service.jvm.buffers.direct.capacity"));
        }
        if (bufferPoolMBeanExists("mapped")) {
            assertThat(names, hasItems("service.jvm.buffers.mapped.count",
                                       "service.jvm.buffers.mapped.used",
                                       "service.jvm.buffers.mapped.capacity"));
        }
    }

    @Test
    void reporterFilterCanSuppressJvmMetersByName() {
        OciMetricsPublisherConfig publisherConfig = OciMetricsPublisherConfig.builder()
                .useRegexFilters(true)
                .excludes(Set.of("service\\.jvm\\..*"))
                .buildPrototype();
        OciMetricsPublisher publisher = OciMetricsPublisher.create(publisherConfig);

        assertThat(publisher.shouldPublish(new TestMeter("service.jvm.memory.heap.used")), is(false));
        assertThat(publisher.shouldPublish(new TestMeter("application.requests")), is(true));
    }

    private static Set<String> meterNames() {
        return meterNames("service");
    }

    private static Set<String> meterNames(String metricsScopeName) {
        Collection<Meter.Builder<?, ?>> builders = new JvmMetersProvider().meterBuilders(metricsFactory(metricsScopeName));
        return builders.stream().map(Meter.Builder::name).collect(Collectors.toSet());
    }

    private static MetricsFactory metricsFactory() {
        return metricsFactory("service");
    }

    private static MetricsFactory metricsFactory(String metricsScopeName) {
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .enabled(true)
                .publishersDiscoverServices(false)
                .build();
        return new OciMetricsFactory(new MicrometerMetricsFactoryProvider().create(Config.empty(), metricsConfig, java.util.List.of()),
                                     OciMetricsPublisherConfig.builder()
                                             .enabled(false)
                                             .project("proj")
                                             .fleet("fleet")
                                             .metricsScopeName(metricsScopeName)
                                             .defaultDimensions(Map.of())
                                             .requestHeaders(Map.of())
                                             .buildPrototype(),
                                     metricsConfig,
                                     java.util.List.of());
    }

    private static boolean bufferPoolMBeanExists(String pool) {
        try {
            ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName("java.nio:type=BufferPool,name=" + pool));
            return true;
        } catch (JMException e) {
            return false;
        }
    }

    private static void assertFileDescriptorGaugeAvailability(Set<String> names, String gaugeName) {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof UnixOperatingSystemMXBean) {
            assertThat(names, hasItem(gaugeName));
        } else {
            assertThat(names, not(hasItem(gaugeName)));
        }
    }

    private static MemoryUsage memoryUsage(long max) {
        return new MemoryUsage(0, 0, 0, max);
    }

    private record TestMeter(String name) implements Meter {
        @Override
        public Id id() {
            return new TestId(name);
        }

        @Override
        public java.util.Optional<String> baseUnit() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> description() {
            return java.util.Optional.empty();
        }

        @Override
        public Type type() {
            return Type.GAUGE;
        }

        @Override
        public java.util.Optional<String> scope() {
            return java.util.Optional.empty();
        }

        @Override
        public <R> R unwrap(Class<? extends R> type) {
            if (type.isInstance(this)) {
                return type.cast(this);
            }
            throw new IllegalArgumentException("Unsupported unwrap type: " + type.getName());
        }
    }

    private record TestId(String name) implements Meter.Id {
        @Override
        public Iterable<io.helidon.metrics.api.Tag> tags() {
            return java.util.List.of();
        }
    }
}

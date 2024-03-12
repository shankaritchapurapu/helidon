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
package com.oracle.helidon.oci.identity;

import io.helidon.microprofile.metrics.RegistryFactory;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;

import static jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;

/**
 * Helper for metrics in support of OCI identity integration.
 * <p>
 *     This extension registers metrics so they are present (with zero values) at start-up
 *     before they are updated (which might never happen).
 * </p>
 */
@Deprecated
public class MetricsHelper implements Extension {

    /**
     * Public only for CDI loading.
     */
    @Deprecated
    public MetricsHelper() {
    }

    private static final String METRIC_NAME_PREFIX = "helidon.oci.identity.stream.";
    static final String IN_MEMORY_METRIC_NAME = METRIC_NAME_PREFIX + "in-memory";
    static final String FILE_METRIC_NAME = METRIC_NAME_PREFIX + "file";
    static final String EXCEPTION_METRIC_NAME = METRIC_NAME_PREFIX + "exceptions";


    private Counter repeatableStreamInMemory;
    private Histogram repeatableStreamFileUsage;
    private Counter repeatableStreamExceptions;

    // Priority must exceed that of the MetricsCdiExtension's observer of @Initialized(ApplicationScoped.class).
    void prepare(@Observes @Priority(LIBRARY_BEFORE + 50) @Initialized(ApplicationScoped.class) Object event) {
        MetricRegistry registry = RegistryFactory.getInstance().getRegistry(MetricRegistry.VENDOR_SCOPE);
        repeatableStreamInMemory = registry
                .counter(Metadata.builder()
                                 .withName(IN_MEMORY_METRIC_NAME)
                                 .withDescription("Repeatable stream in-memory uses")
//                                 .withType(MetricType.COUNTER)
                                 .build());

        repeatableStreamFileUsage = registry
                .histogram(Metadata.builder()
                                   .withName(FILE_METRIC_NAME)
                                   .withDescription("Repeatable stream file usage")
//                                   .withType(MetricType.HISTOGRAM)
                                   .withUnit(MetricUnits.KILOBYTES)
                                   .build());

         repeatableStreamExceptions = registry
                .counter(Metadata.builder()
                                 .withName(EXCEPTION_METRIC_NAME)
                                 .withDescription("Repeatable stream exceptions")
//                                 .withType(MetricType.COUNTER)
                                 .build());
    }

    Counter repeatableStreamInMemory() {
        return repeatableStreamInMemory;
    }

    Histogram repeatableStreamFileUsage() {
        return repeatableStreamFileUsage;
    }

    Counter repeatableStreamExceptions() {
        return repeatableStreamExceptions;
    }
}

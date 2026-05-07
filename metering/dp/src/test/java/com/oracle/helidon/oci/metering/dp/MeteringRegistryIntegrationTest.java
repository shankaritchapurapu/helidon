/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.util.List;
import java.util.concurrent.Callable;

import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.bling.clients.ingest.model.Meters;
import com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeteringRegistryIntegrationTest {
    private ServiceRegistryManager registryManager;

    @BeforeEach
    void setUpRegistry() {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());
    }

    @AfterEach
    void shutdownRegistry() {
        registryManager.shutdown();
    }

    @Test
    void usesMockedLogFileUsageRecorderFactoryFromRegistry() {
        LogFileUsageRecorderFactory factory = mock(LogFileUsageRecorderFactory.class);
        LogFileUsageRecorder recorder = mock(LogFileUsageRecorder.class);
        Meters meters = mock(Meters.class);
        Callable<Boolean> expectedResult = () -> true;
        List<Meters> events = List.of(meters);

        when(factory.get()).thenReturn(recorder);
        when(recorder.recordMetersAsync(events)).thenReturn(expectedResult);

        Services.set(LogFileUsageRecorderFactory.class, factory);

        LogFileUsageRecorder resolvedRecorder = Services.get(LogFileUsageRecorder.class);
        Callable<Boolean> result = resolvedRecorder.recordMetersAsync(events);

        assertThat(resolvedRecorder, sameInstance(recorder));
        assertThat(result, sameInstance(expectedResult));
        verify(factory).get();
        verify(recorder).recordMetersAsync(events);
    }
}

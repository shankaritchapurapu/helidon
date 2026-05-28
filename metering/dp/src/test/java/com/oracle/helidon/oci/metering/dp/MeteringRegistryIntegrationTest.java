/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.util.List;
import java.util.concurrent.Callable;

import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.bling.clients.BlingPublisherClient;
import com.oracle.pic.bling.clients.ingest.model.Meters;
import com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder;
import com.oracle.pic.bling.usagereporter.LogFileUsageReporter;
import com.oracle.pic.bling.usagereporter.MeteringReportingAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockConstruction;
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
    @SuppressWarnings("unchecked")
    void registryProvidesNativeLogFileUsageRecorder() throws Exception {
        Callable<Boolean> recorded = mock(Callable.class);
        try (MockedConstruction<LogFileUsageRecorder> recorderConstruction = mockConstruction(
                LogFileUsageRecorder.class,
                (recorder, context) -> when(recorder.recordMetersAsync(anyList())).thenReturn(recorded));
                MockedConstruction<LogFileUsageReporter> ignoredReporterConstruction =
                        mockConstruction(LogFileUsageReporter.class);
                MockedConstruction<MeteringReportingAgent> ignoredAgentConstruction =
                        mockConstruction(MeteringReportingAgent.class)) {
            Services.set(MeteringConfig.class, MeteringConfig.builder()
                    .endpoint("https://bling-dp.example")
                    .region("us-phoenix-1")
                    .blingPublisherClient(mock(BlingPublisherClient.class))
                    .meteringDir("/tmp/helidon-oci-metering-test")
                    .clientId("inventory-dp")
                    .service("inventory")
                    .build());
            Services.set(com.oracle.pic.bling.config.MeteringAgentConfig.class,
                         com.oracle.pic.bling.config.MeteringAgentConfig.builder()
                                 .endpoint("https://bling-dp.example")
                                 .clientId("inventory-dp")
                                 .service("inventory")
                                 .meteringDir("/tmp/helidon-oci-metering-test")
                                 .meteringPeriodInSeconds(1)
                                 .reportToBlingFrequency(1)
                                 .build());

            var recorder = Services.get(LogFileUsageRecorder.class);
            List<Meters> meters = List.of(mock(Meters.class));
            Callable<Boolean> task = recorder.recordMetersAsync(meters);

            ArgumentCaptor<List<Meters>> events = ArgumentCaptor.forClass(List.class);
            verify(recorderConstruction.constructed().getFirst()).recordMetersAsync(events.capture());
            assertThat(events.getValue(), sameInstance(meters));
            assertThat(events.getValue(), hasSize(1));
            assertThat(task, sameInstance(recorded));
        }
    }
}

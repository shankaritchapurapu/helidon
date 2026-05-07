/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Instant;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.bling.clients.ingest.model.Meters;
import com.oracle.pic.bling.emit.client.MeteringClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    void usesMockedMeteringClientFromRegistry() {
        MeteringClient client = mock(MeteringClient.class);
        Meters meters = mock(Meters.class);

        Services.set(MeteringClient.class, client);

        MeteringClient resolvedClient = Services.get(MeteringClient.class);
        resolvedClient.send(meters);

        assertThat(resolvedClient, sameInstance(client));
        verify(client).send(meters);
    }

    @Test
    void meteringRecorderUnwrapsMockedMeteringClient() throws Exception {
        MeteringClient client = mock(MeteringClient.class);

        Services.set(MeteringRecorder.class, new DirectMeteringRecorder("direct", client));

        MeteringRecorder recorder = Services.get(MeteringRecorder.class);
        MeteringClient unwrapped = recorder.unwrap(MeteringClient.class);
        recorder.record(MeteringEvent.builder()
                                .meterName("requests")
                                .compartmentId("ocid1.compartment.oc1..example")
                                .resourceId("resource-1")
                                .from(Instant.EPOCH)
                                .to(Instant.EPOCH.plusSeconds(1))
                                .amount(1.0D)
                                .tags(Map.of("operation", "create"))
                                .build());

        assertThat(unwrapped, sameInstance(client));
        verify(client).send(any(Meters.class));
    }

    @Test
    void factorySelectsDirectRecorderProvider() {
        Services.set(MeteringRecorderProvider.class,
                     new FakeRecorderProvider("agent"),
                     new FakeRecorderProvider("direct"));

        MeteringRecorder recorder = new MeteringRecorderFactory(
                config(Map.ofEntries(
                        Map.entry("oci.metering.direct.enabled", "true")
                )))
                .get();

        assertThat(recorder, instanceOf(FakeRecorder.class));
        assertThat(recorder.type(), is("direct"));
        assertThat(recorder.name(), is("direct"));
    }

    @Test
    void factorySelectsAgentRecorderProvider() {
        Services.set(MeteringRecorderProvider.class,
                     new FakeRecorderProvider("direct"),
                     new FakeRecorderProvider("agent"));

        MeteringRecorder recorder = new MeteringRecorderFactory(
                config(Map.ofEntries(
                        Map.entry("oci.metering.agent.enabled", "true")
                )))
                .get();

        assertThat(recorder, instanceOf(FakeRecorder.class));
        assertThat(recorder.type(), is("agent"));
        assertThat(recorder.name(), is("agent"));
    }

    private static io.helidon.config.Config config(Map<String, String> values) {
        return io.helidon.config.Config.just(io.helidon.config.ConfigSources.create(values));
    }

    private record FakeRecorderProvider(String configKey) implements MeteringRecorderProvider {
        @Override
        public MeteringRecorder create(Config config, String name) {
            return new FakeRecorder(name);
        }
    }

    private record FakeRecorder(String name) implements MeteringRecorder {
        @Override
        public String type() {
            return name;
        }

        @Override
        public void record(MeteringEvent event) {
        }
    }
}

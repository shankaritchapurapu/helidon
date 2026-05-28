/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.bling.emit.MeteringAgent;
import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.kiev.DataStore;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class MeteringRegistryIntegrationTest {
    private ServiceRegistryManager registryManager;

    @AfterEach
    void shutdownRegistry() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void canLookUpMeteringLogStoresFromRegistry() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                Map.entry("oci.metering.endpoint", "https://bling-cp.example"),
                Map.entry("oci.metering.client-id", "cp-client"),
                Map.entry("oci.metering.region", "us-phoenix-1"),
                Map.entry("oci.metering.enabled", "false"),
                Map.entry("oci.metering.host-name", "cp-host"),
                Map.entry("oci.metering.max-workers", "1"),
                Map.entry("oci.metering.metering-period", "PT60S"),
                Map.entry("oci.metering.scan-page-size", "101"),
                Map.entry("oci.metering.retention-period", "PT48H"),
                Map.entry("oci.metering.bucket-configs.0.bucket-name", "archive-bucket"),
                Map.entry("oci.metering.bucket-configs.0.service-name", "archive-service"),
                Map.entry("oci.metering.bucket-configs.0.meter-name", "archive-meter")
        )));
        MappedDataStore mappedDataStore = new MappedDataStore(dataStore());
        MeteringLogStores logStores = mock(MeteringLogStores.class);
        AtomicReference<List<?>> agentArguments = new AtomicReference<>();

        try (MockedConstruction<MeteringAgent> agentConstruction =
                     mockConstruction(MeteringAgent.class, (agent, context) -> {
                         agentArguments.set(context.arguments());
                         when(agent.getMeteringLogStores()).thenReturn(logStores);
                     })) {
            registryManager = ServiceRegistryManager.create(ServiceRegistryConfig.builder()
                                                                     .discoverServices(true)
                                                                     .putContractInstance(Config.class, config)
                                                                     .putContractInstance(MappedDataStore.class,
                                                                                          mappedDataStore)
                                                                     .putContractInstance(Region.class,
                                                                                          Region.fromPublicRegionName(
                                                                                                  "us-ashburn-1"))
                                                                     .build());
            GlobalServiceRegistry.registry(registryManager.registry());

            assertThat(Services.get(MeteringLogStores.class), sameInstance(logStores));
            assertThat(agentConstruction.constructed(), hasSize(1));
            assertThat(agentArguments.get().get(1), sameInstance(mappedDataStore));
            assertThat(agentArguments.get().get(2), is("cp-host"));
            assertThat(agentArguments.get().get(3), is(Region.fromPublicRegionName("us-phoenix-1")));

            var nativeConfig = (com.oracle.pic.bling.emit.config.MeteringAgentConfig) agentArguments.get().getFirst();
            assertThat(nativeConfig.getEndpoint(), is("https://bling-cp.example"));
            assertThat(nativeConfig.getClientId(), is("cp-client"));
            assertThat(nativeConfig.getMaxWorkers(), is(1));
            assertThat(nativeConfig.getMeteringPeriodInSeconds(), is(60));
            assertThat(nativeConfig.getScanPageSize(), is(101));
            assertThat(nativeConfig.getRetentionPeriodDays(), is(2));
            assertThat(nativeConfig.getBucketConfigs(), hasSize(1));
            assertThat(nativeConfig.getBucketConfigs().getFirst().getBucketName(), is("archive-bucket"));
            assertThat(nativeConfig.getBucketConfigs().getFirst().getServiceName(), is("archive-service"));
            assertThat(nativeConfig.getBucketConfigs().getFirst().getMeterName(), is("archive-meter"));
        }
    }

    private static DataStore dataStore() {
        return (DataStore) Proxy.newProxyInstance(DataStore.class.getClassLoader(),
                                                 new Class<?>[] {DataStore.class},
                                                 (proxy, method, args) -> null);
    }
}

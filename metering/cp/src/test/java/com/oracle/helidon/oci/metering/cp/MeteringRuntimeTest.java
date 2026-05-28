/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.config.ConfigException;

import com.oracle.pic.bling.emit.MeteringAgent;
import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.commons.util.Region;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeteringRuntimeTest {
    @Test
    void createsNativeRuntimeAndManagesMeteringAgentLifecycle() {
        AtomicReference<List<?>> agentArguments = new AtomicReference<>();
        MeteringLogStores logStores = mock(MeteringLogStores.class);
        try (MockedConstruction<MeteringAgent> agentConstruction =
                     mockConstruction(MeteringAgent.class, (agent, context) -> {
                         agentArguments.set(context.arguments());
                         when(agent.getMeteringLogStores()).thenReturn(logStores);
            })) {
            MeteringConfig config = config(true);
            com.oracle.pic.bling.emit.config.MeteringAgentConfig nativeConfig = nativeConfig();
            com.oracle.pic.kiev.mapping.MappedDataStore mappedDataStore = null;

            MeteringRuntime runtime = new MeteringRuntime(config,
                                                          nativeConfig,
                                                          mappedDataStore,
                                                          () -> Region.fromPublicRegionName("us-ashburn-1"));

            assertThat(agentConstruction.constructed(), hasSize(1));
            assertThat(agentArguments.get().getFirst(), sameInstance(nativeConfig));
            assertThat(agentArguments.get().get(1), is(mappedDataStore));
            assertThat(agentArguments.get().get(2), is("cp-host"));
            assertThat(agentArguments.get().get(3), is(Region.fromPublicRegionName("us-phoenix-1")));
            assertThat(runtime.meteringLogStores(), sameInstance(logStores));

            runtime.startMeteringAgent();
            runtime.stopMeteringAgent();

            verify(agentConstruction.constructed().getFirst()).start();
            verify(agentConstruction.constructed().getFirst()).stop();
        }
    }

    @Test
    void doesNotStartOrStopNativeAgentWhenDisabled() {
        try (MockedConstruction<MeteringAgent> agentConstruction = mockConstruction(MeteringAgent.class)) {
            MeteringRuntime runtime = new MeteringRuntime(config(false),
                                                          nativeConfig(),
                                                          null,
                                                          () -> Region.fromPublicRegionName("us-ashburn-1"));

            assertThat(agentConstruction.constructed(), hasSize(1));

            runtime.startMeteringAgent();
            runtime.stopMeteringAgent();

            verify(agentConstruction.constructed().getFirst(), never()).start();
            verify(agentConstruction.constructed().getFirst(), never()).stop();
        }
    }

    @Test
    void resolvesConfiguredPublicRegionName() {
        assertThat(regionArgument(Optional.of("us-phoenix-1")),
                   is(Region.fromPublicRegionName("us-phoenix-1")));
    }

    @Test
    void resolvesConfiguredInternalRegionName() {
        assertThat(regionArgument(Optional.of("r2")), is(Region.fromInternalName("r2")));
    }

    @Test
    void resolvesConfiguredAirportCode() {
        assertThat(regionArgument(Optional.of("PHX")), is(Region.fromAirportCode("PHX")));
    }

    @Test
    void resolvesAbsentConfiguredRegionFromSupplier() {
        Region suppliedRegion = Region.fromPublicRegionName("us-ashburn-1");

        assertThat(regionArgument(Optional.empty(), suppliedRegion), is(suppliedRegion));
    }

    @Test
    void rejectsInvalidConfiguredRegion() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                () -> new MeteringRuntime(config(true, Optional.of("not-a-region")),
                                                                          nativeConfig(),
                                                                          null,
                                                                          () -> {
                                                                              throw new AssertionError();
                                                                          }));

        assertThat(exception.getMessage(), containsString("Invalid OCI region: not-a-region"));
    }

    private static MeteringConfig config() {
        return config(true);
    }

    private static MeteringConfig config(boolean enabled) {
        return config(enabled, Optional.of("us-phoenix-1"));
    }

    private static MeteringConfig config(boolean enabled, Optional<String> region) {
        MeteringConfig.Builder builder = MeteringConfig.builder()
                .enabled(enabled)
                .endpoint("https://bling-cp.example")
                .clientId("inventory-cp")
                .hostName("cp-host");
        region.ifPresent(builder::region);
        return builder.build();
    }

    private static com.oracle.pic.bling.emit.config.MeteringAgentConfig nativeConfig() {
        return com.oracle.pic.bling.emit.config.MeteringAgentConfig.builder()
                .endpoint("https://bling-cp.example")
                .clientId("inventory-cp")
                .build();
    }

    private static Region regionArgument(Optional<String> configuredRegion) {
        return regionArgument(configuredRegion, Region.fromPublicRegionName("us-ashburn-1"));
    }

    private static Region regionArgument(Optional<String> configuredRegion, Region suppliedRegion) {
        AtomicReference<List<?>> agentArguments = new AtomicReference<>();
        try (MockedConstruction<MeteringAgent> ignoredAgentConstruction =
                     mockConstruction(MeteringAgent.class, (agent, context) -> agentArguments.set(context.arguments()))) {
            new MeteringRuntime(config(true, configuredRegion), nativeConfig(), null, () -> suppliedRegion);
        }
        return (Region) agentArguments.get().get(3);
    }
}

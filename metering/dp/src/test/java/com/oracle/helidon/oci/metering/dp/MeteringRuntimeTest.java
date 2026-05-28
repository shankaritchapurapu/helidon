/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.config.ConfigException;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.pic.bling.clients.BlingPublisherClient;
import com.oracle.pic.bling.usagereporter.LogFileUsageReporter;
import com.oracle.pic.bling.usagereporter.MeteringReportingAgent;
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

class MeteringRuntimeTest {
    @Test
    void createsNativeRuntimeAndManagesReportingAgentLifecycle() throws Exception {
        AtomicReference<List<?>> reporterArguments = new AtomicReference<>();
        try (MockedConstruction<LogFileUsageReporter> reporterConstruction =
                        mockConstruction(LogFileUsageReporter.class,
                                         (reporter, context) -> reporterArguments.set(context.arguments()));
                MockedConstruction<MeteringReportingAgent> agentConstruction =
                        mockConstruction(MeteringReportingAgent.class)) {
            MeteringConfig config = config(true);
            com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig = nativeConfig();
            ObjectStorageClient objectStorageClient = mock(ObjectStorageClient.class);

            MeteringRuntime runtime = new MeteringRuntime(config,
                                                          nativeConfig,
                                                          objectStorageClient,
                                                          () -> Region.fromPublicRegionName("us-ashburn-1"));

            assertThat(reporterConstruction.constructed(), hasSize(1));
            assertThat(agentConstruction.constructed(), hasSize(1));
            assertThat(reporterArguments.get().getFirst(), sameInstance(nativeConfig));
            assertThat(reporterArguments.get().get(2), sameInstance(objectStorageClient));
            assertThat(reporterArguments.get().get(4), is(Region.fromPublicRegionName("us-phoenix-1")));

            runtime.startReportingAgent();
            runtime.stopReportingAgent();

            verify(agentConstruction.constructed().getFirst()).start();
            verify(agentConstruction.constructed().getFirst()).stop();
        }
    }

    @Test
    void doesNotStartOrStopReportingAgentWhenDisabled() throws Exception {
        try (MockedConstruction<LogFileUsageReporter> ignoredReporterConstruction =
                        mockConstruction(LogFileUsageReporter.class);
                MockedConstruction<MeteringReportingAgent> agentConstruction =
                        mockConstruction(MeteringReportingAgent.class)) {
            MeteringRuntime runtime = new MeteringRuntime(config(false),
                                                          nativeConfig(),
                                                          mock(ObjectStorageClient.class),
                                                          () -> Region.fromPublicRegionName("us-ashburn-1"));

            assertThat(agentConstruction.constructed(), hasSize(1));

            runtime.startReportingAgent();
            runtime.stopReportingAgent();

            verify(agentConstruction.constructed().getFirst(), never()).start();
            verify(agentConstruction.constructed().getFirst(), never()).stop();
        }
    }

    @Test
    void resolvesConfiguredPublicRegionName() throws Exception {
        assertThat(regionArgument(Optional.of("us-phoenix-1")),
                   is(Region.fromPublicRegionName("us-phoenix-1")));
    }

    @Test
    void resolvesConfiguredInternalRegionName() throws Exception {
        assertThat(regionArgument(Optional.of("r2")), is(Region.fromInternalName("r2")));
    }

    @Test
    void resolvesConfiguredAirportCode() throws Exception {
        assertThat(regionArgument(Optional.of("PHX")), is(Region.fromAirportCode("PHX")));
    }

    @Test
    void resolvesAbsentConfiguredRegionFromSupplier() throws Exception {
        Region suppliedRegion = Region.fromPublicRegionName("us-ashburn-1");

        assertThat(regionArgument(Optional.empty(), suppliedRegion), is(suppliedRegion));
    }

    @Test
    void rejectsInvalidConfiguredRegion() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                () -> new MeteringRuntime(config(true, Optional.of("not-a-region")),
                                                                          nativeConfig(),
                                                                          mock(ObjectStorageClient.class),
                                                                          () -> {
                                                                              throw new AssertionError();
                                                                          }));

        assertThat(exception.getMessage(), containsString("Invalid OCI region: not-a-region"));
    }

    static MeteringConfig config() {
        return config(true);
    }

    static MeteringConfig config(boolean enabled) {
        return config(enabled, Optional.of("us-phoenix-1"));
    }

    static MeteringConfig config(boolean enabled, Optional<String> region) {
        MeteringConfig.Builder builder = MeteringConfig.builder()
                .enabled(enabled)
                .endpoint("https://bling-dp.example")
                .blingPublisherClient(mock(BlingPublisherClient.class))
                .meteringDir("/tmp/helidon-oci-metering-test")
                .clientId("inventory-dp")
                .service("inventory")
                .hostName("dp-host");
        region.ifPresent(builder::region);
        return builder.build();
    }

    static com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig() {
        return com.oracle.pic.bling.config.MeteringAgentConfig.builder()
                .endpoint("https://bling-dp.example")
                .clientId("inventory-dp")
                .service("inventory")
                .meteringDir("/tmp/helidon-oci-metering-test")
                .meteringPeriodInSeconds(1)
                .reportToBlingFrequency(1)
                .build();
    }

    private static Region regionArgument(Optional<String> configuredRegion) throws Exception {
        return regionArgument(configuredRegion, Region.fromPublicRegionName("us-ashburn-1"));
    }

    private static Region regionArgument(Optional<String> configuredRegion, Region suppliedRegion) throws Exception {
        AtomicReference<List<?>> reporterArguments = new AtomicReference<>();
        try (MockedConstruction<LogFileUsageReporter> ignoredReporterConstruction =
                     mockConstruction(LogFileUsageReporter.class,
                                      (reporter, context) -> reporterArguments.set(context.arguments()));
                MockedConstruction<MeteringReportingAgent> ignoredAgentConstruction =
                        mockConstruction(MeteringReportingAgent.class)) {
            new MeteringRuntime(config(true, configuredRegion),
                                nativeConfig(),
                                mock(ObjectStorageClient.class),
                                () -> suppliedRegion);
        }
        return (Region) reporterArguments.get().get(4);
    }
}

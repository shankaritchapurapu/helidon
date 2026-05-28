/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.pic.bling.usagereporter.LogFileUsageReporter;
import com.oracle.pic.bling.usagereporter.MeteringReportingAgent;
import com.oracle.pic.commons.util.Region;

/**
 * Native data plane metering runtime.
 */
@Service.Singleton
@Service.RunLevel(Service.RunLevel.STARTUP)
final class MeteringRuntime {
    private final boolean enabled;
    private final MeteringReportingAgent reportingAgent;

    @Service.Inject
    MeteringRuntime(MeteringConfig config,
                    com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig,
                    ObjectStorageClient objectStorageClient,
                    Supplier<Region> regionSupplier) {
        this.enabled = config.enabled();
        this.reportingAgent = new MeteringReportingAgent(logFileUsageReporter(config,
                                                                              nativeConfig,
                                                                              objectStorageClient,
                                                                              RegionSupport.resolve(config.region(),
                                                                                                    regionSupplier)));
    }

    static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Cannot resolve local host name for metering", e);
        }
    }

    @Service.PostConstruct
    void startReportingAgent() {
        if (enabled) {
            reportingAgent.start();
        }
    }

    @Service.PreDestroy
    void stopReportingAgent() {
        if (enabled) {
            reportingAgent.stop();
        }
    }

    private static LogFileUsageReporter logFileUsageReporter(MeteringConfig config,
                                                             com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig,
                                                             ObjectStorageClient objectStorageClient,
                                                             Region region) {
        return new LogFileUsageReporter(nativeConfig,
                                        config.blingPublisherClient(),
                                        objectStorageClient,
                                        config.hostName().orElseGet(MeteringRuntime::hostName),
                                        region);
    }

}

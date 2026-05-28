/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.pic.bling.emit.MeteringAgent;
import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.kiev.mapping.MappedDataStore;

/**
 * Native control plane metering runtime.
 */
@Service.Singleton
@Service.RunLevel(Service.RunLevel.STARTUP)
class MeteringRuntime {
    private final boolean enabled;
    private final MeteringAgent meteringAgent;

    @Service.Inject
    MeteringRuntime(MeteringConfig config,
                    com.oracle.pic.bling.emit.config.MeteringAgentConfig nativeConfig,
                    MappedDataStore mappedDataStore,
                    Supplier<Region> regionSupplier) {
        this.enabled = config.enabled();
        this.meteringAgent = new MeteringAgent(nativeConfig,
                                               mappedDataStore,
                                               config.hostName().orElseGet(MeteringRuntime::hostName),
                                               RegionSupport.resolve(config.region(), regionSupplier));
    }

    static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Cannot resolve local host name for metering", e);
        }
    }

    @Service.PostConstruct
    void startMeteringAgent() {
        if (enabled) {
            meteringAgent.start();
        }
    }

    @Service.PreDestroy
    void stopMeteringAgent() {
        if (enabled) {
            meteringAgent.stop();
        }
    }

    MeteringLogStores meteringLogStores() {
        return meteringAgent.getMeteringLogStores();
    }
}

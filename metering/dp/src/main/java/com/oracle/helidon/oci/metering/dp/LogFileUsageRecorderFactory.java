/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder;

/**
 * Factory that creates the data plane native log-file usage recorder.
 */
@Service.Singleton
class LogFileUsageRecorderFactory implements Supplier<LogFileUsageRecorder> {
    private final MeteringConfig config;
    private final com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig;

    @Service.Inject
    LogFileUsageRecorderFactory(MeteringConfig config,
                                com.oracle.pic.bling.config.MeteringAgentConfig nativeConfig) {
        this.config = config;
        this.nativeConfig = nativeConfig;
    }

    @Override
    public LogFileUsageRecorder get() {
        return new LogFileUsageRecorder(nativeConfig, config.hostName().orElseGet(LogFileUsageRecorderFactory::hostName));
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Cannot resolve local host name for metering", e);
        }
    }
}

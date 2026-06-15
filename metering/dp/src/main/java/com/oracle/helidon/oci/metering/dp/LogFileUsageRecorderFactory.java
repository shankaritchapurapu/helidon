/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.metering.dp;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.pic.bling.config.MeteringAgentConfig;
import com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder;

@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class LogFileUsageRecorderFactory implements Supplier<LogFileUsageRecorder> {

    private final MeteringConfig meteringConfig;
    private final MeteringAgentConfig meteringAgentConfig;

    @Service.Inject
    LogFileUsageRecorderFactory(MeteringConfig meteringConfig, MeteringAgentConfig meteringAgentConfig) {
        this.meteringConfig = meteringConfig;
        this.meteringAgentConfig = meteringAgentConfig;
    }

    @Override
    public LogFileUsageRecorder get() {
        return new LogFileUsageRecorder(meteringAgentConfig,
                                        meteringConfig.hostName().orElseGet(MeteringRuntime::hostName));
    }

}

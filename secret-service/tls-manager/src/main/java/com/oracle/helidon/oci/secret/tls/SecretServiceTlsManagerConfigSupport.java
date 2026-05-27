/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import io.helidon.builder.api.Prototype;
import io.helidon.config.Config;
import io.helidon.scheduling.CronConfig;

final class SecretServiceTlsManagerConfigSupport {
    static final String DEFAULT_RELOAD_CRON = "0 0/30 * * * ? *";

    private SecretServiceTlsManagerConfigSupport() {
    }

    static CronConfig defaultReload() {
        return CronConfig.builder()
                .expression(DEFAULT_RELOAD_CRON)
                .concurrentExecution(false)
                .task(inv -> {
                    // CronConfig requires a task; the manager installs the real reload task when scheduling.
                })
                .buildPrototype();
    }

    @Prototype.ConfigFactoryMethod("reload")
    static CronConfig reload(Config config) {
        // CronConfig requires a task, but configuration should only supply scheduling fields.
        return CronConfig.builder(defaultReload())
                .config(config)
                .concurrentExecution(false)
                .buildPrototype();
    }
}

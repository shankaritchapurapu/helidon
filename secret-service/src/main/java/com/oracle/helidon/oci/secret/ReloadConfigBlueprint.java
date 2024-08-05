/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
@Prototype.Configured
interface ReloadConfigBlueprint {
    /**
     * The schedule for trigger a reload, downloading PKI material from SSv2.
     *
     * @return the schedule for reload
     */
    @Option.Default("*/30 * * * * ? *")
    String cron();

    /**
     * Enable or disable.
     * @return
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration interface for a single whitelist rule.
 */
@Prototype.Blueprint
@Prototype.Configured
interface RuleConfigBlueprint {
    @Option.Configured
    @Option.Default("")
    String resources();

    @Option.Configured
    @Option.Default("")
    String actions();

    @Option.Configured
    @Option.Default("")
    String values();
}

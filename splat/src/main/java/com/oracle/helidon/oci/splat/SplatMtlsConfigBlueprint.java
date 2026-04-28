/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for SPLAT validation of generated Helidon endpoints.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.splat")
interface SplatMtlsConfigBlueprint {

    /**
     * Whether SPLAT mTLS validation is enabled. This defaults to {@code true}.
     *
     * @return {@code true} if enabled; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Whether the SPLAT authorization validation check should be skipped. This defaults to {@code false}.
     *
     * @return {@code true} if skipped; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean skipAuthzValidationCheck();

    /**
     * Whether to reject Cross Region calls. This defaults to {@code false}.
     *
     * @return {@code true} if enabled; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean rejectXRegionCalls();

    /**
     * Overrides the OCI region that is automatically retrieved from the environment (for example, `us-ashburn-1`).
     *
     * @return OCI region name
     */
    @Option.Configured
    Optional<String> region();

}

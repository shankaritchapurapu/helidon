/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for {@link com.oracle.helidon.oci.splat.SplatMtlsFilter}.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.splat")
interface SplatMtlsConfigBlueprint extends Prototype.Factory<SplatMtlsFeature> {

    /**
     * Whether the Splat mTLS Filter support is enabled.  This defaults to {@code true}.
     *
     * @return {@code true} if enabled; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Whether the Splat Authorization validation check is enabled. This defaults to {@code true}.
     *
     * @return {@code true} if enabled; {@code false} otherwise
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
     * Overrides the Oci region that is automatically retrieved from the environment (ex, us-ashburn-1).
     *
     * @return Oci region name
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Webserver socket names the SplatMtlsFilter should be exposed on. If not defined, defaults to
     * the default socket name ({@value io.helidon.webserver.WebServer#DEFAULT_SOCKET_NAME}).
     *
     * @return list of sockets to register SplatMtlsFilter on
     */
    @Option.Configured
    List<String> sockets();
}

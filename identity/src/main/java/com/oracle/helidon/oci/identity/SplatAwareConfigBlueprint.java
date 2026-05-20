/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration blueprint for the Auth SDK SPLAT-aware request filter.
 */
@Prototype.Blueprint
@Prototype.Configured
interface SplatAwareConfigBlueprint {

    /**
     * Port where SPLAT requests are expected to arrive.
     *
     * @return configured SPLAT request port
     */
    @Option.Configured
    @Option.DefaultInt(0)
    int splatRequestPort();

    /**
     * Additional mTLS-enabled ports where SPLAT requests are expected to arrive.
     *
     * @return additional SPLAT request ports
     */
    @Option.Configured
    List<Integer> additionalSplatRequestPorts();

    /**
     * Whether the service-side authorization call should be skipped for SPLAT
     * requests that carry the SPLAT skip-authorization header.
     *
     * @return {@code true} to allow SPLAT-authorized requests to skip service-side authorization
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean skipAuthorizationForSplat();

    /**
     * Whether SPLAT client certificate common name validation should be used to
     * identify SPLAT requests on configured SPLAT ports.
     *
     * @return {@code true} to validate the SPLAT client certificate
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean validateSplatCert();

    /**
     * Whether tag-only request detection should be disabled for SPLAT requests.
     *
     * @return {@code true} to disable tag-only request detection
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean disableTagOnlyRequestCheck();

    /**
     * Whether cross-region SPLAT client certificates should be rejected.
     *
     * @return {@code true} to reject cross-region calls
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean rejectXRegionCalls();

    /**
     * Optional OCI region name used by SPLAT certificate validation. If omitted,
     * the filter factory tries the authentication region, authorization region,
     * and finally the runtime region from the service registry.
     *
     * @return optional OCI region name
     */
    @Option.Configured
    Optional<String> region();
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Helidon Blueprint configuration interface for AuditConfig.
 */
@Prototype.Blueprint
@Prototype.Configured
interface AuditV2ConfigBlueprint extends Prototype.Factory<AuditV2Feature> {

    /**
     * Enables auditing functionality.
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * The configured event source, defaulting to "EventSourceNotConfigured" if not provided.
     */
    @Option.Configured
    @Option.Default("EventSourceNotConfigured")
    String eventSource();

    /**
     * Whether the 'oci-splat-audited' flag in the request headers should be respected.
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean respectSplatAuditedFlag();

    /**
     * Collection of request parameter rules (patterns).
     */
    @Option.Configured
    List<RuleConfig> requestParameterRules();

    /**
     * Collection of request header rules (patterns).
     */
    @Option.Configured
    List<RuleConfig> requestHeaderRules();

    /**
     * Collection of response header rules (patterns).
     */
    @Option.Configured
    List<RuleConfig> responseHeaderRules();
}

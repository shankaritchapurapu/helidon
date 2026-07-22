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
     * Event name to use when application code does not set a more specific audit event name.
     */
    @Option.Configured
    @Option.Default("HttpRequest")
    String eventName();

    /**
     * Tenant OCID to use when application code or identity integration does not set the principal tenant.
     */
    @Option.Configured
    @Option.Default("")
    String tenantId();

    /**
     * Compartment OCID to use when application code does not set a target compartment.
     */
    @Option.Configured
    @Option.Default("")
    String compartmentId();

    /**
     * Resource identifier to use when application code does not set the audited resource id.
     */
    @Option.Configured
    @Option.Default("")
    String resourceId();

    /**
     * Resource name to use when application code does not set the audited resource name.
     */
    @Option.Configured
    @Option.Default("")
    String resourceName();

    /**
     * Whether the 'oci-splat-audited' flag in the request headers should be respected.
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean respectSplatAuditedFlag();

    /**
     * CIDR ranges for proxies whose {@code X-Forwarded-For} header may be used for audit client attribution.
     */
    @Option.Configured
    List<String> trustedProxyCidrs();

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

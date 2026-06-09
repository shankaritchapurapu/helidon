/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.identity.client;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.helidon.oci.sdk.common.ConfigSupport;

/**
 * OCI Java SDK Identity client configuration.
 */
@Prototype.Blueprint
@Prototype.Configured(IdentityClientConfigFactory.OCI_IDENTITY_CLIENT)
@Prototype.CustomMethods(ConfigSupport.ClientConfigurationOptionSupport.class)
interface IdentityClientConfigBlueprint {

    /**
     * OCI SDK client configuration.
     *
     * @return client configuration
     */
    @Option.Configured
    Optional<ClientConfiguration> client();

    /**
     * Explicit Identity endpoint. When configured, this takes precedence over {@link #region()} and
     * {@link #realmSpecificEndpointTemplateEnabled()}.
     *
     * @return endpoint
     */
    @Option.Configured
    Optional<String> endpoint();

    /**
     * OCI region used to derive the Identity endpoint when no explicit endpoint is configured. If omitted, the
     * configured OCI SDK region provider is used.
     *
     * @return region
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Whether to use realm-specific endpoint templates. This setting applies only when no explicit
     * {@link #endpoint()} is configured.
     *
     * @return whether realm-specific endpoint templates are enabled
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean realmSpecificEndpointTemplateEnabled();
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.sdk.common.core;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.Api;

/**
 * Root configuration for reusable dynamic SSL context providers.
 */
@Prototype.Blueprint
@Prototype.Configured("oci")
@Api.Internal
interface DynamicSslProvidersConfigBlueprint {

    /**
     * Named dynamic SSL context provider configurations.
     *
     * @return configured provider entries
     */
    @Option.Configured
    List<DynamicSslProviderConfig> dynamicSslContextProviders();
}

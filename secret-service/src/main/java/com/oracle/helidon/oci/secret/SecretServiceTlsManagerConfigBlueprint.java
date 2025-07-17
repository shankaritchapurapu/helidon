/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.configurable.Resource;

/**
 * Blueprint configuration for {@link com.oracle.helidon.oci.secret.DefaultSecretServiceTlsManager}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface SecretServiceTlsManagerConfigBlueprint extends Prototype.Factory<SecretServiceTlsManager> {

    /**
     * The schedule for trigger a reload, downloading PKI material from SSv2.
     *
     * @return the schedule for reload
     */
    @Option.Configured
    ReloadConfig reload();

    /**
     * The SSv2 path to PKI provided certificate material.
     *
     * @return the secret service path
     */
    @Option.Configured
    PkiConfig pki();

    /**
     * CA trust path.
     *
     * @return path to CA pem file
     */
    @Option.Configured
    Resource trust();

}

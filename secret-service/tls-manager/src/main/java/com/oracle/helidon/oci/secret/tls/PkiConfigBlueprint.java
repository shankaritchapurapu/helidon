/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.configurable.Resource;

/**
 * SSv2 PKI material configuration.
 */
@Prototype.Blueprint(decorator = PkiConfigSupport.class)
@Prototype.Configured
interface PkiConfigBlueprint {
    /**
     * The password used when the private key is encrypted.
     *
     * @return PKI material private key password
     */
    @Option.Configured
    @Option.Confidential
    Optional<char[]> password();

    /**
     * SSv2 path to PKI certificate material JSON.
     *
     * @return SSv2 secret path
     */
    @Option.Configured
    Optional<String> secretPath();

    /**
     * Local or classpath resource with PKI certificate material JSON.
     *
     * @return PKI JSON resource
     */
    @Option.Configured
    Optional<Resource> resource();
}

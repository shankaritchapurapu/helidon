/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.configurable.Resource;

@Prototype.Blueprint(decorator = PkiConfigBlueprint.PkiDecorator.class)
@Prototype.Configured
interface PkiConfigBlueprint {
    /**
     * The password used in case private key is encrypted.
     *
     * @return the PKI material private key password
     */
    @Option.Configured
    @Option.Default("password")
    String password();

    /**
     * The configuration prefix used for secret retrieval.
     * Default is {@code oci.ssv2}.
     *
     * @return the prefix used for secret retrieval over configuration.
     */
    @Option.Configured
    @Option.Default(SecretServiceMpConfigSource.DEFAULT_PREFIX)
    String prefix();

    /**
     * The SSv2 path to PKI provided certificate material.
     *
     * @return the secret service path
     */
    @Option.Configured
    Optional<String> secret();

    /**
     * PKI mTls certificate material in JSON format.
     *
     * @return resource with PKI mTls material JSON
     */
    @Option.Configured
    Optional<Resource> resource();

    class PkiDecorator implements Prototype.BuilderDecorator<PkiConfig.BuilderBase<?, ?>> {
        @Override
        public void decorate(PkiConfig.BuilderBase<?, ?> b) {
            if (b.secret().isPresent() && b.resource().isPresent()) {
                throw new IllegalArgumentException("Only one SSv2 secret or PKI JSON resource can be configured at a time.");
            }
        }
    }
}

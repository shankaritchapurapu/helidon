/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.time.Duration;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for {@link com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig}.
 */
@Prototype.Blueprint
@Prototype.Configured("oci.dynamic-ssl-context-provider")
interface DynamicSslCtxProviderConfigBlueprint {

    @Option.Configured
    Optional<String> leafCertPath();

    @Option.Configured
    Optional<String> leafCertKeyPath();

    @Option.Configured
    Optional<String> leafCertKeyPassphrase();

    @Option.Configured
    Optional<String> intermediateCertPath();

    @Option.Configured
    Optional<String> rootCertPath();

    @Option.Configured
    Optional<Duration> duration();
}

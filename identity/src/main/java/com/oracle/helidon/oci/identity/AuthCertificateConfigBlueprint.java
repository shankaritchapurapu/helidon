/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration blueprint for authentication certificates.
 * <p>
 * This blueprint defines the configuration options required to load and use
 * an authentication certificate, its associated private key, and an optional
 * passphrase protecting that private key.
 * <p>
 * Typical usage is to map external configuration to this blueprint so that
 * the application can locate the certificate and key material at runtime.
 *
 * <p><strong>Configuration options:</strong></p>
 * <ul>
 *     <li>{@link #certificate()} – location of the public certificate
 *     <li>{@link #privateKey()} – optional location of the private key
 *     <li>{@link #passphrase()} – optional passphrase for the private key
 * </ul>
 */
@Prototype.Blueprint
@Prototype.Configured
interface AuthCertificateConfigBlueprint {

    /**
     * Returns the path or identifier of the local resource that contains the
     * public certificate used for authentication.
     * <p>
     * The value is typically resolved from configuration and should point to a
     * readable resource (for example, a file on the filesystem or a classpath
     * resource) containing the certificate in a supported format (such as PEM
     * or DER).
     *
     * @return the configured certificate resource location; never {@code null}
     */
    @Option.Configured
    String certificate();

    /**
     * Returns the path or identifier of the local resource that contains the
     * private key associated with the authentication certificate.
     * <p>
     * The value, when present, is typically resolved from configuration and
     * should point to a readable resource (for example, a file on the
     * filesystem or a classpath resource) containing the private key in a
     * supported format (such as PEM). If no private key resource is
     * configured, an empty {@link Optional} is returned.
     *
     * @return an {@link Optional} containing the configured private key
     *         resource location, or an empty {@link Optional} if none is
     *         configured
     */
    @Option.Configured
    Optional<String> privateKey();

    /**
     * Returns the passphrase used to protect the private key associated with the
     * authentication certificate, if any.
     * <p>
     * The passphrase is typically used to decrypt an encrypted private key stored
     * in the resource referenced by {@link #privateKey()}. If no passphrase is
     * required or configured, this method returns an empty string.
     *
     * @return the configured passphrase for the private key, or an empty string
     *         if no passphrase is required or has been provided
     */
    @Option.Configured
    @Option.Default("")
    String passphrase();
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Blueprint configuration for the Authentication component.
 * <p>
 * This blueprint defines the set of configuration options required to
 * communicate with the authentication service, including service location,
 * tenancy scoping information, certificate handling, and observability
 * integration. Concrete configuration types generated from this blueprint
 * are responsible for supplying values for these options, typically from a
 * configuration source such as {@code application.yaml}.
 * </p>
 *
 * @see AuthCertificateConfigBlueprint
 */
@Prototype.Blueprint
@Prototype.Configured
interface AuthenticationConfigBlueprint {

    /**
     * Returns the URI of the authentication service.
     * <p>
     * This acts as an explicit endpoint override for the underlying Auth SDK.
     * When this option is configured, {@link #region()} must not also be
     * configured because the Auth SDK accepts exactly one endpoint source.
     * If neither this option nor {@link #region()} is configured, the client
     * uses the default region supplied by the OCI environment configuration.
     * </p>
     *
     * @return an {@link Optional} containing the configured authentication
     *         service {@link URI}, or an empty {@code Optional} if the URI
     *         should be inferred from the region
     */
    @Option.Configured
    Optional<URI> serviceUri();

    /**
     * Returns the global business unit (GBU) identifier used for authentication
     * and scoping of requests.
     * <p>
     * This value typically represents the Oracle global business unit under
     * which the calling application or service is operating and may be used
     * for routing, authorization, auditing, or metrics tagging.
     * </p>
     *
     * @return the global business unit identifier; never {@code null}
     */
    @Option.Configured
    String globalBusinessUnit();

    /**
     * Returns the name of the team on whose behalf authentication requests are made.
     * <p>
     * This value is typically used for identification, routing, auditing, and
     * observability (for example, tagging logs or metrics) and should uniquely
     * identify the engineering or service team that owns the calling
     * application.
     * </p>
     *
     * @return the team name; never {@code null} or empty
     */
    @Option.Configured
    String teamName();

    /**
     * Returns the name of the application on whose behalf authentication requests
     * are performed.
     * <p>
     * This value is typically used for identification, routing, auditing, and
     * observability (for example, tagging logs or metrics) and should uniquely
     * identify the calling application or service within the owning team or
     * global business unit.
     * </p>
     *
     * @return the application name; never {@code null} or empty
     */
    @Option.Configured
    String applicationName();

    /**
     * Returns the Oracle Cloud Infrastructure (OCI) region in which
     * authentication requests should be performed.
     * <p>
     * The region is typically specified using its canonical public name
     * (for example, {@code "us-phoenix-1"}, {@code "eu-frankfurt-1"}) and is
     * used to derive the Auth SDK endpoint when {@link #serviceUri()} is not
     * explicitly configured. If this option is omitted, the client falls back
     * to the default region supplied by the OCI environment configuration.
     * </p>
     *
     * @return an {@link Optional} containing the configured authentication
     *         region, or an empty {@code Optional} if an explicit
     *         {@link #serviceUri()} or the default region is used instead
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Indicates whether instance principal certificates should be used to
     * authenticate with the authentication service.
     * <p>
     * When this option is enabled ({@code true}), the client obtains
     * certificates from the Oracle Cloud Infrastructure instance metadata
     * service (IMDS) and uses them as instance principal credentials. In this
     * mode, the {@link #instancePrincipalUri()} option may be used to override
     * the default IMDS endpoint (for example, in test environments).
     * </p>
     * <p>
     * When this option is disabled ({@code false}), instance principal
     * authentication is not used and the configuration must instead supply
     * explicit certificates via {@link #certificates()} and, optionally,
     * {@link #rootCertPath()}.
     * </p>
     *
     * @return {@code true} if instance principal certificates should be used
     *         for authentication (the default), or {@code false} if explicit
     *         certificates are expected instead
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean useInstancePrincipal();

    /**
     * Returns the URI of the Oracle Cloud Infrastructure instance metadata
     * service (IMDS) endpoint used to obtain instance principal certificates.
     * <p>
     * When {@link #useInstancePrincipal()} is {@code true}, this URI, if
     * present, overrides the default IMDS endpoint that would otherwise be
     * used by the client to fetch instance principal certificates. This is
     * primarily intended for non-production or test scenarios, such as when an
     * SSH tunnel or local proxy is used to access IMDS.
     * </p>
     * <p>
     * When the {@code Optional} is empty, the client uses the standard IMDS
     * endpoint configured for the running environment.
     * </p>
     *
     * @return an {@link Optional} containing the IMDS endpoint {@link URI} to
     *         use for retrieving instance principal certificates, or an empty
     *         {@code Optional} to indicate that the default IMDS endpoint
     *         should be used
     */
    @Option.Configured
    Optional<URI> instancePrincipalUri();

    /**
     * Returns the list of explicit certificates to use for authentication when
     * instance principal authentication is disabled.
     * <p>
     * The list may contain one or more certificates, including intermediate and
     * leaf certificates, that together form the certificate chain used by the
     * client when establishing a secure connection to the authentication
     * service. The order of certificates in the list should reflect the
     * intended certificate chain (for example, leaf first, followed by any
     * intermediates).
     * </p>
     * <p>
     * This configuration is ignored when {@link #useInstancePrincipal()} is
     * {@code true}, in which case certificates are obtained from the Oracle
     * Cloud Infrastructure instance metadata service instead of being supplied
     * explicitly.
     * </p>
     *
     * @return a {@link List} of {@link AuthCertificateConfig} instances
     *         representing the certificates to use for authentication; never
     *         {@code null}, but may be empty if no explicit certificates are
     *         configured
     */
    @Option.Configured
    List<AuthCertificateConfig> certificates();

    /**
     * Returns the filesystem path to a trusted root certificate to be used when
     * establishing TLS connections to the authentication service.
     * <p>
     * This option is typically used when instance principal authentication is
     * disabled (see {@link #useInstancePrincipal()}) and the client must rely on
     * explicitly configured certificates and trust material. The path usually
     * points to a PEM-encoded certificate file that represents the root of the
     * certificate chain used to validate the authentication service's TLS
     * certificate.
     * </p>
     * <p>
     * When the {@code Optional} is empty, the client relies on the platform or
     * JVM default trust store for root certificate validation.
     * </p>
     *
     * @return an {@link Optional} containing the filesystem path to the root
     *         certificate to use for TLS verification, or an empty
     *         {@code Optional} if the default trust configuration should be
     *         used instead
     */
    @Option.Configured
    Optional<String> rootCertPath();

    /**
     * Returns the name of the metrics library to be used for emitting
     * authentication-related metrics.
     * <p>
     * When present, this value enables metrics integration and typically
     * identifies the underlying metrics or observability framework in use
     * (for example, {@code "telemetry"} or {@code "commons"}). The exact
     * semantics of the value are determined by the consuming component,
     * which may use it to select an appropriate metrics adapter or
     * implementation.
     * </p>
     * <p>
     * When the {@code Optional} is empty, no metrics library is configured
     * and metrics emission may be disabled or fall back to a default,
     * implementation-specific behavior.
     * </p>
     *
     * @return an {@link Optional} containing the configured metrics library
     *         name, or an empty {@code Optional} if metrics should not be
     *         explicitly enabled
     */
    @Option.Configured
    Optional<String> metricsLib();

}

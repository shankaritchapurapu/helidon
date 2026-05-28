/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration blueprint for service authorization.
 * <p>
 * This blueprint defines the configuration options required to set up
 * and control authorization behavior, including enabling or disabling
 * authorization, selecting the target service and deployment context,
 * configuring the authorization service endpoint, and specifying
 * optional security- and observability-related settings.
 */
@Prototype.Blueprint(decorator = AuthorizationConfigSupport.class)
@Prototype.Configured
interface AuthorizationConfigBlueprint {

    /**
     * Indicates whether the authorization feature is enabled.
     * <p>
     * When this option is not explicitly configured, it defaults to {@code true}.
     *
     * @return {@code true} if authorization is enabled; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * The authorization service URI.
     * <p>
     * When configured, this value is passed directly to the underlying Auth SDK
     * as an explicit authorization endpoint. Additional fields such as
     * {@link #region()} and {@link #physicalAd()} can still be required by the
     * Auth SDK depending on whether the explicit endpoint targets an overlay or
     * a service-enclave deployment. When a non-service-enclave endpoint needs a
     * region and {@link #region()} is omitted, the client uses the default
     * region supplied by the OCI environment configuration.
     * </p>
     *
     * @return authorization URI
     */
    @Option.Configured
    Optional<URI> serviceUri();

    /**
     * Returns the name of the service for which authorization is configured.
     * <p>
     * This value is typically used to associate authorization rules and
     * policies with a specific service or application component.
     *
     * @return the configured service name; never {@code null}
     */
    @Option.Configured
    String serviceName();

    /**
     * Alias for {@link #serviceName()} using the common config name.
     *
     * @return optional service alias
     */
    @Option.Configured
    Optional<String> service();

    /**
     * Returns the optional region identifier associated with this authorization configuration.
     * <p>
     * The region can be used to scope authorization rules to a specific geographic
     * or logical region (for example, {@code "us-ashburn-1"} or {@code "eu-frankfurt-1"}).
     * If no region is configured for non-service-enclave authorization, the client falls back
     * to the default region supplied by the OCI environment configuration.
     *
     * @return an {@link Optional} containing the configured region value, or an empty
     *         {@link Optional} if running in a service enclave or using the default region
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Returns the optional physical availability domain (AD) associated with this
     * authorization configuration.
     * <p>
     * The physical AD can be used to further scope authorization rules within a
     * region to a specific availability domain (for example, {@code "AD-1"} or
     * {@code "phx-ad-1"}). Typically needs to be provided together with an
     * explicit or default region when {@link #serviceEnclave()} is {@code false}.
     *
     * @return an {@link Optional} containing the configured physical availability
     *         domain value, or an empty {@link Optional} if no physical AD is set
     */
    @Option.Configured
    Optional<String> physicalAd();

    /**
     * Returns the optional availability domain associated with this
     * authorization configuration.
     * <p>
     * This value is only used when {@link #serviceEnclave()} is {@code true}
     * and no explicit {@link #serviceUri()} is configured. In that case, the
     * underlying Auth SDK uses the availability domain to derive the
     * service-enclave authorization endpoint. If this option is omitted, the
     * client falls back to the availability domain supplied by the OCI
     * environment configuration.
     * </p>
     *
     * @return an {@link Optional} containing the configured availability
     *         domain name, or an empty {@link Optional} if the default
     *         availability domain is used
     */
    @Option.Configured
    Optional<String> availabilityDomain();

    /**
     * Indicates whether this service is running in an enclave environment.
     * <p>
     * When this option is not explicitly configured, it defaults to {@code false}.
     * A value of {@code true} typically implies that the service is deployed in a
     * more restricted or isolated runtime environment (for example, a secure enclave
     * or hardened execution context), which may influence how authorization policies
     * are evaluated or how external dependencies are accessed.
     *
     * @return {@code true} if the service is configured to run in an enclave
     *         environment; {@code false} otherwise
     */
    @Option.Configured
    @Option.DefaultBoolean(false)
    boolean serviceEnclave();

    /**
     * Returns the optional path to the root certificate used for authorization.
     * <p>
     * When configured, this value typically points to a file system location
     * (for example, a PEM-encoded certificate file) that contains the root
     * certificate used to validate certificates presented to the authorization
     * service. If no value is configured, a default trust configuration may be
     * used instead, depending on the runtime environment and deployment setup.
     *
     * @return an {@link Optional} containing the configured root certificate
     *         path, or an empty {@link Optional} if no explicit root certificate
     *         path is set
     */
    @Option.Configured
    Optional<String> rootCertPath();

    /**
     * Returns the name of the metrics library to be used for emitting
     * authorization-related metrics.
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

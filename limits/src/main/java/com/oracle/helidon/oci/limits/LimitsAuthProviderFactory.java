/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.configurable.Resource;
import io.helidon.common.pki.Keys;
import io.helidon.integrations.oci.HelidonOci;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder;
import com.oracle.bmc.auth.X509CertificateSupplier;
import com.oracle.bmc.auth.internal.S2SConstants;
import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalAuthConfig;
import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalCertificateConfig;

/**
 * Factory for the authentication provider used by the Limits data plane client.
 */
@Service.Singleton
final class LimitsAuthProviderFactory {

    private final LimitsConfig limitsConfig;
    private final OciConfig ociConfig;
    private final Supplier<Optional<BasicAuthenticationDetailsProvider>> globalAuthProvider;

    @Service.Inject
    LimitsAuthProviderFactory(LimitsConfig limitsConfig,
                              OciConfig ociConfig,
                              Supplier<Optional<BasicAuthenticationDetailsProvider>> globalAuthProvider) {
        this.limitsConfig = limitsConfig;
        this.ociConfig = ociConfig;
        this.globalAuthProvider = globalAuthProvider;
    }

    BasicAuthenticationDetailsProvider authProvider() {
        return authProvider(limitsConfig, ociConfig, globalAuthProvider);
    }

    static BasicAuthenticationDetailsProvider authProvider(
            LimitsConfig limitsConfig,
            OciConfig ociConfig,
            Supplier<Optional<BasicAuthenticationDetailsProvider>> authProvider) {

        Optional<LimitsAuthConfig> limitsAuth = limitsConfig.auth();
        if (limitsAuth.isEmpty()) {
            return authProvider.get()
                    .orElseThrow(() -> new IllegalStateException("BasicAuthenticationDetailsProvider must be "
                                                                          + "available when oci.limits.auth is not "
                                                                          + "configured"));
        }

        LimitsAuthConfig auth = limitsAuth.get();
        if (!"service-principal".equals(auth.authenticationMethod().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("oci.limits.auth.authentication-method supports only service-principal");
        }

        ServicePrincipalAuthConfig servicePrincipalConfig = auth.servicePrincipal()
                .orElseGet(ServicePrincipalAuthConfig::create);
        OciConfig effectiveConfig = effectiveOciConfig(ociConfig, servicePrincipalConfig);
        validateExplicitServicePrincipalConfig(effectiveConfig, servicePrincipalConfig);
        return servicePrincipalAuthProvider(effectiveConfig, servicePrincipalConfig)
                .orElseThrow(() -> new IllegalStateException("oci.limits.auth service-principal requires "
                                                                     + "platform-provided S2S configuration to be "
                                                                     + "available"));
    }

    private static void validateExplicitServicePrincipalConfig(OciConfig effectiveConfig,
                                                               ServicePrincipalAuthConfig servicePrincipalConfig) {
        if (servicePrincipalConfig.usePlatformProvided()) {
            return;
        }
        if (effectiveConfig.federationEndpoint().isEmpty()) {
            throw new IllegalStateException("oci.limits.auth.service-principal.federation-endpoint or "
                                                    + "helidon.oci.federation-endpoint must be configured when "
                                                    + "oci.limits.auth.service-principal.use-platform-provided "
                                                    + "is false");
        }
        if (effectiveConfig.tenantId().isEmpty()) {
            throw new IllegalStateException("oci.limits.auth.service-principal.tenant-id or "
                                                    + "helidon.oci.tenant-id must be configured when "
                                                    + "oci.limits.auth.service-principal.use-platform-provided "
                                                    + "is false");
        }
        if (servicePrincipalConfig.certificates().isEmpty()) {
            throw new IllegalStateException("oci.limits.auth.service-principal.certificates must contain at least "
                                                    + "the leaf certificate when use-platform-provided "
                                                    + "is false");
        }
    }

    static OciConfig effectiveOciConfig(OciConfig ociConfig,
                                        ServicePrincipalAuthConfig servicePrincipalConfig) {
        OciConfig.Builder builder = OciConfig.builder(ociConfig);
        ociConfig.region().ifPresent(builder::region);
        servicePrincipalConfig.federationEndpoint().ifPresent(builder::federationEndpoint);
        servicePrincipalConfig.tenantId().ifPresent(builder::tenantId);
        servicePrincipalConfig.imdsBaseUri().ifPresent(builder::imdsBaseUri);
        return builder.build();
    }

    private static Optional<BasicAuthenticationDetailsProvider> servicePrincipalAuthProvider(
            OciConfig config,
            ServicePrincipalAuthConfig servicePrincipalConfig) {

        LimitsS2SAuthenticationDetailsProviderBuilder builder = new LimitsS2SAuthenticationDetailsProviderBuilder();
        config.region()
                .ifPresent(builder::region);
        config.federationEndpoint()
                .map(URI::toString)
                .ifPresent(builder::federationEndpoint);
        config.imdsBaseUri()
                .map(URI::toString)
                .ifPresent(builder::metadataBaseUrl);
        config.tenantId()
                .ifPresent(builder::tenancyId);

        if (servicePrincipalConfig.usePlatformProvided()) {
            if (!HelidonOci.imdsAvailable(config)) {
                return Optional.empty();
            }
            return Optional.of(builder.useInstancePrincipals().build());
        }

        configureExplicitCertificates(servicePrincipalConfig, builder);
        return Optional.of(builder.servicePrincipalPurpose().build());
    }

    private static void configureExplicitCertificates(
            ServicePrincipalAuthConfig servicePrincipalConfig,
            S2SAuthenticationDetailsProviderBuilder builder) {

        List<ServicePrincipalCertificateConfig> certificates = servicePrincipalConfig.certificates();
        builder.leafCertificateSupplier(certificateSupplier(certificates.getFirst(), true));

        if (certificates.size() > 1) {
            Set<X509CertificateSupplier> intermediateSuppliers = new LinkedHashSet<>();
            certificates.stream()
                    .skip(1)
                    .map(certificate -> certificateSupplier(certificate, false))
                    .forEach(intermediateSuppliers::add);
            builder.intermediateCertificateSuppliers(intermediateSuppliers);
        }
    }

    private static X509CertificateSupplier certificateSupplier(ServicePrincipalCertificateConfig config,
                                                              boolean requirePrivateKey) {
        X509Certificate certificate = loadX509Certificate(config);
        Optional<RSAPrivateKey> privateKey = loadRsaPrivateKey(config);
        if (requirePrivateKey && privateKey.isEmpty()) {
            throw new IllegalStateException("The first oci.limits.auth.service-principal certificate entry "
                                                    + "must configure private-key");
        }
        return new ConfiguredX509CertificateSupplier(certificate, privateKey.orElse(null));
    }

    private static X509Certificate loadX509Certificate(ServicePrincipalCertificateConfig config) {
        String certificate = config.certificate();
        try {
            Resource resource = Resource.create(certificate);
            Keys keys = Keys.builder()
                    .pem(pemBuilder -> pemBuilder.certChain(resource))
                    .build();
            return keys.publicCert()
                    .orElseThrow(() -> new IllegalStateException(
                            "Limits service-principal certificate resource '" + certificate
                                    + "' did not contain a public certificate"));
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load Limits service-principal certificate resource '" + certificate + "'", e);
        }
    }

    private static Optional<RSAPrivateKey> loadRsaPrivateKey(ServicePrincipalCertificateConfig config) {
        if (config.privateKey().isEmpty()) {
            return Optional.empty();
        }
        String privateKey = config.privateKey().get();
        try {
            Resource resource = Resource.create(privateKey);
            Keys keys = Keys.builder()
                    .pem(pemBuilder -> {
                        pemBuilder.key(resource);
                        if (!config.passphrase().isEmpty()) {
                            pemBuilder.keyPassphrase(config.passphrase().toCharArray());
                        }
                    })
                    .build();
            PrivateKey key = keys.privateKey()
                    .orElseThrow(() -> new IllegalStateException(
                            "Limits service-principal private-key resource '" + privateKey
                                    + "' did not contain a private key"));
            if (key instanceof RSAPrivateKey rsaPrivateKey) {
                return Optional.of(rsaPrivateKey);
            }
            throw new IllegalStateException("Limits service-principal private-key resource '" + privateKey
                                                    + "' must contain an RSA private key, but contained "
                                                    + key.getAlgorithm());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load Limits service-principal private-key resource '" + privateKey + "'", e);
        }
    }

    private static final class ConfiguredX509CertificateSupplier implements X509CertificateSupplier {
        private final CertificateAndPrivateKeyPair certificateAndPrivateKeyPair;

        private ConfiguredX509CertificateSupplier(X509Certificate certificate, RSAPrivateKey privateKey) {
            this.certificateAndPrivateKeyPair = new CertificateAndPrivateKeyPair(certificate, privateKey);
        }

        @Override
        @Deprecated
        public X509Certificate getCertificate() {
            return certificateAndPrivateKeyPair.getCertificate();
        }

        @Override
        @Deprecated
        public RSAPrivateKey getPrivateKey() {
            return certificateAndPrivateKeyPair.getPrivateKey();
        }

        @Override
        public CertificateAndPrivateKeyPair getCertificateAndKeyPair() {
            return certificateAndPrivateKeyPair;
        }
    }

    private static final class LimitsS2SAuthenticationDetailsProviderBuilder
            extends S2SAuthenticationDetailsProviderBuilder {

        private LimitsS2SAuthenticationDetailsProviderBuilder region(Region region) {
            this.region = region;
            return this;
        }

        private LimitsS2SAuthenticationDetailsProviderBuilder servicePrincipalPurpose() {
            purpose(S2SConstants.SERVICE_PRINCIPAL_PURPOSE);
            return this;
        }
    }
}

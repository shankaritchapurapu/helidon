/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.configurable.Resource;
import io.helidon.common.pki.Keys;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder;
import com.oracle.bmc.auth.X509CertificateSupplier;

/**
 * Service principal builder provider, uses the {@link S2SAuthenticationDetailsProviderBuilder}.
 */
@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class ServicePrincipalBuilderProvider implements Supplier<S2SAuthenticationDetailsProviderBuilder> {
    private static final System.Logger LOGGER = System.getLogger(ServicePrincipalBuilderProvider.class.getName());

    private final OciConfig config;
    private final Supplier<Optional<ServicePrincipalMethodConfig>> servicePrincipalConfig;

    ServicePrincipalBuilderProvider(OciConfig config,
                                    Supplier<Optional<ServicePrincipalMethodConfig>> servicePrincipalConfig) {
        this.config = config;
        this.servicePrincipalConfig = servicePrincipalConfig;
    }

    @Override
    public S2SAuthenticationDetailsProviderBuilder get() {
        ServicePrincipalS2SAuthenticationDetailsProviderBuilder builder = getBuilder();
        Optional<ServicePrincipalMethodConfig> maybeServicePrincipalConfig = servicePrincipalConfig.get();

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

        boolean useInstancePrincipal = useInstancePrincipal(maybeServicePrincipalConfig);
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Configuring service-principal authentication builder; "
                               + "regionConfigured={0}, federationEndpointConfigured={1}, "
                               + "metadataBaseUrlConfigured={2}, tenancyIdConfigured={3}, "
                               + "servicePrincipalConfigPresent={4}, useInstancePrincipal={5}",
                       builder.getRegion() != null,
                       builder.getFederationEndpoint() != null,
                       builder.getMetadataBaseUrl() != null,
                       builder.getTenancyId() != null,
                       maybeServicePrincipalConfig.isPresent(),
                       useInstancePrincipal);
        }

        if (useInstancePrincipal) {
            return builder.useInstancePrincipals();
        }

        return configureExplicitCertificates(maybeServicePrincipalConfig, builder);
    }

    ServicePrincipalS2SAuthenticationDetailsProviderBuilder getBuilder() {
        return new ServicePrincipalS2SAuthenticationDetailsProviderBuilder();
    }

    static boolean useInstancePrincipal(Supplier<Optional<ServicePrincipalMethodConfig>> servicePrincipalConfig) {
        return useInstancePrincipal(servicePrincipalConfig.get());
    }

    static boolean useInstancePrincipal(Optional<ServicePrincipalMethodConfig> servicePrincipalConfig) {
        return servicePrincipalConfig
                .map(ServicePrincipalMethodConfig::useInstancePrincipal)
                .orElse(true);
    }

    private static ServicePrincipalS2SAuthenticationDetailsProviderBuilder configureExplicitCertificates(
            Optional<ServicePrincipalMethodConfig> maybeServicePrincipalConfig,
            ServicePrincipalS2SAuthenticationDetailsProviderBuilder builder) {

        if (builder.getFederationEndpoint() == null) {
            throw new IllegalStateException(
                    "helidon.oci.federation-endpoint must be configured when service-principal "
                            + "use-instance-principal is false");
        }
        if (builder.getTenancyId() == null) {
            throw new IllegalStateException(
                    "helidon.oci.tenant-id must be configured when service-principal use-instance-principal is false");
        }

        ServicePrincipalMethodConfig servicePrincipalConfig = maybeServicePrincipalConfig
                .orElseThrow(() -> new IllegalStateException(
                        "helidon.oci.authentication.service-principal must be configured when service-principal "
                                + "use-instance-principal is false"));
        List<ServicePrincipalCertificateConfig> certificates = servicePrincipalConfig.certificates();
        if (certificates.isEmpty()) {
            throw new IllegalStateException(
                    "helidon.oci.authentication.service-principal.certificates must contain at least the leaf "
                            + "certificate when use-instance-principal is false");
        }

        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Configuring service-principal builder with explicit certificate material; "
                               + "certificateEntries={0}, intermediateCertificateEntries={1}",
                       certificates.size(),
                       Math.max(0, certificates.size() - 1));
        }

        builder.leafCertificateSupplier(certificateSupplier(certificates.getFirst(), true, true));

        if (certificates.size() > 1) {
            Set<X509CertificateSupplier> intermediateSuppliers = new LinkedHashSet<>();
            certificates.stream()
                    .skip(1)
                    .map(certificate -> certificateSupplier(certificate, false, false))
                    .forEach(intermediateSuppliers::add);
            builder.intermediateCertificateSuppliers(intermediateSuppliers);
        }

        return builder.servicePrincipalPurpose();
    }

    private static X509CertificateSupplier certificateSupplier(ServicePrincipalCertificateConfig config,
                                                              boolean requirePrivateKey,
                                                              boolean leafCertificate) {
        X509Certificate certificate = loadX509Certificate(config);
        Optional<RSAPrivateKey> privateKey = loadRsaPrivateKey(config);
        if (requirePrivateKey && privateKey.isEmpty()) {
            throw new IllegalStateException("The first service-principal certificate entry must configure private-key");
        }
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "Loaded service-principal {0} certificate; certificateResource={1}, "
                               + "privateKeyConfigured={2}, privateKeyRequired={3}, privateKeyLoaded={4}",
                       leafCertificate ? "leaf" : "intermediate",
                       config.certificate(),
                       config.privateKey().isPresent(),
                       requirePrivateKey,
                       privateKey.isPresent());
        }
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE,
                       "Loaded service-principal {0} certificate details; subject={1}, issuer={2}, notAfter={3}",
                       leafCertificate ? "leaf" : "intermediate",
                       certificate.getSubjectX500Principal().getName(),
                       certificate.getIssuerX500Principal().getName(),
                       certificate.getNotAfter());
        }
        return new ConfiguredX509CertificateSupplier(certificate, privateKey.orElse(null));
    }

    static X509Certificate loadX509Certificate(ServicePrincipalCertificateConfig config) {
        String certificate = config.certificate();
        try {
            Resource resource = Resource.create(certificate);
            Keys keys = Keys.builder()
                    .pem(pemBuilder -> pemBuilder.certChain(resource))
                    .build();
            return keys.publicCert()
                    .orElseThrow(() -> new IllegalStateException(
                            "Service-principal certificate resource '" + certificate
                                    + "' did not contain a public certificate"));
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load service-principal certificate resource '" + certificate + "'", e);
        }
    }

    static Optional<RSAPrivateKey> loadRsaPrivateKey(ServicePrincipalCertificateConfig config) {
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
                            "Service-principal private-key resource '" + privateKey
                                    + "' did not contain a private key"));
            if (key instanceof RSAPrivateKey rsaPrivateKey) {
                return Optional.of(rsaPrivateKey);
            }
            throw new IllegalStateException("Service-principal private-key resource '" + privateKey
                                                    + "' must contain an RSA private key, but contained "
                                                    + key.getAlgorithm());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load service-principal private-key resource '" + privateKey + "'", e);
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
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.configurable.Resource;
import io.helidon.common.pki.Keys;
import io.helidon.service.registry.Service;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.auth.AuthMetricsFactory;
import com.oracle.pic.identity.authentication.AuthServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.entities.X509FederationRequest;
import com.oracle.pic.identity.authentication.key.X509CertificateAndRsaPrivateKey;
import com.oracle.pic.identity.authentication.supplier.InstancePrincipalCertificateSupplier;

/**
 * A factory class responsible for creating instances of
 * {@link com.oracle.pic.identity.authentication.ServiceAuthenticationClient}.
 */
@Service.Singleton
public class ServiceAuthenticationClientFactory implements Supplier<ServiceAuthenticationClient> {

    private final AuthenticationConfig config;

    ServiceAuthenticationClientFactory(IdentityConfigFactory config) {
        this.config = config.get().authentication();
    }

    @Override
    public ServiceAuthenticationClient get() {
        // set up client builder from config properties
        AuthServiceAuthenticationClient.Builder builder = new AuthServiceAuthenticationClient.Builder()
                .globalBusinessUnit(config.globalBusinessUnit())
                .teamName(config.teamName())
                .applicationName(config.applicationName())
                .purpose(X509FederationRequest.Purpose.SERVICE_PRINCIPAL)
                .region(Region.fromPublicRegionName(config.region()));

        config.rootCertPath().ifPresent(builder::rootCertPath);

        if (config.metricsLib().isPresent()) {
            builder.authMetrics(AuthMetricsFactory.getInstance(config.metricsLib().get()));
        } else {
            builder.withNoAuthMetrics();
        }

        // check if hardcoded keys
        if (config.hardCodedKeySupplier()) {
            builder.certificateSupplier(List::of);        // required
            return builder.build();
        }

        // check to use instance principal
        if (config.useInstancePrincipal()) {
            InstancePrincipalCertificateSupplier supplier = config.instancePrincipalUri().isPresent()
                    ? new InstancePrincipalCertificateSupplier(config.instancePrincipalUri().get().toString())
                    : new InstancePrincipalCertificateSupplier();
            builder.certificateSupplier(supplier);
        } else {
            // use a certificate list from configuration
            builder.certificateSupplier(() -> loadCertificates(config));
        }

        return builder.build();
    }

    static List<X509CertificateAndRsaPrivateKey> loadCertificates(AuthenticationConfig config) {
        List<AuthCertificateConfig> certConfigs = config.certificates();
        return certConfigs.stream()
                .map(certConfig ->
                             new X509CertificateAndRsaPrivateKey(
                                     loadX509Certificate(certConfig),
                                     loadRsaPrivateKey(certConfig)))
                .toList();
    }

    static X509Certificate loadX509Certificate(AuthCertificateConfig config) {
        Resource resource = Resource.create(config.certificate());
        Keys conf = Keys.builder()
                .pem(pemBuilder -> pemBuilder.certChain(resource))
                .build();
        return conf.publicCert().orElseThrow();
    }

    static Optional<RSAPrivateKey> loadRsaPrivateKey(AuthCertificateConfig config) {
        if (config.privateKey().isEmpty()) {
            return Optional.empty();
        }
        Resource resource = Resource.create(config.privateKey().get());
        Keys conf = Keys.builder()
                .pem(pemBuilder -> {
                    pemBuilder.key(resource);
                    if (!config.passphrase().isEmpty()) {
                        pemBuilder.keyPassphrase(config.passphrase().toCharArray());
                    }
                })
                .build();
        return conf.privateKey().map(pk -> (RSAPrivateKey) pk);
    }
}

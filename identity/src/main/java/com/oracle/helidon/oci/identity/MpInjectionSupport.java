/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.identity;

import java.nio.file.Path;
import java.util.Objects;

import io.helidon.security.Subject;

import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.identity.auth.AuthMetricsConstants;
import com.oracle.pic.identity.auth.AuthMetricsFactory;
import com.oracle.pic.identity.authentication.AuthServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.entities.X509FederationRequest;
import com.oracle.pic.identity.authentication.supplier.InstancePrincipalCertificateSupplier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;

import static com.oracle.helidon.oci.identity.OciIdentityConfiguration.AppConfig;
import static com.oracle.helidon.oci.identity.OciIdentityConfiguration.AuthConfig;
import static com.oracle.helidon.oci.identity.OciIdentityConfiguration.appConfig;
import static com.oracle.helidon.oci.identity.OciIdentityConfiguration.authConfig;

/**
 * Provides the bridge from OCI injectable identity types into CDI.
 */
@ApplicationScoped
public class MpInjectionSupport {
    private static final Subject EMPTY_SUBJECT = Subject.builder().build();

    private MpInjectionSupport() {
    }

    /**
     * Creates a {@link com.oracle.pic.identity.authentication.AuthenticatorClient}.
     * If auth is disabled then provide an offline version.
     */
    @Produces
    @Default
    // inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/pegasus-control-plane/browse/test-data/templates/netty-reference-service-template/reference-service/src/main/java/com/oracle/oci/sfw/netty/reference/module/MessageServiceModule.java#224
    static AuthenticatorClient produceAuthenticatorClient() {
        AuthConfig config = authConfig();
        AuthenticatorClient.Builder builder = new AuthenticatorClient.Builder()
                .keyServiceUrl(config.authServiceEndpoint())
                .serviceAuthenticationClient(serviceAuthenticationClient());
        // https://jira.oci.oraclecorp.com/browse/IDDP-7093 -moved to AuthServiceAuthenticationClient
        //        if (config.rootCertFilePath().isPresent()) {
        //            builder.rootCertPath(config.rootCertPath());
        //        }
        if (config.metricsEnabled()) {
            builder.authMetrics(AuthMetricsFactory.getInstance(AuthMetricsConstants.COMMONS_LIB));
        } else {
            builder.withNoAuthMetrics();
        }

        return builder.build();
    }

    /**
     * Creates a {@link com.oracle.pic.identity.authentication.ServiceAuthenticationClient}.
     * If auth is disabled then provide an offline version.
     */
    @Produces
    @Default
    // inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/pegasus-control-plane/browse/test-data/templates/netty-reference-service-template/reference-service/src/main/java/com/oracle/oci/sfw/netty/reference/module/MessageServiceModule.java#181
    static ServiceAuthenticationClient serviceAuthenticationClient() {
        AuthConfig authConfig = authConfig();
        if (!authConfig.authEnabled()) {
            // return offline AuthN client if auth is disabled
            return new PassThruServiceAuthenticationClient();
        }

        // This is used in *overlay* hosts to contact identity, if you are in service enclave, set to null
        Path rootCertFilePath = authConfig.rootCertFilePath().orElse(null);
        DynamicSslContextProviderConfig dynamicSslConfig =
                (rootCertFilePath == null)
                        ? null
                        : new DynamicSslContextProviderConfig(
                                null, null, null, rootCertFilePath.toString());

        AppConfig appConfig = appConfig();
        AuthServiceAuthenticationClient.Builder builder = AuthServiceAuthenticationClient.builder()
                .authServiceEndpoint(Objects.requireNonNull(authConfig.authServiceEndpoint(), "authServiceEndpoint is required")
                                             .toString())
                .certificateSupplier(produceInstancePrincipalCertificateSupplier())
                .expirationBufferInSeconds(authConfig().refreshAuthTokenBeforeSecondsToExpire())
                .purpose(X509FederationRequest.Purpose.SERVICE_PRINCIPAL)
                .trustRootConfig(dynamicSslConfig)
                .globalBusinessUnit(appConfig.globalBusinessUnit())
                .teamName(appConfig.teamName())
                .applicationName(appConfig.name());

        if (authConfig.rootCertFilePath().isPresent()) {
            builder.rootCertPath(authConfig.rootCertPath());
        }

        if (authConfig.metricsEnabled()) {
            builder.authMetrics(AuthMetricsFactory.getInstance(AuthMetricsConstants.COMMONS_LIB));
        } else {
            builder.withNoAuthMetrics();
        }
        return builder.build();
    }

    @Produces
    @Default
    static InstancePrincipalCertificateSupplier produceInstancePrincipalCertificateSupplier() {
        AppConfig generalConfig = appConfig();

        // See https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/pegasus-control-plane/browse/test-data/templates/netty-reference-service-template/reference-service/src/main/java/com/oracle/oci/sfw/netty/reference/module/MessageServiceModule.java#
        // Integrating with Service-to-Service (S2S): https://confluence.oci.oraclecorp.com/x/Jk7lGw
        //        if (configuration.getStage().equals(DEV_STAGE)
        //                && StringUtils.isNotBlank(authConfig.getInstancePrincipalUrl())) {
        //            certificateSupplier =
        //                    new InstancePrincipalCertificateSupplier(authConfig.getInstancePrincipalUrl());
        //        } else {
        //            certificateSupplier = new InstancePrincipalCertificateSupplier();
        //        }

        if (generalConfig.isDevStage()) {
            return new InstancePrincipalCertificateSupplier(authConfig().metadataEndpoint().toString());
        } else {
            return new InstancePrincipalCertificateSupplier();
        }
    }
}

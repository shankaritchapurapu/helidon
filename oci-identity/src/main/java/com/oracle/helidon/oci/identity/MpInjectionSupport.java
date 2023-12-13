/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.identity;

import java.nio.file.Path;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import io.helidon.security.SecurityContext;
import io.helidon.security.Subject;

import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;
import com.oracle.pic.identity.authentication.AuthServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.PrincipalType;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.entities.X509FederationRequest;
import com.oracle.pic.identity.authentication.supplier.InstancePrincipalCertificateSupplier;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequestFactory;

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
        if (config.rootCertFilePath().isPresent()) {
            builder.rootCertPath(config.rootCertPath());
        }
        if (config.metricsEnabled()) {
            // TODO: https://jira.oci.oraclecorp.com/browse/WLMS-807
            // see https://bitbucket.oci.oraclecorp.com/projects/IDENT/repos/authorization-sdk/browse/metrics-implementation
            builder.withNoAuthMetrics();
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
        DynamicSslContextProviderConfig dynamicSslConfig = (rootCertFilePath == null) ? null :
                new DynamicSslContextProviderConfig(
                        null, null, null, rootCertFilePath.toString());

        AppConfig appConfig = appConfig();
        AuthServiceAuthenticationClient.Builder builder = AuthServiceAuthenticationClient.builder()
                .authServiceEndpoint(Objects.requireNonNull(authConfig.authServiceEndpoint(), "authServiceEndpoint is required").toString())
                .certificateSupplier(produceInstancePrincipalCertificateSupplier())
                .expirationBufferInSeconds(authConfig().refreshAuthTokenBeforeSecondsToExpire())
                .purpose(X509FederationRequest.Purpose.SERVICE_PRINCIPAL)
                .trustRootConfig(dynamicSslConfig)
                .globalBusinessUnit(appConfig.globalBusinessUnit())
                .teamName(appConfig.teamName())
                .applicationName(appConfig.name());

        if (authConfig.metricsEnabled()) {
            // TODO: https://jira.oci.oraclecorp.com/browse/WLMS-807
            builder.withNoAuthMetrics();
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

    // Subject.class is final so can't be proxied so can't be @RequestScoped.
    @Produces
    @Default
    @Dependent
    static Subject produceDefaultSubject(SecurityContext sc) {
        // TODO: https://jira.oci.oraclecorp.com/browse/WLMS-786
        return sc.service().orElse(EMPTY_SUBJECT);
    }

    @Produces
    @RequestScoped
    static com.oracle.pic.identity.authentication.Principal produceContextualPrincipal(SecurityContext sc/*,
            InjectionPoint ip*/) {
        // TODO: https://jira.oci.oraclecorp.com/browse/WLMS-786
        return com.oracle.pic.authproxy.AuthProxyAnonymousPrincipal.builder().build();
    }

    @Produces
    @RequestScoped
    static com.oracle.pic.identity.authorization.sdk.AuthorizationRequest produceContextualAuthorizationRequest(SecurityContext sc/*,
            InjectionPoint ip*/) {
        // TODO: https://jira.oci.oraclecorp.com/browse/WLMS-786
        return AuthorizationRequestFactory.serviceRequest(PrincipalType.SERVICE.name(), produceContextualPrincipal(sc));
    }

}

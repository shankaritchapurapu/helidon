/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.List;

import com.oracle.pic.identity.authentication.key.X509CertificateAndRsaPrivateKey;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class LoadCertificatesTest {

    @Test
    void loadCertificates() {
        AuthenticationConfig config = AuthenticationConfig.builder()
                .globalBusinessUnit("bu")
                .applicationName("app")
                .region("region")
                .teamName("team")
                .certificates(List.of(AuthCertificateConfig.builder()
                                              .certificate("serverCert.pem")
                                              .privateKey("serverKey.pem")
                                              .build()))
                .build();
        List<X509CertificateAndRsaPrivateKey> certificates = ServiceAuthenticationClientFactory.loadCertificates(config);
        assertThat(certificates.size(), is(1));
        X509CertificateAndRsaPrivateKey certificate = certificates.getFirst();
        assertThat(certificate.getX509Certificate().getSubjectX500Principal().getName(), is("CN=localhost"));
        assertThat(certificate.getRsaPrivateKey().isPresent(), is(true));
    }
}

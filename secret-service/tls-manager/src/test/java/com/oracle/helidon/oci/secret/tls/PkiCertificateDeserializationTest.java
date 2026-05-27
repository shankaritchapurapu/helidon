/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PkiCertificateDeserializationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void parsesPkiJsonMaterial() throws Exception {
        PkiCertificate certificate = PkiCertificate.newInstance(
                Files.readAllBytes(Path.of("src/test/resources/mtls/client-pki.json")),
                "password".toCharArray());

        assertThat(certificate.getKey(), notNullValue());
        assertThat(certificate.getCert().getSubjectX500Principal().getName(), is("CN=Helidon-Test-Client"));
        assertThat(certificate.getIntermediates().size(), is(1));
        assertThat(certificate.getIntermediates().getFirst().getSubjectX500Principal().getName(), is("CN=Helidon-Test-CA"));
        assertThat(certificate.getLeafCertAsPEM().contains("BEGIN CERTIFICATE"), is(true));
        assertThat(certificate.getKeyAsPEM().contains("BEGIN RSA PRIVATE KEY"), is(true));
    }

    @Test
    void parsesUnencryptedPkiJsonMaterialWithoutPassword() throws Exception {
        PkiCertificate certificate = PkiCertificate.newInstance(
                Files.readAllBytes(Path.of("src/test/resources/mtls/client-pki.json")),
                null);

        assertThat(certificate.getKey(), notNullValue());
    }

    @Test
    void acceptsMissingIntermediates() throws Exception {
        PkiCertificate.PkiCertificateJson material = pkiJson("client-pki.json");
        byte[] json = JSON.writeValueAsBytes(new PkiCertificate.PkiCertificateJson(material.key(),
                                                                                    material.cert(),
                                                                                    List.of()));

        PkiCertificate certificate = PkiCertificate.newInstance(json, "password".toCharArray());

        assertThat(certificate.getKey(), notNullValue());
        assertThat(certificate.getCert().getSubjectX500Principal().getName(), is("CN=Helidon-Test-Client"));
        assertThat(certificate.getIntermediates().isEmpty(), is(true));
    }

    @Test
    void rejectsMismatchedPrivateKeyAndLeafCertificate() throws Exception {
        PkiCertificate.PkiCertificateJson client = pkiJson("client-pki.json");
        PkiCertificate.PkiCertificateJson server = pkiJson("server-pki.json");
        byte[] json = JSON.writeValueAsBytes(new PkiCertificate.PkiCertificateJson(client.key(),
                                                                                    server.cert(),
                                                                                    server.intermediates()));

        assertThrows(IllegalArgumentException.class, () -> PkiCertificate.newInstance(json, "password".toCharArray()));
    }

    private static PkiCertificate.PkiCertificateJson pkiJson(String fileName) throws Exception {
        return JSON.readValue(Files.readAllBytes(Path.of("src/test/resources/mtls/" + fileName)),
                              PkiCertificate.PkiCertificateJson.class);
    }
}

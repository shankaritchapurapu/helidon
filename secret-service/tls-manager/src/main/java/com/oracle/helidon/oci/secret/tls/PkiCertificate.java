/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.pic.commons.crypto.PEMFileRSAPrivateKeySupplier;
import com.oracle.pic.commons.util.CertUtils;

/**
 * PKI service certificate material stored in SSv2.
 */
class PkiCertificate {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RSAPrivateKey key;
    private List<X509Certificate> intermediates;
    private X509Certificate cert;

    /**
     * Creates PKI certificate material from a JSON blob.
     *
     * @param jsonBlob certificate material JSON
     * @param password private key password
     * @return parsed certificate material
     */
    static PkiCertificate newInstance(byte[] jsonBlob, char[] password) {
        Objects.requireNonNull(jsonBlob, "jsonBlob");
        try {
            PkiCertificateJson parsed = OBJECT_MAPPER.readValue(jsonBlob, PkiCertificateJson.class);
            PkiCertificate certificate = new PkiCertificate();
            certificate.setKey(parseKey(parsed.key(), password));
            certificate.setCert(parseCertificate(parsed.cert()));
            certificate.setIntermediates(parsed.intermediates()
                                                 .stream()
                                                 .map(PkiCertificate::parseCertificate)
                                                 .toList());
            certificate.validate();
            return certificate;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse PKI JSON material", e);
        }
    }

    /**
     * Private key.
     *
     * @return private key
     */
    RSAPrivateKey getKey() {
        return key;
    }

    /**
     * Set private key.
     *
     * @param key private key
     */
    void setKey(RSAPrivateKey key) {
        this.key = key;
    }

    /**
     * Intermediate certificates.
     *
     * @return intermediate certificates
     */
    List<X509Certificate> getIntermediates() {
        return intermediates;
    }

    /**
     * Set intermediate certificates.
     *
     * @param intermediates intermediate certificates
     */
    void setIntermediates(List<X509Certificate> intermediates) {
        this.intermediates = intermediates;
    }

    /**
     * Leaf certificate.
     *
     * @return leaf certificate
     */
    X509Certificate getCert() {
        return cert;
    }

    /**
     * Set leaf certificate.
     *
     * @param cert leaf certificate
     */
    void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    /**
     * Leaf certificate as PEM.
     *
     * @return PEM encoded leaf certificate
     */
    String getLeafCertAsPEM() {
        return CertUtils.convertCertificateToPEM(cert);
    }

    /**
     * Private key as PEM.
     *
     * @return PEM encoded private key
     */
    String getKeyAsPEM() {
        return CertUtils.convertRSAPrivateKeyToPEM(key);
    }

    /**
     * Intermediate certificates as PEM.
     *
     * @return PEM encoded intermediate certificates
     */
    String getIntermediatesAsPEM() {
        return CertUtils.convertCertificatesToPEM(intermediates);
    }

    private void validate() {
        Objects.requireNonNull(key, "PKI key is required");
        Objects.requireNonNull(cert, "PKI certificate is required");
        Objects.requireNonNull(intermediates, "PKI intermediates are required");
        validateKeyMatchesCertificate();
    }

    private void validateKeyMatchesCertificate() {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            byte[] probe = "helidon-oci-secret-service-tls-manager".getBytes(StandardCharsets.UTF_8);
            signature.initSign(key);
            signature.update(probe);
            byte[] signed = signature.sign();

            signature.initVerify(cert.getPublicKey());
            signature.update(probe);
            if (!signature.verify(signed)) {
                throw new IllegalArgumentException("PKI private key does not match leaf certificate");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Unable to validate PKI private key against leaf certificate", e);
        }
    }

    private static RSAPrivateKey parseKey(String pem, char[] password) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("PKI key is required");
        }
        byte[] keyBytes = pem.getBytes(StandardCharsets.UTF_8);
        PEMFileRSAPrivateKeySupplier supplier = blank(password)
                ? PEMFileRSAPrivateKeySupplier.newInstance(keyBytes)
                : PEMFileRSAPrivateKeySupplier.newInstance(keyBytes, new String(password));
        return supplier.getKey("key")
                .orElseThrow(() -> new IllegalArgumentException("Unable to parse PKI private key"));
    }

    private static boolean blank(char[] password) {
        if (password == null || password.length == 0) {
            return true;
        }
        for (char ch : password) {
            if (!Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    private static X509Certificate parseCertificate(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("PKI certificate is required");
        }
        try {
            return CertUtils.parsePEM(pem);
        } catch (IOException | CertificateException e) {
            throw new IllegalArgumentException("Unable to parse PKI certificate", e);
        }
    }

    /**
     * PKI JSON payload.
     *
     * @param key private key PEM
     * @param cert leaf certificate PEM
     * @param intermediates intermediate certificate PEM values
     */
    record PkiCertificateJson(String key,
                              String cert,
                              List<String> intermediates) {
        /**
         * Creates a PKI JSON payload.
         *
         * @param key private key PEM
         * @param cert leaf certificate PEM
         * @param intermediates intermediate certificate PEM values
         */
        PkiCertificateJson {
            intermediates = Objects.requireNonNullElse(intermediates, List.of());
        }
    }
}

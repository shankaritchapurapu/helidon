/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.splat;

import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

/**
 * Splat proxy in Overlay presents splat-api-client cert for mTLS when calling downstream.
 * A correct mTLS implementation on downstream must do:
 * - import MC CA into key store (since splat cert is issued by PKI, signed with MC CA key)
 * - verify if client cert CName match splat-api-client... pattern (see @method resolveSplatAPIClientCName)
 * - verify if issuer is internal PKI. This can be done easily by setting truststore (for client cert) to OCI-only CA
 *   - use https connector
 *   - set needClientAuth to true
 *   - set trust store to OCI-only CA. Again, make sure that it's OCI-only CA, not the bundle that includes 3rd party CAs
 *
 * IMPORTANT NOTE: This Helper class is copied from the OCI Splat SDK so it is important to sync this up with any changes made in
 *                 the original code.
 */
final class CertHelper {
    // splat proxy hosts in Overlay use client cert whose CName is splat-api-client.$region.oci.$domain
    static final Pattern SPLAT_API_CLIENT_CNAME_REGEX = Pattern.compile(
            "(splat-client|"
                    + "splat-api-client\\.[a-z0-9\\-]+\\.oci\\."
                    + "((oc(i)?\\.ic)|(oc(i)?)|"
                    + "(oracle((gov|onsr|nsr)?iaas|realm[\\d]+|"
                    + "(cloud|realm|dodrealm)\\.ic|"
                    + "(cloud|realm)\\.smil)))"
                    + "\\.(com|uk|gov|scloud|mil))"
    );
    // Splat proxy hosts in SE use node client certs whose CName is
    //      splat-proxy-se-[0-9]+\.node\.ad[0-9]\.[a-zA-Z0-9]+
    // or
    //      splat-proxy-se-cell-[0-9]+-[0-9]+\.node\.ad[0-9]\.[a-zA-Z0-9]+
    // or
    //      splat-store-[0-9]+\.node\.ad[0-9]\.[a-zA-Z0-9]+
    // or
    //      splat-store-cell-\w+-[0-9]+\.node\.ad[0-9]\.[a-zA-Z0-9]+
    //
    // This is based on our hostclasses, which are owned by Splat team only.
    static final Pattern SPLAT_CLIENT_CERT_SUBJECT_NAME_REGEX = Pattern.compile(
            "(splat-proxy-se|"
                    + "splat-proxy-se-cell-[0-9]+|"
                    + "splat-store|"
                    + "splat-store-cell-\\w+)-[0-9]+\\.node\\.ad[0-9]\\.[a-z0-9\\-]+"
    );
    // Proxy SE hosts use server certs with the same cname as the client certs.
    // This is based on our hostclasses, which are owned by Splat team only.
    static final Pattern SPLAT_SERVER_CERT_SUBJECT_NAME_REGEX = SPLAT_CLIENT_CERT_SUBJECT_NAME_REGEX;

    private CertHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Returns certificate subject name CN part.
     * e.g. CN=Splat-client -> Splat-client
     *      OU=SomeValue, CN=SubjectName -> SubjectName
     * @param certificate certificate
     * @return subject name part, if 'CN=' doesn't exist, it returns an empty string.
     */
    public static String getCName(X509Certificate certificate) {
        return getCName(certificate.getSubjectX500Principal().getName());
    }

    /**
     * Returns certificate subject name CN part.
     * e.g. CN=Splat-client -> Splat-client
     *      OU=SomeValue, CN=SubjectName -> SubjectName
     * @param subject
     * @return subject name part, if 'CN=' doesn't exist, it returns an empty string.
     */
    public static String getCName(String subject) {
        for (String item : subject.split(",")) {
            String trimmed = item.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }

        return "";
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Method;
import io.helidon.webserver.http.ServerRequest;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.platform.splat.sdk.SplatHelper;
import com.oracle.pic.platform.splat.sdk.mtls.CertHelper;

class SplatMtlsRequestValidator {
    private static final HeaderName SKIP_AUTHORIZATION_FOR_SPLAT =
            HeaderNames.create("oci-skip-authorization-for-splat");
    private static final String NO_CLIENT_CERTIFICATE = "No client certificate found";
    private static final String INSECURE_REQUEST = "Splat mTLS validation requires a secure request";
    private static final String CLIENT_CERTIFICATE_NOT_ALLOW_LISTED = "Client cert is not allow-listed";
    private static final String AUTHZ_NOT_ENABLED = "Authz has not been enabled at Splat";

    private final SplatMtlsConfig config;

    SplatMtlsRequestValidator(SplatMtlsConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    Optional<String> validate(ServerRequest request, Region region) {
        if (!request.isSecure()) {
            return Optional.of(INSECURE_REQUEST);
        }

        // Helidon populates this header from the TLS peer certificate when the client presents one.
        Optional<String> commonName = request.headers().first(HeaderNames.X_HELIDON_CN);
        if (commonName.isEmpty()) {
            return Optional.of(NO_CLIENT_CERTIFICATE);
        }

        if (!isAllowListed(commonName.orElseThrow(), region)) {
            return Optional.of(CLIENT_CERTIFICATE_NOT_ALLOW_LISTED);
        }

        if (!isAuthzValidationSkipped(request)) {
            return Optional.of(AUTHZ_NOT_ENABLED);
        }

        return Optional.empty();
    }

    private boolean isAllowListed(String commonName, Region region) {
        return splatApiClientCnamePattern(region).matcher(commonName).matches()
                || CertHelper.SPLAT_CLIENT_CERT_SUBJECT_NAME_REGEX.matcher(commonName).matches();
    }

    private Pattern splatApiClientCnamePattern(Region region) {
        Optional<String> iaasDomainName = region.getRealm().getOciIaasDomainName();
        if (iaasDomainName.isEmpty()) {
            return Pattern.compile("(?:splat-client|splat-api-client)\\.[a-z0-9\\-]+\\..+");
        }

        String domainSuffix;
        if (config.rejectXRegionCalls()) {
            String regionName = region == Region.SEA ? region.getInternalName() : region.getPublicRegionName();
            domainSuffix = domainSuffix(regionName, iaasDomainName.orElseThrow());
        } else {
            domainSuffix = domainSuffix("[a-z0-9\\-]+", iaasDomainName.orElseThrow());
        }
        return Pattern.compile("(?:splat-client|splat-api-client)\\." + domainSuffix);
    }

    private String domainSuffix(String subdomain, String realmDomain) {
        String primary = SplatHelper.getOciIaasDomainRegex(subdomain + "." + realmDomain);
        if (realmDomain.startsWith("oci.")) {
            return primary;
        }
        String ociPrefixed = SplatHelper.getOciIaasDomainRegex(subdomain + ".oci." + realmDomain);
        return "(?:" + primary + "|" + ociPrefixed + ")";
    }

    private boolean isAuthzValidationSkipped(ServerRequest request) {
        if (config.skipAuthzValidationCheck()) {
            return true;
        }
        if (Method.OPTIONS == request.prologue().method()) {
            return true;
        }
        return request.headers()
                .first(SKIP_AUTHORIZATION_FOR_SPLAT)
                .filter("true"::equalsIgnoreCase)
                .isPresent();
    }
}

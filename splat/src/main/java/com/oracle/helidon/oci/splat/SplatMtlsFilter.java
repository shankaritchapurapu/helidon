/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.service.registry.Services;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authentication.ClaimType;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authentication.PrincipalSerializerFactory;

/**
 * This Helidon SE filter is the equivalent of Oci's JaxRS ContainerRequest based SplatMtlsFilter found in the oci splat SDK.
 *
 * IMPORTANT NOTE: This class copied com.oracle.pic.platform.splat.sdk.mtls.CertHelper and some relevant code from
 *                 com.oracle.pic.platform.splat.SplatHelper that are both part of the oci splat SDK. It is important to
 *                 synch this up with any changes made in the original code.
 */
class SplatMtlsFilter implements Filter {
    static final HeaderName PRINCIPAL_OPC_HEADER_NAME = HeaderNames.create(Principal.OPC_HEADER);
    private static final String SKIP_AUTHORIZATION_FOR_SPLAT = "oci-skip-authorization-for-splat";
    static final HeaderName SKIP_AUTHORIZATION_FOR_SPLAT_HEADER_NAME = HeaderNames.create(SKIP_AUTHORIZATION_FOR_SPLAT);
    private static final String SPLAT_SERVICE_PRINCIPAL_NAME = "splat";
    private static final Logger LOGGER = Logger.getLogger(SplatMtlsFilter.class.getName());

    private final SplatMtlsConfig config;
    private final Pattern splatApiClientCnamePattern;

    SplatMtlsFilter(SplatMtlsConfig config) {
        LOGGER.finest(() -> "Setting up SplatMtlsFilter");
        this.config = config;

        String regionId = this.config.region().orElseGet(this::getDefaultRegion);
        Region region = Region.fromPublicRegionName(regionId);
        String iaasDomainName = region.getRealm().getOciIaasDomainName().orElse(null);
        if (iaasDomainName == null) {
            // ociIaasDomain name Will be empty in DEV/DESKTOP/INTEG realms, fall back to use old cert pattern.
            this.splatApiClientCnamePattern = CertHelper.SPLAT_API_CLIENT_CNAME_REGEX;
            LOGGER.info("Ignoring rejectXRegionCalls flag as either region is not provided or region is in dev realms.");
        } else if (this.config.rejectXRegionCalls()) {
            this.splatApiClientCnamePattern = getSplatApiClientCnameRegexRejectXRegionCalls(region);
        } else {
            String splatApiClientCnameRegx = "splat-client|splat-api-client\\.[a-z0-9\\-]+\\."
                    + getOciIaasDomainRegex(iaasDomainName);
            this.splatApiClientCnamePattern = Pattern.compile(splatApiClientCnameRegx);
        }
        LOGGER.info(String.format("SplatMtlsFilter will be invoked on each request in %s", region));
    }

    @Override
    public void filter(FilterChain filterChain, RoutingRequest request, RoutingResponse response) {
        if (splatApiClientCnamePattern == null) {
            // splatMtlsFilter is disabled, so don't do anything
            LOGGER.finest(() -> "splatMtlsFilter is disabled");
            filterChain.proceed();
            return;
        }
        // Only require auth on https requests (this allows developers to easily switch https off)
        if (!"https".equalsIgnoreCase(request.requestedUri().scheme())) {
            LOGGER.finest(() -> "Skip validating client cert on non-https");
            filterChain.proceed();
            return;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            request.headers()
                    .forEach((header) -> header.allValues()
                            .forEach(value -> LOGGER.finest(header.name() + " : " + value)));
        }
        X509Certificate certificate = getClientTlsCertificate(request);
        if (certificate != null) {
            String cname = CertHelper.getCName(certificate);
            // https://confluence.oci.oraclecorp.com/display/PLAT/Splat+mTLS+scenarios
            if (!splatApiClientCnamePattern.matcher(cname).matches()
                    && !CertHelper.SPLAT_CLIENT_CERT_SUBJECT_NAME_REGEX.matcher(cname).matches()) {
                LOGGER.warning("Client cert " + cname + " is not whitelisted");
                response.status(Status.FORBIDDEN_403).send("Client cert is not whitelisted");
                return;
            }

            if (request.prologue().method() != Method.OPTIONS
                    && !"true".equalsIgnoreCase(
                            request.headers().first(SKIP_AUTHORIZATION_FOR_SPLAT_HEADER_NAME).orElse("false"))
                    && !shouldSkipAuthzValidation(request)) {
                LOGGER.warning("Authz has not been enabled at Splat");
                response.status(Status.FORBIDDEN_403).send("Authz has not been enabled at Splat");
                return;
            }
        } else {
            String errorMessage = "No client certificate was found";
            LOGGER.warning(errorMessage);
            response.status(Status.FORBIDDEN_403).send(errorMessage);
            return;
        }
        filterChain.proceed();
    }

    protected X509Certificate getClientTlsCertificate(RoutingRequest req) {
        Certificate[] certificateChain = req.remotePeer().tlsCertificates().orElse(null);
        // Tls certificate is located at the beginning of the chain
        if (certificateChain != null && certificateChain[0] instanceof X509Certificate certificate) {
            return certificate;
        }
        return null;
    }

    protected String getDefaultRegion() {
        return Services.get(com.oracle.bmc.Region.class).getRegionId();
    }

    private boolean shouldSkipAuthzValidation(RoutingRequest request) {
        String principalHeader = request.headers().first(PRINCIPAL_OPC_HEADER_NAME).orElse(null);
        return config.skipAuthzValidationCheck()
                || principalHeader != null && PrincipalSerializerFactory.create().deserialize(principalHeader)
                        .map(this::isAuthnSkippedAtSplat).orElse(false);
    }

    private boolean isAuthnSkippedAtSplat(Principal principal) {
        return SPLAT_SERVICE_PRINCIPAL_NAME.equalsIgnoreCase(principal.getSubjectId())
                && principal.getClaim(ClaimType.SERVICE_NAME).stream()
                        .anyMatch(c -> SPLAT_SERVICE_PRINCIPAL_NAME.equalsIgnoreCase(c.getValue())
                                && SPLAT_SERVICE_PRINCIPAL_NAME.equalsIgnoreCase(c.getIssuer()));
    }

    // Borrowed from com.oracle.pic.platform.splat.SplatHelper
    private static String getOciIaasDomainRegex(String domain) {
        return domain.replaceAll("\\.", "\\\\.");
    }

    // Borrowed from com.oracle.pic.platform.splat.SplatHelper
    private static Pattern getSplatApiClientCnameRegexRejectXRegionCalls(Region region) {
        String regionMask = region == Region.SEA ? region.getInternalName() : region.getPublicRegionName();
        String splatApiClientCnameRegx = "splat-client|splat-api-client\\."
                + getOciIaasDomainRegex(regionMask + "." + region.getRealm().getOciIaasDomainName().orElse("null"));

        return Pattern.compile(splatApiClientCnameRegx);
    }
}

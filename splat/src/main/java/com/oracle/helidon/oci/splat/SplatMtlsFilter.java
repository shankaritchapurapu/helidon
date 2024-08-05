/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.config.mp.MpConfig;
import io.helidon.integrations.oci.ImdsInstanceInfo;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.webserver.http.ServerRequest;

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JakartaServerFilter;
import com.oracle.pic.commons.util.Region;
import com.oracle.pic.platform.splat.sdk.config.SplatMtlsFilterConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Create a new filter that retrieves request certificate from Helidon and forwards it to SplatMtlsFilter.
 */
@ConstrainedTo(RuntimeType.SERVER)
@Provider
public class SplatMtlsFilter implements ContainerRequestFilter {
    static final String SPLAT_MTLS_FILTER_CONFIG_KEY = "oci.splat.mtls-filter-config";
    static final String ENABLED = "enabled";
    static final String SKIP_AUTHZ_VALIDATION_CHECK = "skip-authz-validation-check";
    static final String REJECT_X_REGION_CALLS = "reject-x-region-calls";
    static final String OCI_REGION_CONFIG_KEY = "oci.region";
    private static final String X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";
    private static final Logger LOGGER = Logger.getLogger(SplatMtlsFilter.class.getName());
    private JakartaServerFilter shimmedSplatMtlsFilter;
    @Context
    private ServerRequest req;

    /**
     * Post construct method, initialization procedures.
     */
    @PostConstruct
    public void postConstruct() {
        LOGGER.info("Setting up SplatMtlsFilter");
        Config globalConfig = getGlobalConfig();
        Config helidonSplatMtlsFilterConfig = globalConfig.get(SPLAT_MTLS_FILTER_CONFIG_KEY);
        boolean splatMtlsFilterEnabled = helidonSplatMtlsFilterConfig.get(ENABLED).asBoolean().orElse(true);
        if (splatMtlsFilterEnabled) {
            SplatMtlsFilterConfig splatMtlsFilterConfig = setSplatMtlsFilterConfig(helidonSplatMtlsFilterConfig);
            String region = getRegion(globalConfig);
            createSplatMtlsFilter(region, splatMtlsFilterConfig);
            LOGGER.info(String.format("SplatMtlsFilter will be invoked on each request in %s", region));
        } else {
            LOGGER.info("SplatMtlsFilter is disabled");
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (shimmedSplatMtlsFilter == null) {
            // splatMtlsFilter is disabled, so don't do anything
            LOGGER.info("Skip SplatMtlsFilter");
            return;
        }
        // Only require auth on https requests (this allows developers to easily switch https off)
        if (!"https".equalsIgnoreCase(requestContext.getUriInfo().getBaseUri().getScheme())) {
            LOGGER.info("Skip validating client cert on non-https");
            return;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            requestContext.getHeaders()
                    .forEach((key, values) -> values
                            .forEach(value -> LOGGER.finest("Request Header: " + key + "=" + value)));
        }
        X509Certificate certificate = getClientTlsCertificate();
        if (certificate != null) {
            // SplatMtlsFilter.filter() will only validate the client TLS certificate,
            // so there is no need to pass the whole certificate chain.
            requestContext.setProperty(X509_CERTIFICATE_ATTRIBUTE, new X509Certificate[] {certificate});
            try {
                shimmedSplatMtlsFilter.filter(requestContext);
            } catch (IOException ioe) {
                String errorMessage = "Unable to use filter: " + ioe;
                LOGGER.warning(errorMessage);
                throw new ForbiddenException(errorMessage);
            }
        } else {
            String errorMessage = "No client certificate was found";
            LOGGER.warning(errorMessage);
            throw new ForbiddenException(errorMessage);
        }
    }

    protected Config getGlobalConfig() {
        return MpConfig.toHelidonConfig(ConfigProvider.getConfig());
    }

    protected void createSplatMtlsFilter(String region, SplatMtlsFilterConfig splatMtlsFilterConfig) {
        try {
            shimmedSplatMtlsFilter = new JakartaServerFilter(
                    new com.oracle.pic.platform.splat.sdk.mtls.SplatMtlsFilter(Region.fromPublicRegionName(region),
                                                                               splatMtlsFilterConfig));
        } catch (Throwable e) {
            terminate(e);
        }
    }

    protected X509Certificate getClientTlsCertificate() {
        Certificate[] certificateChain = req.remotePeer().tlsCertificates().orElse(null);
        // Tls certificate is located at the beginning of the chain
        if (certificateChain != null && certificateChain[0] instanceof X509Certificate certificate) {
            return certificate;
        }
        return null;
    }

    protected String getRegion(Config config) {
        String regionOverride = config.get(OCI_REGION_CONFIG_KEY).asString().orElse(null);
        if (regionOverride != null && !regionOverride.isEmpty()) {
            LOGGER.info("Region config override: " + regionOverride);
            return regionOverride;
        }
        return getRegionFromIMDS();
    }

    protected String getRegionFromIMDS() {
        return GlobalServiceRegistry.registry().get(ImdsInstanceInfo.class).canonicalRegionName();
    }

    private static SplatMtlsFilterConfig setSplatMtlsFilterConfig(Config helidonSplatMtlsFilterConfig) {
        SplatMtlsFilterConfig splatMtlsFilterConfig = setSplatMtlsFilterParameter(helidonSplatMtlsFilterConfig,
                                                                                  SKIP_AUTHZ_VALIDATION_CHECK,
                                                                                  null);
        splatMtlsFilterConfig = setSplatMtlsFilterParameter(helidonSplatMtlsFilterConfig,
                                                            REJECT_X_REGION_CALLS,
                                                            splatMtlsFilterConfig);
        return splatMtlsFilterConfig;
    }

    private static SplatMtlsFilterConfig setSplatMtlsFilterParameter(Config helidonSplatMtlsFilterConfig,
                                                                     String configKey,
                                                                     SplatMtlsFilterConfig splatMtlsFilterConfig) {
        if (helidonSplatMtlsFilterConfig.get(configKey).exists()) {
            boolean value = helidonSplatMtlsFilterConfig.get(configKey).asBoolean().get();
            if (splatMtlsFilterConfig == null) {
                splatMtlsFilterConfig = new SplatMtlsFilterConfig();
            }
            if (Objects.equals(configKey, SKIP_AUTHZ_VALIDATION_CHECK)) {
                splatMtlsFilterConfig.setSkipAuthzValidationCheck(value);
            } else {
                splatMtlsFilterConfig.setRejectXRegionCalls(value);
            }
            LOGGER.info("splatMtlsFilterConfig." + configKey + "=" + value);
        }
        return splatMtlsFilterConfig;
    }

    private static void terminate(Throwable e) {
        LOGGER.log(Level.SEVERE, "Unable to start SplatMtlsFilter due to: " + e);
        System.exit(1);
    }
}

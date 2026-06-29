/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.examples.limits;

import java.util.logging.Logger;

import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Service.Inject;

import com.oracle.oci.limits.LimitsDPClient;
import com.oracle.oci.limits.requests.GetServiceLimitsRequest;
import com.oracle.oci.limits.responses.GetServiceLimitsResponse;

@Service.Singleton
class LimitsExample {

    private static final Logger LOGGER = Logger.getLogger(LimitsExample.class.getName());
    private final OciConfig ociConfig;
    private final LimitsDPClient client;

    @Inject
    LimitsExample(OciConfig ociConfig, LimitsDPClient client) {
        this.ociConfig = ociConfig;
        this.client = client;
    }

    public void run() {
        LOGGER.info("OCI config: " + ociConfig);

        GetServiceLimitsRequest request = GetServiceLimitsRequest.builder()
                .tag("tag")
                .group("group")
                .build();

        GetServiceLimitsResponse response = client.getServiceLimits(request);
        LOGGER.info("GetServiceLimits: " + response.getItems());
    }
}

/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.io.IOException;
import java.net.URI;

import io.helidon.microprofile.server.ServerCdiExtension;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Workaround for dynamic ports in HelidonTest.
 */
public class RestClientDynamicPortFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String port = String.valueOf(CDI.current().getBeanManager().getExtension(ServerCdiExtension.class).port());
        URI uri = requestContext.getUri();
        String fixedUri = uri.toString().replace("8080", port);
        requestContext.setUri(URI.create(fixedUri));
    }
}

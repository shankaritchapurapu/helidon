/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterProvider(RestClientDynamicPortFilter.class)
@RegisterRestClient(baseUri = "https://localhost:8080")
public interface MTlsTestRestClient {

    @GET
    @Path("/mtls-hello-world")
    @Produces(MediaType.TEXT_PLAIN)
    String getMtlsHelloWorld();
}

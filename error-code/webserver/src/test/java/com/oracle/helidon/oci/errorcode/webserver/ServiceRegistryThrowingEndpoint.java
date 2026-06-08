/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.errorcode.webserver;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

import com.oracle.helidon.oci.errorcode.ErrorCodes;
import com.oracle.helidon.oci.errorcode.ErrorDetail;
import com.oracle.helidon.oci.errorcode.RenderableException;

@RestServer.Endpoint
@Http.Path("/declarative")
@Service.Singleton
class ServiceRegistryThrowingEndpoint {
    @Http.GET
    @Http.Path("/service-registry-error")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    ErrorDetail serviceRegistryError() {
        throw new RenderableException(ErrorCodes.InvalidParameter,
                                      "Invalid parameter sent from declarative endpoint");
    }
}

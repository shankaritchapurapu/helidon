/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.requestid.microprofile;

import io.helidon.microprofile.server.ServerCdiExtension;

import com.oracle.helidon.oci.common.requestid.webserver.RequestIdServerFeature;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

@ApplicationScoped
class RegisterRequestIdFeatureBean {
    private final ServerCdiExtension server;

    @Inject
    RegisterRequestIdFeatureBean(ServerCdiExtension server) {
        this.server = server;
    }

    // must run before the server is started
    void registerFeature(@Observes
                         @Priority(Interceptor.Priority.LIBRARY_BEFORE - 100)
                         @Initialized(ApplicationScoped.class) Object event) {
        // feature discover is disabled in Helidon MicroProfile
        server.addFeature(RequestIdServerFeature.create());
    }
}

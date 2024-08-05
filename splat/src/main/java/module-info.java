/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

/**
 * Helidon support for oci-splat.
 */
module com.oracle.helidon.oci.splat {
    requires helidon.oci.common.javax.shim;

    requires java.logging;

    requires jakarta.annotation;
    requires jakarta.ws.rs;

    requires io.helidon.webserver;
    requires io.helidon.config.mp;

    requires core.regions;
    requires splat.sdk;
    requires io.helidon.service.registry;
    requires io.helidon.integrations.oci;
}

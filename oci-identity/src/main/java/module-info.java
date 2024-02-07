/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.oracle.helidon.oci.identity.MetricsHelper;

/**
 * Helidon support for oci-identity.
 */
module com.oracle.helidon.oci.identity {
    requires java.logging;
    requires java.ws.rs;

    requires io.helidon.logging.common;
    requires io.helidon.logging.jul;
    requires io.helidon.metrics.api;
    requires io.helidon.microprofile.server;

    requires microprofile.metrics.api;

    requires request.id;
    requires authentication.client;
    requires core;
    requires sdk;
    requires metrics.implementation;
    requires io.helidon.security;
    requires authproxy.filter;
    requires hk2.api;
    requires org.apache.commons.io;
    requires org.apache.commons.codec;
    requires io.helidon.config.mp;
    requires jakarta.interceptor.api;

    uses com.oracle.pic.identity.authentication.ServiceAuthenticationClient;

    exports com.oracle.helidon.oci.identity;

    provides org.glassfish.jersey.internal.spi.AutoDiscoverable
            with com.oracle.helidon.oci.identity.AuthenticationSupportAutoDiscoverable;

    provides javax.enterprise.inject.spi.Extension with MetricsHelper;

    // needed when running with modules - to make private methods accessible
    opens com.oracle.helidon.oci.identity to weld.core.impl, io.helidon.microprofile.cdi;
}

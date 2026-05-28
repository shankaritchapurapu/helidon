/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.commons.util.Region;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class IdentityRegistryDefaultRegionTest extends BaseAuthenticationClientTest {

    private ServiceRegistryManager registryManager;

    @AfterEach
    void shutdownServices() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void serviceRegistryUsesOciEnvRegionWhenIdentityRegionIsMissing() {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());

        assertThat(Services.get(Region.class).getPublicRegionName(), is("us-ashburn-1"));
        assertThat(Services.get(ServiceAuthenticationClient.class), notNullValue());
    }
}

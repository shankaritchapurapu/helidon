/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.authentication;

import io.helidon.common.types.TypeName;
import io.helidon.integrations.oci.ImdsInstanceInfo;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;

import com.oracle.bmc.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class InstancePrincipalAuthenticationDetailsProviderIT {
    private static final System.Logger LOGGER =
            System.getLogger(InstancePrincipalAuthenticationDetailsProviderIT.class.getName());
    private static final TypeName INSTANCE_PRINCIPAL_METHOD_IMPL = TypeName.create(
            "io.helidon.integrations.oci.authentication.instance.AuthenticationMethodInstancePrincipal");
    private static final String HELIDON_OCI_TENANCY_OCID =
            "ocid1.tenancy.oc1..aaaaaaaajvzlykcfhewzdlwnphkuowjuobt53bi5eyhok4pazitzd4w7t4eq";
    private static ServiceRegistry registry;

    @BeforeAll
    static void beforeAll() {
        LogConfig.configureRuntime();
        LOGGER.log(System.Logger.Level.INFO, "Initializing Service Registry");
        registry = GlobalServiceRegistry.registry();
    }

    @Test
    void testInstancePrincipalMethodAvailable() {
        LOGGER.log(System.Logger.Level.INFO, "Testing Instance Principal Authentication Details Provider");

        OciAuthenticationMethod atnMethod = registry.get(INSTANCE_PRINCIPAL_METHOD_IMPL);
        assertThat(atnMethod.method(), is("instance-principal"));
        assertThat(atnMethod.provider().isPresent(), is(true));

        ImdsInstanceInfo instanceInfo = registry.get(ImdsInstanceInfo.class);
        assertThat(instanceInfo.tenantId(), is(HELIDON_OCI_TENANCY_OCID));

        Region region = registry.get(Region.class);
        assertThat(region, is(Region.US_PHOENIX_1));
    }
}

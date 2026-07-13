/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.resource;

import java.net.URI;

import io.helidon.integrations.oci.OciConfig;
import io.helidon.integrations.oci.OciResourcePrincipalProvider;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourcePrincipalBuilderProviderTest {

    @Test
    void getUsesConfiguredResourcePrincipalOverrides() {
        var config = OciConfig.builder()
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .imdsBaseUri(URI.create("http://127.0.0.1/opc/v2/"))
                .tenantId("ocid1.tenancy.oc1..testresourceprincipal")
                .build();

        var builder = new ResourcePrincipalBuilderProvider(config).get();

        assertThat(builder.getFederationEndpoint(), is("https://auth.test.oraclecloud.com/v1/x509"));
        assertThat(builder.getMetadataBaseUrl(), is("http://127.0.0.1/opc/v2/"));
        assertThat(builder.getTenancyId(), is("ocid1.tenancy.oc1..testresourceprincipal"));
    }

    @Test
    void serviceRegistryProvidesBasicAuthenticationDetailsProvider() {
        var ociConfig = OciConfig.builder()
                .authenticationMethod("resource-principal")
                .build();

        var builder = mock(ResourcePrincipalAuthenticationDetailsProviderBuilder.class);
        var provider = mock(ResourcePrincipalAuthenticationDetailsProvider.class);
        when(builder.build()).thenReturn(provider);

        var registryConfig = ServiceRegistryConfig.builder()
                .discoverServices(true)
                .putContractInstance(OciConfig.class, ociConfig)
                .putContractInstance(ResourcePrincipalAuthenticationDetailsProviderBuilder.class, builder)
                .build();

        ServiceRegistryManager manager = ServiceRegistryManager.create(registryConfig);
        try {
            var registry = manager.registry();
            var authMethods = registry.all(OciAuthenticationMethod.class);
            BasicAuthenticationDetailsProvider resolved = registry.get(BasicAuthenticationDetailsProvider.class);
            var resourcePrincipalProvider = registry.get(OciResourcePrincipalProvider.class);
            var maybeResourcePrincipalMethod = authMethods.stream()
                    .filter(method -> "io.helidon.integrations.oci.authentication.resource.AuthenticationMethodResourcePrincipal"
                            .equals(method.getClass().getName()))
                    .findFirst();

            assertThat("AuthenticationMethodResourcePrincipal should be discovered",
                       maybeResourcePrincipalMethod.isPresent(),
                       is(true));
            assertThat(maybeResourcePrincipalMethod.map(OciAuthenticationMethod::method).orElse(null),
                       is("resource-principal"));
            assertThat(resolved, sameInstance(provider));
            assertThat(resourcePrincipalProvider.provider().orElseThrow(), sameInstance(provider));
        } finally {
            manager.shutdown();
        }
    }
}

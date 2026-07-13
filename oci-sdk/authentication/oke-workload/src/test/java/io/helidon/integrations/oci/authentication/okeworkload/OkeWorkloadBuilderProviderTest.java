/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.okeworkload;

import java.net.URI;
import java.time.Duration;

import io.helidon.integrations.oci.OciConfig;
import io.helidon.integrations.oci.OciResourcePrincipalProvider;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OkeWorkloadBuilderProviderTest {

    @Test
    void getUsesConfiguredOkeWorkloadOverrides() {
        var config = OciConfig.builder()
                .authenticationTimeout(Duration.ofSeconds(3))
                .imdsDetectRetries(2)
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .imdsBaseUri(URI.create("http://127.0.0.1/opc/v2/"))
                .tenantId("ocid1.tenancy.oc1..testokeworkload")
                .build();

        var builder = new OkeWorkloadBuilderProvider(config).get();

        assertThat(builder.getFederationEndpoint(), is("https://auth.test.oraclecloud.com/v1/x509"));
        assertThat(builder.getMetadataBaseUrl(), is("http://127.0.0.1/opc/v2/"));
        assertThat(builder.getTenancyId(), is("ocid1.tenancy.oc1..testokeworkload"));
    }

    @Test
    void serviceRegistryProvidesBasicAuthenticationDetailsProvider() {
        var ociConfig = OciConfig.builder()
                .authenticationMethod("oke-workload-identity")
                .build();

        var builder = mock(OkeWorkloadIdentityAuthenticationDetailsProviderBuilder.class);
        var provider = mock(OkeWorkloadIdentityAuthenticationDetailsProvider.class);
        when(builder.build()).thenReturn(provider);

        var registryConfig = ServiceRegistryConfig.builder()
                .discoverServices(true)
                .putContractInstance(OciConfig.class, ociConfig)
                .putContractInstance(OkeWorkloadIdentityAuthenticationDetailsProviderBuilder.class, builder)
                .build();

        ServiceRegistryManager manager = ServiceRegistryManager.create(registryConfig);
        try {
            var registry = manager.registry();
            var authMethods = registry.all(OciAuthenticationMethod.class);
            var resourcePrincipalProviders = registry.all(OciResourcePrincipalProvider.class);
            BasicAuthenticationDetailsProvider resolved = registry.get(BasicAuthenticationDetailsProvider.class);
            var maybeOkeWorkloadMethod = authMethods.stream()
                    .filter(method -> "io.helidon.integrations.oci.authentication.okeworkload.AuthenticationMethodOkeWorkload"
                            .equals(method.getClass().getName()))
                    .findFirst();

            assertThat("AuthenticationMethodOkeWorkload should be discovered",
                       maybeOkeWorkloadMethod.isPresent(),
                       is(true));
            assertThat(maybeOkeWorkloadMethod.map(OciAuthenticationMethod::method).orElse(null),
                       is("oke-workload-identity"));
            assertThat(resolved, sameInstance(provider));
            assertThat(resourcePrincipalProviders.size(), is(1));
            assertThat(resourcePrincipalProviders.getFirst().provider().orElseThrow(), sameInstance(provider));
        } finally {
            manager.shutdown();
        }
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.identity.client;

import java.util.function.Supplier;

import io.helidon.service.registry.Service;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.IdentityClient;

/**
 * Factory that creates a configured OCI Java SDK {@link Identity} client.
 */
@Service.Singleton
class IdentityClientFactory implements Supplier<Identity> {
    private final IdentityClientConfig config;
    private final BasicAuthenticationDetailsProvider authProvider;

    @Service.Inject
    IdentityClientFactory(IdentityClientConfig config, BasicAuthenticationDetailsProvider authProvider) {
        this.config = config;
        this.authProvider = authProvider;
    }

    @Override
    public Identity get() {
        ClientConfiguration clientConfiguration = config.client()
                .orElseGet(() -> ClientConfiguration.builder().build());

        IdentityClient.Builder builder = IdentityClient.builder()
                .configuration(clientConfiguration);

        Identity client = builder.build(authProvider);
        configureEndpoint(client);
        // Throws exception if endpoint was not resolved
        client.getEndpoint();
        return client;
    }

    private void configureEndpoint(Identity client) {
        config.endpoint().ifPresentOrElse(client::setEndpoint, () -> {
            config.region().ifPresent(client::setRegion);
            if (config.realmSpecificEndpointTemplateEnabled()) {
                client.useRealmSpecificEndpointTemplate(true);
            }
        });
    }

}

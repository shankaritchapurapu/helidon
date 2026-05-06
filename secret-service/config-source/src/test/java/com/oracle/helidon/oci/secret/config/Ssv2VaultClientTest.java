/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ProvidesClientConfigurators;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.CompositeClientConfigurator;
import com.oracle.bmc.http.JerseyDefaultConnectorConfigurator;
import com.oracle.bmc.http.internal.RestClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

class Ssv2VaultClientTest {
    private static final String PROVIDER_PROPERTY = "ssv2.provider";

    @Test
    void getSecretDoesNotSendVaultToken() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<List<String>> vaultTokenHeaders = new AtomicReference<>();
        AtomicReference<List<String>> authorizationHeaders = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawPath());
            vaultTokenHeaders.set(exchange.getRequestHeaders().get("X-Vault-Token"));
            authorizationHeaders.set(exchange.getRequestHeaders().get("Authorization"));

            byte[] response = "{\"data\":{\"secret\":\"dmFsdWU=\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        server.start();

        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            try (Ssv2VaultClient client = new Ssv2VaultClient(authProvider(),
                                                              ClientConfiguration.builder().build(),
                                                              null,
                                                              endpoint)) {
                Map<String, String> secret = client.getSecret("/secret/demo/latest");

                assertThat(secret.get("secret"), is("dmFsdWU="));
                assertThat(requestPath.get(), is("/v1/%2Fsecret%2Fdemo%2Flatest"));
                assertThat(vaultTokenHeaders.get(), nullValue());
                assertThat(authorizationHeaders.get(), notNullValue());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void getSecretReturnsNullForNotFound() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawPath());

            byte[] response = "{\"code\":\"NotAuthorizedOrNotFound\",\"message\":\"not found\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        server.start();

        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            try (Ssv2VaultClient client = new Ssv2VaultClient(authProvider(),
                                                              ClientConfiguration.builder().build(),
                                                              null,
                                                              endpoint)) {
                assertThat(client.getSecret("/secret/deleted/latest"), nullValue());
                assertThat(requestPath.get(), is("/v1/%2Fsecret%2Fdeleted%2Flatest"));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void providerConfiguratorSurvivesConnectorOverride() throws ReflectiveOperationException {
        ClientConfigurator providerConfigurator = new ClientConfigurator() {
            @Override
            public void customizeBuilder(ClientBuilder builder) {
                builder.property(PROVIDER_PROPERTY, "configured");
            }

            @Override
            public void customizeClient(Client client) {
            }
        };
        ClientConfigurator customConfigurator = new ClientConfigurator() {
            @Override
            public void customizeBuilder(ClientBuilder builder) {
            }

            @Override
            public void customizeClient(Client client) {
            }
        };
        BasicAuthenticationDetailsProvider authenticationDetailsProvider =
                new ConfiguratorAuthProvider(authProvider(), List.of(providerConfigurator));

        try (Ssv2VaultClient client = new Ssv2VaultClient(authenticationDetailsProvider,
                                                          ClientConfiguration.builder().build(),
                                                          customConfigurator,
                                                          "http://127.0.0.1:1/v1")) {
            Field clientField = Ssv2VaultClient.class.getDeclaredField("client");
            clientField.setAccessible(true);
            RestClient restClient = (RestClient) clientField.get(client);

            ClientConfigurator configurator = restClient.getClientConfigurator();
            assertThat(configurator, instanceOf(CompositeClientConfigurator.class));
            CompositeClientConfigurator composite = (CompositeClientConfigurator) configurator;
            assertThat(composite.getConfigurators().get(0), instanceOf(JerseyDefaultConnectorConfigurator.class));
            assertThat(composite.getConfigurators().get(1), sameInstance(providerConfigurator));
            assertThat(composite.getConfigurators().get(2).getClass().getSimpleName(),
                       is("HttpUrlConnectorConfigurator"));
            assertThat(composite.getConfigurators().get(3), sameInstance(customConfigurator));

            ClientBuilder builder = ClientBuilder.newBuilder();
            configurator.customizeBuilder(builder);
            assertThat(builder.getConfiguration().getProperty(PROVIDER_PROPERTY), is("configured"));
        }
    }

    private static BasicAuthenticationDetailsProvider authProvider() {
        Path keyPath = testKeyPath();
        Supplier<InputStream> privateKeySupplier = () -> {
            try {
                return Files.newInputStream(keyPath);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read test private key", e);
            }
        };
        return SimpleAuthenticationDetailsProvider.builder()
                .tenantId("ocid1.tenancy.oc1..testtenant")
                .userId("ocid1.user.oc1..testuser")
                .fingerprint("11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00")
                .region(Region.US_ASHBURN_1)
                .privateKeySupplier(privateKeySupplier)
                .build();
    }

    private static Path testKeyPath() {
        List<Path> candidates = List.of(
                Path.of("..", "..", "limits", "src", "test", "resources", "key.pem"),
                Path.of("limits", "src", "test", "resources", "key.pem"));
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to locate limits test private key");
    }

    private record ConfiguratorAuthProvider(BasicAuthenticationDetailsProvider delegate,
                                            List<ClientConfigurator> configurators)
            implements BasicAuthenticationDetailsProvider, ProvidesClientConfigurators {
        @Override
        public String getKeyId() {
            return delegate.getKeyId();
        }

        @Override
        public InputStream getPrivateKey() {
            return delegate.getPrivateKey();
        }

        @Override
        public String getPassPhrase() {
            return delegate.getPassPhrase();
        }

        @Override
        public char[] getPassphraseCharacters() {
            return delegate.getPassphraseCharacters();
        }

        @Override
        public List<ClientConfigurator> getClientConfigurators() {
            return configurators;
        }
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.instance;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.helidon.integrations.oci.OciConfig;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider.InstancePrincipalsAuthenticationDetailsProviderBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstancePrincipalBuilderProviderTest {

    @Test
    void getUsesConfiguredInstancePrincipalOverrides() {
        var config = OciConfig.builder()
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .imdsBaseUri(URI.create("http://127.0.0.1/opc/v2/"))
                .tenantId("ocid1.tenancy.oc1..testinstanceprincipal")
                .build();

        var builder = new InstancePrincipalBuilderProvider(config).get();

        assertThat(builder.getFederationEndpoint(), is("https://auth.test.oraclecloud.com/v1/x509"));
        assertThat(builder.getMetadataBaseUrl(), is("http://127.0.0.1/opc/v2/"));
        assertThat(builder.getTenancyId(), is("ocid1.tenancy.oc1..testinstanceprincipal"));
    }

    @Test
    void getKeepsSdkDefaultsWhenOverridesAbsent() {
        var expected = InstancePrincipalsAuthenticationDetailsProvider.builder();
        var builder = new InstancePrincipalBuilderProvider(OciConfig.create()).get();

        assertThat(builder.getFederationEndpoint(), is(expected.getFederationEndpoint()));
        assertThat(builder.getMetadataBaseUrl(), is(expected.getMetadataBaseUrl()));
        assertThat(builder.getTenancyId(), is(expected.getTenancyId()));
    }

    @Test
    void serviceRegistryProvidesBasicAuthenticationDetailsProvider() throws IOException {
        try (var server = new TestImdsServer()) {
            var ociConfig = OciConfig.builder()
                    .authenticationMethod("instance-principal")
                    .imdsBaseUri(URI.create("http://127.0.0.1:" + server.port() + "/opc/v2/"))
                    .build();

            var builder = mock(InstancePrincipalsAuthenticationDetailsProviderBuilder.class);
            var provider = mock(InstancePrincipalsAuthenticationDetailsProvider.class);
            when(builder.build()).thenReturn(provider);

            var registryConfig = ServiceRegistryConfig.builder()
                    .discoverServices(true)
                    .putContractInstance(OciConfig.class, ociConfig)
                    .putContractInstance(InstancePrincipalsAuthenticationDetailsProviderBuilder.class, builder)
                    .build();

            ServiceRegistryManager manager = ServiceRegistryManager.create(registryConfig);
            try {
                var registry = manager.registry();
                var authMethods = registry.all(OciAuthenticationMethod.class);
                BasicAuthenticationDetailsProvider resolved = registry.get(BasicAuthenticationDetailsProvider.class);
                var maybeInstancePrincipalMethod = authMethods.stream()
                        .filter(method -> "io.helidon.integrations.oci.authentication.instance.AuthenticationMethodInstancePrincipal"
                                .equals(method.getClass().getName()))
                        .findFirst();

                assertThat("AuthenticationMethodInstancePrincipal should be discovered",
                           maybeInstancePrincipalMethod.isPresent(),
                           is(true));
                assertThat(maybeInstancePrincipalMethod.map(OciAuthenticationMethod::method).orElse(null),
                           is("instance-principal"));
                assertThat(resolved, sameInstance(provider));
            } finally {
                manager.shutdown();
            }
        }
    }

    private static final class TestImdsServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final Thread thread;

        private TestImdsServer() throws IOException {
            this.serverSocket = new ServerSocket(0);
            this.thread = new Thread(this::serve, "test-imds-server");
            this.thread.start();
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private void serve() {
            try (Socket socket = serverSocket.accept();
                 OutputStream outputStream = socket.getOutputStream()) {
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                outputStream.write(("HTTP/1.1 200 OK\r\n"
                                            + "Content-Type: application/json\r\n"
                                            + "Content-Length: " + body.length + "\r\n"
                                            + "Connection: close\r\n"
                                            + "\r\n")
                                           .getBytes(StandardCharsets.US_ASCII));
                outputStream.write(body);
                outputStream.flush();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            try {
                thread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing test IMDS server", e);
            }
        }
    }
}

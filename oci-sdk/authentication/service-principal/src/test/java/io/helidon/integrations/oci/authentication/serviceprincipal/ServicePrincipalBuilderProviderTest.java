/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.integrations.oci.OciResourcePrincipalProvider;
import io.helidon.integrations.oci.OciServicePrincipalProvider;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RpS2SAuthenticationDetailsProvider;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider.S2SAuthenticationDetailsProviderBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicePrincipalBuilderProviderTest {
    private static final String METHOD_CLASS_NAME =
            "io.helidon.integrations.oci.authentication.serviceprincipal.AuthenticationMethodServicePrincipal";

    @Test
    void getUsesConfiguredServicePrincipalOverrides() {
        var config = OciConfig.builder()
                .region(Region.US_ASHBURN_1)
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .imdsBaseUri(URI.create("http://127.0.0.1/opc/v2/"))
                .tenantId("ocid1.tenancy.oc1..testserviceprincipal")
                .build();
        var testBuilder = new TestServicePrincipalS2SAuthenticationDetailsProviderBuilder();

        var builder = new ServicePrincipalBuilderProvider(config, Optional::empty) {
            @Override
            ServicePrincipalS2SAuthenticationDetailsProviderBuilder getBuilder() {
                return testBuilder;
            }
        }.get();

        assertThat(builder.getRegion(), is(Region.US_ASHBURN_1));
        assertThat(builder.getFederationEndpoint(), is("https://auth.test.oraclecloud.com/v1/x509"));
        assertThat(builder.getMetadataBaseUrl(), is("http://127.0.0.1/opc/v2/"));
        assertThat(builder.getTenancyId(), is("ocid1.tenancy.oc1..testserviceprincipal"));
        assertThat(testBuilder.useInstancePrincipalsCalled, is(true));
    }

    @Test
    void getUsesConfiguredServicePrincipalCertificates() {
        var config = OciConfig.builder()
                .region(Region.US_ASHBURN_1)
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .tenantId("ocid1.tenancy.oc1..testserviceprincipal")
                .build();
        var servicePrincipalConfig = ServicePrincipalMethodConfig.builder()
                .useInstancePrincipal(false)
                .certificates(List.of(
                        ServicePrincipalCertificateConfig.builder()
                                .certificate("serverCert.pem")
                                .privateKey("serverKey.pem")
                                .build(),
                        ServicePrincipalCertificateConfig.builder()
                                .certificate("serverCert.pem")
                                .build()))
                .build();
        var testBuilder = new TestServicePrincipalS2SAuthenticationDetailsProviderBuilder();

        var builder = new ServicePrincipalBuilderProvider(config, () -> Optional.of(servicePrincipalConfig)) {
            @Override
            ServicePrincipalS2SAuthenticationDetailsProviderBuilder getBuilder() {
                return testBuilder;
            }
        }.get();

        var certificateAndKeyPair = builder.getLeafCertificateSupplier().getCertificateAndKeyPair();
        assertThat(builder.getRegion(), is(Region.US_ASHBURN_1));
        assertThat(builder.getFederationEndpoint(), is("https://auth.test.oraclecloud.com/v1/x509"));
        assertThat(builder.getTenancyId(), is("ocid1.tenancy.oc1..testserviceprincipal"));
        assertThat(certificateAndKeyPair.getCertificate().getSubjectX500Principal().getName(), is("CN=localhost"));
        assertThat(certificateAndKeyPair.getPrivateKey(), notNullValue());
        assertThat(testBuilder.intermediateCertificateSupplierCount(), is(1));
        assertThat(testBuilder.useInstancePrincipalsCalled, is(false));
        assertThat(testBuilder.servicePrincipalPurposeCalled, is(true));
    }

    @Test
    void configBindingIncludesServicePrincipalCertificates() {
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                entry("helidon.oci.authentication-method", "service-principal"),
                entry("helidon.oci.federation-endpoint", "https://auth.test.oraclecloud.com/v1/x509"),
                entry("helidon.oci.tenant-id", "ocid1.tenancy.oc1..testserviceprincipal"),
                entry("helidon.oci.authentication.service-principal.use-instance-principal", "false"),
                entry("helidon.oci.authentication.service-principal.certificates.0.certificate", "serverCert.pem"),
                entry("helidon.oci.authentication.service-principal.certificates.0.private-key", "serverKey.pem"),
                entry("helidon.oci.authentication.service-principal.certificates.0.passphrase", ""),
                entry("helidon.oci.authentication.service-principal.certificates.1.certificate", "serverCert.pem")
        )));

        OciConfig ociConfig = OciConfig.create(config.get("helidon.oci"));
        ServicePrincipalMethodConfig servicePrincipalConfig = ServicePrincipalConfigProvider.create(ociConfig).get();

        assertThat(ociConfig.authenticationMethod(), is("service-principal"));
        assertThat(ociConfig.federationEndpoint().orElseThrow(),
                   is(URI.create("https://auth.test.oraclecloud.com/v1/x509")));
        assertThat(ociConfig.tenantId().orElseThrow(), is("ocid1.tenancy.oc1..testserviceprincipal"));
        assertThat(servicePrincipalConfig.useInstancePrincipal(), is(false));
        assertThat(servicePrincipalConfig.certificates().size(), is(2));
        assertThat(servicePrincipalConfig.certificates().getFirst().certificate(), is("serverCert.pem"));
        assertThat(servicePrincipalConfig.certificates().getFirst().privateKey().orElseThrow(), is("serverKey.pem"));
    }

    @Test
    void serviceRegistryProvidesConfiguredServicePrincipalAuthenticationDetailsProvider() throws IOException {
        try (var server = new TestImdsServer()) {
            var ociConfig = OciConfig.builder()
                    .authenticationMethod("service-principal")
                    .imdsBaseUri(URI.create("http://127.0.0.1:" + server.port() + "/opc/v2/"))
                    .build();

            var builder = mock(S2SAuthenticationDetailsProviderBuilder.class);
            var provider = mock(S2SAuthenticationDetailsProvider.class);
            when(builder.build()).thenReturn(provider);

            var registryConfig = ServiceRegistryConfig.builder()
                    .discoverServices(true)
                    .putContractInstance(Config.class, Config.empty())
                    .putContractInstance(OciConfig.class, ociConfig)
                    .putContractInstance(S2SAuthenticationDetailsProviderBuilder.class, builder)
                    .build();

            ServiceRegistryManager manager = ServiceRegistryManager.create(registryConfig);
            try {
                var registry = manager.registry();
                var authMethods = registry.all(OciAuthenticationMethod.class);
                var servicePrincipalProviders = registry.all(OciServicePrincipalProvider.class);
                BasicAuthenticationDetailsProvider resolved = registry.get(BasicAuthenticationDetailsProvider.class);
                var maybeServicePrincipalMethod = authMethods.stream()
                        .filter(method -> METHOD_CLASS_NAME.equals(method.getClass().getName()))
                        .findFirst();

                assertThat("AuthenticationMethodServicePrincipal should be discovered",
                           maybeServicePrincipalMethod.isPresent(),
                           is(true));
                assertThat(maybeServicePrincipalMethod.map(OciAuthenticationMethod::method).orElse(null),
                           is("service-principal"));
                assertThat(resolved, sameInstance(provider));
                assertThat(servicePrincipalProviders.size(), is(1));
                assertThat(servicePrincipalProviders.getFirst().provider().orElseThrow(), sameInstance(provider));
            } finally {
                manager.shutdown();
            }
        }
    }

    @Test
    void servicePrincipalAuthenticationDetailsProviderDoesNotRequireImdsForConfiguredCertificates() {
        var ociConfig = OciConfig.builder()
                .authenticationMethod("service-principal")
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .tenantId("ocid1.tenancy.oc1..testserviceprincipal")
                .build();
        var servicePrincipalConfig = ServicePrincipalMethodConfig.builder()
                .useInstancePrincipal(false)
                .certificates(List.of(
                        ServicePrincipalCertificateConfig.builder()
                                .certificate("serverCert.pem")
                                .privateKey("serverKey.pem")
                                .build()))
                .build();

        var builder = mock(S2SAuthenticationDetailsProviderBuilder.class);
        var provider = mock(S2SAuthenticationDetailsProvider.class);
        when(builder.build()).thenReturn(provider);

        var method = new AuthenticationMethodServicePrincipal(ociConfig,
                                                              () -> Optional.of(servicePrincipalConfig),
                                                              () -> Optional.of(builder),
                                                              List.of());

        assertThat(method.provider().orElseThrow(), sameInstance(provider));
    }

    @Test
    void resourcePrincipalIsElevatedToReusableServicePrincipal() {
        var ociConfig = OciConfig.builder()
                .region(Region.US_ASHBURN_1)
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .tenantId("ocid1.tenancy.oc1..testserviceprincipal")
                .build();
        BasicAuthenticationDetailsProvider resourcePrincipal = mock(BasicAuthenticationDetailsProvider.class);
        when(resourcePrincipal.getKeyId()).thenReturn("ST$resource-principal-token");
        when(resourcePrincipal.getPrivateKey())
                .thenAnswer(invocation -> Files.newInputStream(Path.of("src/test/resources/serverKey.pem")));
        OciResourcePrincipalProvider resourcePrincipalProvider = () -> Optional.of(resourcePrincipal);

        var method = new AuthenticationMethodServicePrincipal(ociConfig,
                                                              Optional::empty,
                                                              Optional::empty,
                                                              List.of(resourcePrincipalProvider));

        assertInstanceOf(RpS2SAuthenticationDetailsProvider.class, method.provider().orElseThrow());
        assertInstanceOf(OciServicePrincipalProvider.class, method);
    }

    @Test
    void missingCertificateReportsConfiguredResource() {
        var config = ServicePrincipalCertificateConfig.builder()
                .certificate("missing-service-principal-cert.pem")
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> ServicePrincipalBuilderProvider.loadX509Certificate(config));

        assertThat(error.getMessage(), containsString("missing-service-principal-cert.pem"));
    }

    @Test
    void missingPrivateKeyReportsConfiguredResource() {
        var config = ServicePrincipalCertificateConfig.builder()
                .certificate("serverCert.pem")
                .privateKey("missing-service-principal-key.pem")
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> ServicePrincipalBuilderProvider.loadRsaPrivateKey(config));

        assertThat(error.getMessage(), containsString("missing-service-principal-key.pem"));
    }

    @Test
    void nonRsaPrivateKeyReportsConfiguredResource() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        String keyResource = "ec-key.pem";
        Path keyPath = Path.of(ServicePrincipalBuilderProviderTest.class.getResource("/").toURI())
                .resolve(keyResource);
        Files.writeString(keyPath, privateKeyPem(generator.generateKeyPair().getPrivate().getEncoded()),
                          StandardCharsets.US_ASCII);
        var config = ServicePrincipalCertificateConfig.builder()
                .certificate("serverCert.pem")
                .privateKey(keyResource)
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> ServicePrincipalBuilderProvider.loadRsaPrivateKey(config));

        assertThat(error.getMessage(), containsString(keyResource));
        assertThat(error.getCause().getMessage(), containsString("RSA private key"));
    }

    private static String privateKeyPem(byte[] encoded) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encoded)
                + "\n-----END PRIVATE KEY-----\n";
    }

    private static final class TestServicePrincipalS2SAuthenticationDetailsProviderBuilder
            extends ServicePrincipalS2SAuthenticationDetailsProviderBuilder {
        private boolean useInstancePrincipalsCalled;
        private boolean servicePrincipalPurposeCalled;

        @Override
        public S2SAuthenticationDetailsProviderBuilder useInstancePrincipals() {
            useInstancePrincipalsCalled = true;
            return this;
        }

        @Override
        ServicePrincipalS2SAuthenticationDetailsProviderBuilder servicePrincipalPurpose() {
            servicePrincipalPurposeCalled = true;
            return super.servicePrincipalPurpose();
        }

        private int intermediateCertificateSupplierCount() {
            return intermediateCertificateSuppliers == null ? 0 : intermediateCertificateSuppliers.size();
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

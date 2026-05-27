/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import io.helidon.common.LazyValue;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistry;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.waiter.DelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.WaiterConfiguration;

import static java.lang.System.Logger.Level.DEBUG;

@Service.Singleton
final class DefaultSsv2Client implements Ssv2Client {
    static final String DEFAULT_PREFIX = "oci.ssv2";
    static final String DEFAULT_ENDPOINT = "https://secret-service-ce.${oci.env.iaas-domain-name}/v1";
    static final String DEFAULT_CA_BUNDLE = "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem";
    static final int DEFAULT_MAX_RETRIES = 3;
    static final long DEFAULT_MIN_RETRY_DELAY_MS = 200L;

    private static final System.Logger LOGGER = System.getLogger(DefaultSsv2Client.class.getName());

    private final boolean enabled;
    private final String endpoint;
    private final String caBundle;
    private final ClientConfiguration clientConfiguration;
    private final LazyValue<Ssv2VaultClient> client;

    @Service.Inject
    DefaultSsv2Client(SecretServiceConfigSourceFactory factory) {
        this(factory.resolvedClientConfig());
    }

    DefaultSsv2Client(Ssv2ClientConfig config) {
        Objects.requireNonNull(config, "config");
        this.enabled = config.enabled();
        this.endpoint = config.endpoint();
        this.caBundle = config.tlsConfig().caBundle();
        this.clientConfiguration = clientConfiguration(config.retryConfig());
        this.client = LazyValue.create(this::initClient);
    }

    @Override
    public Optional<byte[]> getSecretAsBytes(String path) {
        return getSecretAsBase64(path)
                .map(Base64.getDecoder()::decode);
    }

    Optional<String> getSecretAsString(String path) {
        return getSecretAsBytes(path)
                .map(value -> new String(value, StandardCharsets.UTF_8));
    }

    Optional<String> getSecretAsBase64(String path) {
        if (!enabled) {
            return Optional.empty();
        }

        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Getting secret {0}", path);
        }

        Objects.requireNonNull(path, "path");
        Map<String, String> data = client.get().getSecret(path);

        if (data == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(data.get("secret"));
    }

    @Service.PreDestroy
    void close() {
        if (client.isLoaded()) {
            client.get().close();
        }
    }

    ClientConfiguration clientConfiguration() {
        return clientConfiguration;
    }

    private Ssv2VaultClient initClient() {
        ServiceRegistry registry = GlobalServiceRegistry.registry();
        BasicAuthenticationDetailsProvider authenticationDetailsProvider =
                registry.get(BasicAuthenticationDetailsProvider.class);

        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Resolved SSv2 client endpoint: {0}", endpoint);
        }

        return new Ssv2VaultClient(authenticationDetailsProvider,
                                   clientConfiguration,
                                   new CaBundleClientConfigurator(caBundle),
                                   endpoint);
    }

    private static ClientConfiguration clientConfiguration(Ssv2RetryConfig retryConfig) {
        RetryConfiguration retryConfiguration = retryConfiguration(retryConfig);
        ClientConfiguration.ClientConfigurationBuilder builder = ClientConfiguration.builder();
        if (retryConfiguration != null) {
            builder.retryConfiguration(retryConfiguration);
        }
        return builder.build();
    }

    private static RetryConfiguration retryConfiguration(Ssv2RetryConfig retryConfig) {
        int maxRetries = retryConfig.maxRetries();
        if (maxRetries <= 0) {
            return RetryConfiguration.NO_RETRY_CONFIGURATION;
        }

        long minRetryDelayMs = retryConfig.minRetryDelayInMs();
        long maxRetryDelayMs = retryConfig.maxRetryDelayInMs()
                .orElse(minRetryDelayMs);

        return RetryConfiguration.builder()
                .terminationStrategy(new MaxAttemptsTerminationStrategy(maxRetries))
                .delayStrategy(new BoundedExponentialDelayStrategy(minRetryDelayMs, maxRetryDelayMs))
                .build();
    }

    private static SSLContext sslContext(String caBundle) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            try (InputStream inputStream = Files.newInputStream(Path.of(caBundle))) {
                Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(inputStream);
                if (certificates.isEmpty()) {
                    throw new IllegalArgumentException("No certificates found in CA bundle: " + caBundle);
                }
                int index = 0;
                for (Certificate certificate : certificates) {
                    trustStore.setCertificateEntry("ssv2-ca-" + index++, certificate);
                }
            }

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize SSv2 trust configuration from " + caBundle, e);
        }
    }

    private static final class CaBundleClientConfigurator implements ClientConfigurator {
        private final String caBundle;

        private CaBundleClientConfigurator(String caBundle) {
            this.caBundle = caBundle;
        }

        @Override
        public void customizeBuilder(ClientBuilder builder) {
            builder.sslContext(sslContext(caBundle));
        }

        @Override
        public void customizeClient(Client client) {
        }
    }

    private static final class BoundedExponentialDelayStrategy implements DelayStrategy {
        private final long minDelayMs;
        private final long maxDelayMs;

        private BoundedExponentialDelayStrategy(long minDelayMs, long maxDelayMs) {
            this.minDelayMs = Math.max(1L, minDelayMs);
            this.maxDelayMs = Math.max(this.minDelayMs, maxDelayMs);
        }

        @Override
        public long nextDelay(WaiterConfiguration.WaitContext context) {
            int attemptsMade = Math.max(0, context.getAttemptsMade() - 1);
            long delay = minDelayMs;
            for (int i = 0; i < attemptsMade; i++) {
                if (delay >= maxDelayMs) {
                    return maxDelayMs;
                }
                delay = Math.min(maxDelayMs, delay * 2);
            }
            return delay;
        }
    }
}

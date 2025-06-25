/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.pic.identity.authentication.swagger.utils;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Optional;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.OciCircuitBreaker;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.jipher.provider.JipherJCE;
import com.oracle.pic.identity.authentication.SwaggerClientConfigurator;
import com.oracle.pic.identity.authentication.X509FederationClient;
import com.oracle.pic.identity.authentication.key.SessionKeySupplier;
import com.oracle.pic.identity.authentication.metrics.NoopAuthMetricsImpl;
import org.junit.jupiter.api.Test;

public class SwaggerClientUtilTest {

    @Test
    void retry() {
        RetryConfiguration retryConfiguration = SwaggerClientUtil.getInstanceOfRetryConfiguration(3);
        OciCircuitBreaker breaker = SwaggerClientUtil.getInstanceOfCircuitBreaker();
        ClientConfiguration configuration = SwaggerClientUtil.getInstanceOfClientConfiguration(3);

        breaker.acquirePermission();
        breaker.acquirePermission();
        breaker.acquirePermission();
        breaker.releasePermission();
        breaker.releasePermission();
        breaker.releasePermission();

        breaker.getState();

        SwaggerClientConfigurator.builder().build();
    }

    @Test
    void clientConfiguration() {
        SwaggerClientConfigurator.builder().build();
    }

    @Test
    void fedClient() throws NoSuchAlgorithmException {
        if (Arrays.stream(Security.getProviders())
                .map(Provider::getName)
                .noneMatch(JipherJCE.PROVIDER_NAME::equals)) {
            Security.addProvider(new JipherJCE());
        }

        X509FederationClient.builder()
                //https://auth.us-phoenix-1.oraclecloud.com
                .authServiceEndpoint("http://localhost:8080/auth")
                .x509CertificateChainSupplier(() -> null)
                .sessionKeySupplier(new SessionKeySupplier() {
                    @Override
                    public KeyPair getKeyPair() {
                        return null;
                    }

                    @Override
                    public Optional<RSAPublicKey> getPublicKey() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<RSAPrivateKey> getPrivateKey() {
                        return Optional.empty();
                    }

                    @Override
                    public void refreshKeys() {

                    }
                })
                .fingerprintAlgorithm("SHA256withRSA")
                .authMetrics(new NoopAuthMetricsImpl())
                .build();
    }
}

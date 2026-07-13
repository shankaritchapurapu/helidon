/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Optional;

import io.helidon.common.configurable.Resource;
import io.helidon.common.pki.Keys;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RefreshableOnNotAuthenticatedProvider;
import com.oracle.pic.identity.authentication.key.SessionKeySupplier;

/**
 * Adapts OCI resource-principal credentials for AuthSDK RP-to-SP exchange.
 */
final class AuthSdkResourcePrincipal implements SessionKeySupplier {
    private static final String SECURITY_TOKEN_PREFIX = "ST$";

    private final BasicAuthenticationDetailsProvider provider;

    AuthSdkResourcePrincipal(BasicAuthenticationDetailsProvider provider) {
        this.provider = provider;
    }

    String sessionToken() {
        String keyId = provider.getKeyId();
        if (keyId == null || !keyId.startsWith(SECURITY_TOKEN_PREFIX)) {
            throw new IllegalStateException("The OCI resource principal provider did not return a security token");
        }
        return keyId.substring(SECURITY_TOKEN_PREFIX.length());
    }

    @Override
    public KeyPair getKeyPair() {
        try (InputStream inputStream = provider.getPrivateKey()) {
            Keys keys = Keys.builder()
                    .pem(pem -> pem.key(Resource.create("oci-resource-principal-private-key", inputStream)))
                    .build();
            PrivateKey privateKey = keys.privateKey()
                    .orElseThrow(() -> new IllegalStateException(
                            "The OCI resource principal provider did not return a private key"));
            if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)) {
                throw new IllegalStateException("The OCI resource principal private key is not an RSA CRT key");
            }
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(),
                                                                 rsaPrivateKey.getPublicExponent());
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
            return new KeyPair(publicKey, rsaPrivateKey);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to load the OCI resource principal session key", e);
        }
    }

    @Override
    public Optional<RSAPublicKey> getPublicKey() {
        return Optional.of((RSAPublicKey) getKeyPair().getPublic());
    }

    @Override
    public Optional<RSAPrivateKey> getPrivateKey() {
        return Optional.of((RSAPrivateKey) getKeyPair().getPrivate());
    }

    @Override
    public void refreshKeys() {
        if (provider instanceof RefreshableOnNotAuthenticatedProvider<?> refreshableProvider) {
            refreshableProvider.refresh();
        }
    }
}

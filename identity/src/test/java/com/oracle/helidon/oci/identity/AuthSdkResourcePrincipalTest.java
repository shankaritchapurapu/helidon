/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RefreshableOnNotAuthenticatedProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthSdkResourcePrincipalTest {

    @Test
    void adaptsSecurityTokenAndSessionKey() throws IOException {
        TestAuthenticationDetailsProvider provider =
                new TestAuthenticationDetailsProvider("ST$resource-principal-token", privateKey());
        AuthSdkResourcePrincipal resourcePrincipal = new AuthSdkResourcePrincipal(provider);

        KeyPair keyPair = resourcePrincipal.getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();

        assertThat(resourcePrincipal.sessionToken(), is("resource-principal-token"));
        assertThat(publicKey.getModulus(), is(privateKey.getModulus()));
        assertThat(publicKey.getPublicExponent(), is(privateKey.getPublicExponent()));
        assertThat(resourcePrincipal.getPublicKey().orElseThrow().getModulus(), is(privateKey.getModulus()));
        assertThat(resourcePrincipal.getPrivateKey().orElseThrow().getModulus(), is(privateKey.getModulus()));
    }

    @Test
    void refreshesTheOciProvider() throws IOException {
        TestAuthenticationDetailsProvider provider =
                new TestAuthenticationDetailsProvider("ST$resource-principal-token", privateKey());
        AuthSdkResourcePrincipal resourcePrincipal = new AuthSdkResourcePrincipal(provider);

        resourcePrincipal.refreshKeys();

        assertThat(provider.refreshed, is(true));
    }

    @Test
    void rejectsNonSecurityTokenKeyId() throws IOException {
        TestAuthenticationDetailsProvider provider =
                new TestAuthenticationDetailsProvider("not-a-security-token", privateKey());
        AuthSdkResourcePrincipal resourcePrincipal = new AuthSdkResourcePrincipal(provider);

        assertThrows(IllegalStateException.class, resourcePrincipal::sessionToken);
    }

    private static byte[] privateKey() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/serverKey.pem"));
    }

    private static final class TestAuthenticationDetailsProvider
            implements BasicAuthenticationDetailsProvider, RefreshableOnNotAuthenticatedProvider<String> {
        private final String keyId;
        private final byte[] privateKey;
        private boolean refreshed;

        private TestAuthenticationDetailsProvider(String keyId, byte[] privateKey) {
            this.keyId = keyId;
            this.privateKey = privateKey;
        }

        @Override
        public String getKeyId() {
            return keyId;
        }

        @Override
        public InputStream getPrivateKey() {
            return new ByteArrayInputStream(privateKey);
        }

        @Override
        public String getPassPhrase() {
            return null;
        }

        @Override
        public char[] getPassphraseCharacters() {
            return null;
        }

        @Override
        public String refresh() {
            refreshed = true;
            return keyId;
        }
    }
}

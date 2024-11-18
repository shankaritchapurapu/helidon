/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.io.IOException;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.pic.identity.authorization.AuthorizationSwaggerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthZClientTest {

    @Test
    public void authSwaggerClientBuilder() throws IOException, ClassNotFoundException {
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AuthorizationSwaggerClient.Builder builder = AuthorizationSwaggerClient.builder();
            builder.build(new BadAuthenticationProvider());
        });

        assertEquals("Unsupported auth provider type: com.oracle.helidon.oci.identity.AuthZClientTest$BadAuthenticationProvider", e.getMessage());

    }

    public static class BadAuthenticationProvider implements AbstractAuthenticationDetailsProvider {

    }
}

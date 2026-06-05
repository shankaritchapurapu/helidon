/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class AuthenticationMethodSessionTokenTest {

    @Test
    void explicitSessionTokenConfigDoesNotReadConfigFile() {
        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .passphrase("passphrase")
                .tenantId("tenant")
                .userId("user")
                .privateKeyPath(Path.of("private-key.pem"))
                .sessionToken("configured-token")
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();
        SessionTokenAuthenticationDetailsProvider expectedProvider =
                new AdpSessionTokenBuilderProvider(config, Optional::empty).get().orElseThrow();
        AtomicBoolean configFileEvaluated = new AtomicBoolean();

        var method = new AuthenticationMethodSessionToken(config,
                                                          () -> {
                                                              // Explicit session token config must short-circuit config file lookup.
                                                              configFileEvaluated.set(true);
                                                              throw new UncheckedIOException(new IOException("should not read config file"));
                                                          },
                                                          () -> Optional.of(expectedProvider));
        Optional<BasicAuthenticationDetailsProvider> provider = method.provider();

        assertThat(provider, is(Optional.of(expectedProvider)));
        assertThat(configFileEvaluated.get(), is(false));
    }
}

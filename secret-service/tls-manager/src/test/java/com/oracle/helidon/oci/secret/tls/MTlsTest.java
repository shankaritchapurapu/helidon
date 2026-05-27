/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.nio.file.Path;
import java.security.Principal;
import java.time.Duration;

import io.helidon.common.tls.Tls;
import io.helidon.common.tls.TlsClientAuth;
import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.scheduling.CronConfig;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.api.WebClient;
import io.helidon.webserver.WebServer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class MTlsTest {
    @Test
    void serverAndClientUseSecretServiceTlsManagers() {
        WebServer server = WebServer.builder()
                .port(0)
                .connectionOptions(options -> options.readTimeout(Duration.ZERO)
                        .connectTimeout(Duration.ZERO))
                .tls(serverTls())
                .routing(routing -> routing.get("/name", (req, res) -> {
                    String name = req.remotePeer().tlsPrincipal().map(Principal::getName).orElse("");
                    res.send(name);
                }))
                .build()
                .start();
        try {
            WebClient client = WebClient.builder()
                    .baseUri("https://localhost:" + server.port())
                    .tls(clientTls())
                    .connectTimeout(Duration.ZERO)
                    .readTimeout(Duration.ZERO)
                    .build();

            ClientResponseTyped<String> response = client.method(Method.GET)
                    .uri("/name")
                    .request(String.class);

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.entity(), is("CN=Helidon-Test-Client"));
        } finally {
            server.stop();
        }
    }

    private static Tls serverTls() {
        return Tls.builder()
                .endpointIdentificationAlgorithm("NONE")
                .clientAuth(TlsClientAuth.REQUIRED)
                .manager(SecretServiceTlsManager.create(localConfig("server-pki.json")))
                .build();
    }

    private static Tls clientTls() {
        return Tls.builder()
                .endpointIdentificationAlgorithm("NONE")
                .manager(SecretServiceTlsManager.create(localConfig("client-pki.json")))
                .build();
    }

    private static SecretServiceTlsManagerConfig localConfig(String pkiFile) {
        return SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .resource(resource -> resource.path(Path.of("src/test/resources/mtls/" + pkiFile)))
                             .buildPrototype())
                .trust(trust -> trust.path(Path.of("src/test/resources/mtls/ca.pem")))
                .buildPrototype();
    }

    private static CronConfig reload(boolean enabled) {
        return CronConfig.builder(SecretServiceTlsManagerConfigSupport.defaultReload())
                .enabled(enabled)
                .buildPrototype();
    }
}

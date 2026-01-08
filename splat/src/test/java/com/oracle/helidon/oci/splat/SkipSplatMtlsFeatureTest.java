/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RoutingTest
class SkipSplatMtlsFeatureTest {
    private final DirectClient client;

    SkipSplatMtlsFeatureTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        SplatMtlsFeatureTest.setUp(router);
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        Config config = Config.just(ConfigSources.create(
                Map.of("oci.splat.region", "us-ashburn-1",
                       "oci.splat.skip-authz-validation-check", "true")));
        return List.of(SplatMtlsFeature.create(new SplatMtlsConfigFactory(config).get()));
    }

    /*
     * Validation will be skipped because "oci.splat.skip-authz-validation-check" config is set to true.
     * This parameter should never be used in production.
     */
    @Test
    void testSkipSplatValidation() {
        var clientCertificateCN = SplatMtlsFeatureTest.VALID_SPLAT_CERTIFICATE_CN;
        client.setTls(true);
        if (clientCertificateCN != null) {
            client.clientTlsCertificates(new Certificate[] {SplatMtlsFeatureTest.getClientTlsCertificate(clientCertificateCN)});
        }
        Http1ClientResponse response = client.get("/get").request();

        assertThat(response.status(), is(Status.OK_200));
    }
}

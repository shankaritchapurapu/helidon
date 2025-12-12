/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.errorcode.webserver;

import java.util.List;
import java.util.Map;

import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.oracle.helidon.oci.errorcode.ErrorCodes;
import com.oracle.helidon.oci.errorcode.ErrorDetail;
import com.oracle.helidon.oci.errorcode.RenderableException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
public class WebServerErrorCodeTest {
    private final Http1Client client;

    WebServerErrorCodeTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRules rules) {
        rules.get("/error1", WebServerErrorCodeTest::error1)
                .get("/error2", WebServerErrorCodeTest::error2);
    }

    @SetUpFeatures
    static List<ServerFeature> serverFeatures() {
        return List.of(ErrorCodeServerFeature.create());
    }

    @Test
    void testError() {
        ClientResponseTyped<ErrorDetail> response = client.get("/error1")
                .request(ErrorDetail.class);

        assertThat(response.status(), is(ErrorCodes.InvalidParameter.status()));
        ErrorDetail errorDetail = response.entity();
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.InvalidParameter.errorCode()));
        assertThat(errorDetail.getMessage(), is("Invalid parameter sent"));
    }

    @Test
    void testError2() {
        ClientResponseTyped<ErrorDetail> response = client.get("/error2")
                .request(ErrorDetail.class);

        assertThat(response.status(), is(ErrorCodes.NotAuthenticated.status()));
        ErrorDetail errorDetail = response.entity();
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.NotAuthenticated.errorCode()));
        assertThat(errorDetail.getMessage(), is("User 'helidon' is not authenticated"));
        assertThat(errorDetail.getOriginalMessage(), is("Authentication failure for 'helidon'"));
        assertThat(errorDetail.getOriginalMessageTemplate(), is("Authentication failure for '{user}'"));
        assertThat(errorDetail.getMessageArguments(), is(Map.of("user", "helidon")));
    }

    private static void error2(ServerRequest req, ServerResponse res) {
        throw new RenderableException(ErrorCodes.NotAuthenticated,
                                      "User 'helidon' is not authenticated",
                                      "Authentication failure for 'helidon'",
                                      "Authentication failure for '{user}'",
                                      Map.of("user", "helidon"));
    }

    private static void error1(ServerRequest req, ServerResponse res) {
        throw new RenderableException(ErrorCodes.InvalidParameter, "Invalid parameter sent");
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.limits;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.HeaderNames;
import io.helidon.service.registry.Services;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.oracle.oci.limits.LimitsDPClient;
import com.oracle.oci.limits.requests.EvaluateLimitForAdRequest;
import com.oracle.oci.limits.responses.EvaluateLimitForAdResponse;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
public class LimitsDpClientTest {
    private final LimitsDPClient client;

    LimitsDpClientTest() {
        this.client = Services.get(LimitsDPClient.class);
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        builder.get("/20180322/limits/evaluate/group/{group}/limit/{limit}/tag/{tag}/value/{value}/region/{region}/ad/{ad}",
                  (request, response) -> response
                          .header(HeaderNames.CONTENT_TYPE_NAME, MediaTypes.APPLICATION_JSON_VALUE)
                          .send("true"));
    }

    @Test
    void test() {
        EvaluateLimitForAdRequest request = EvaluateLimitForAdRequest.builder()
                .ad("ad1")
                .value("val1")
                .region("region1")
                .limit("20")
                .compartmentId("c1")
                .tag("tag1")
                .group("group1")
                .build();

        EvaluateLimitForAdResponse response = client.evaluateLimitForAd(request);
        assertThat(response.getValue(), is(true));
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class StoreEndpointIT {
    private final Http1Client client;
    private final Set<String> createdKeys = new LinkedHashSet<>();

    StoreEndpointIT(Http1Client client) {
        this.client = client;
    }

    @AfterEach
    void cleanupCreatedKeys() {
        for (String key : createdKeys) {
            try (Http1ClientResponse response = delete("/store/items/" + key)) {
                assertThat(response.status(), anyOf(is(Status.OK_200), is(Status.NOT_FOUND_404)));
            }
        }
        createdKeys.clear();
    }

    @Test
    void testPostAndGetAgainstKievService() {
        String key = "!it-post-" + System.nanoTime();
        String value = "it-value-" + System.nanoTime();

        try (Http1ClientResponse response = post("/store/items", key + "=" + value)) {
            createdKeys.add(key);
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(key + "=" + value));
        }

        try (Http1ClientResponse response = get("/store/items/" + key)) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(value));
        }
    }

    @Test
    void testPutAndGetAgainstKievService() {
        String prefix = "!it-key-" + System.nanoTime();
        String key = prefix + "-1";
        String value = "it-value-" + System.nanoTime();
        String key2 = prefix + "-2";
        String value2 = value + "-2";

        try (Http1ClientResponse response = postItem(key, value)) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(key + "=" + value));
        }

        try (Http1ClientResponse response = putItem(key, value2)) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(key + "=" + value2));
        }

        try (Http1ClientResponse response = get("/store/items/" + key)) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(value2));
        }

        try (Http1ClientResponse response = postItem(key2, value2)) {
            assertThat(response.status(), is(Status.OK_200));
        }

        String pageToken = null;
        StringBuilder combinedBody = new StringBuilder();
        boolean sawNextPageToken = false;
        for (int i = 0; i < 20; i++) {
            var request = client.get("/store/items")
                    .queryParam("pageSize", "1")
                    .header(HeaderValues.ACCEPT_TEXT);
            if (pageToken != null) {
                request.queryParam("pageToken", pageToken);
            }
            try (Http1ClientResponse response = request.request()) {
                assertThat(response.status(), is(Status.OK_200));
                String body = response.as(String.class);
                combinedBody.append(body);
                Optional<String> nextPageToken = extractNextPageToken(body);
                sawNextPageToken |= nextPageToken.isPresent();
                pageToken = nextPageToken.orElse(null);
                if (combinedBody.indexOf(key + "=" + value2 + "\n") >= 0
                        && combinedBody.indexOf(key2 + "=" + value2 + "\n") >= 0) {
                    break;
                }
                if (pageToken == null) {
                    break;
                }
            }
        }

        assertThat(sawNextPageToken, is(true));
        assertThat(combinedBody.toString(), containsString(key + "=" + value2 + "\n"));
        assertThat(combinedBody.toString(), containsString(key2 + "=" + value2 + "\n"));

        try (Http1ClientResponse response = delete("/store/items/" + key)) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(key));
        }

        try (Http1ClientResponse response = get("/store/items/" + key)) {
            assertThat(response.status(), is(Status.NOT_FOUND_404));
        }
    }

    private static Optional<String> extractNextPageToken(String body) {
        for (String line : body.split("\n")) {
            if (line.startsWith("next-page-token=")) {
                return Optional.of(line.substring("next-page-token=".length()));
            }
        }
        return Optional.empty();
    }

    private Http1ClientResponse post(String path, String body) {
        return client.post(path)
                .header(HeaderValues.CONTENT_TYPE_TEXT_PLAIN)
                .header(HeaderValues.ACCEPT_TEXT)
                .submit(body);
    }

    private Http1ClientResponse postItem(String key, String value) {
        createdKeys.add(key);
        return post("/store/items", key + "=" + value);
    }

    private Http1ClientResponse put(String path, String body) {
        return client.put(path)
                .header(HeaderValues.CONTENT_TYPE_TEXT_PLAIN)
                .header(HeaderValues.ACCEPT_TEXT)
                .submit(body);
    }

    private Http1ClientResponse putItem(String key, String value) {
        return put("/store/items/" + key, value);
    }

    private Http1ClientResponse delete(String path) {
        return client.delete(path)
                .header(HeaderValues.ACCEPT_TEXT)
                .request();
    }

    private Http1ClientResponse get(String path) {
        return client.get(path)
                .header(HeaderValues.ACCEPT_TEXT)
                .request();
    }
}

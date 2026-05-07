/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.util.Map;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.json.binding.JsonBinding;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ServerTest
class TaggingEndpointTest {
    private static final JsonBinding JSON_BINDING = JsonBinding.create();

    private final Http1Client client;

    TaggingEndpointTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void testCreateAndDecodeResourceTagSlugRoundTrip() {
        String resourceId = "ocid1.instance.oc1..example";
        ResourceTags tags = new ResourceTags(
                Map.of("owner", "platform",
                       "environment", "dev"),
                Map.of("Operations",
                       Map.<String, Object>of("CostCenter", "42")),
                Map.of("orcl-cloud",
                       Map.<String, Object>of("free-tier-retained", "true")));
        TagSetPayload payload = new TagSetPayload(resourceId, tags);

        TagSlugView slug;
        try (Http1ClientResponse response = client.post("/tagging/slugs")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(payload))) {
            assertEquals(Status.OK_200, response.status());
            slug = JSON_BINDING.deserialize(response.as(String.class), TagSlugView.class);
            assertEquals(resourceId, slug.resourceId());
            assertEquals(tags, slug.tags());
        }

        try (Http1ClientResponse response = client.post("/tagging/tag-sets")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(new TagSlugRequest(resourceId, slug.tagSlug())))) {
            assertEquals(Status.OK_200, response.status());
            TaggedResourceView decoded = JSON_BINDING.deserialize(response.as(String.class), TaggedResourceView.class);
            assertEquals(resourceId, decoded.resourceId());
            assertEquals(tags, decoded.tags());
        }
    }

    @Test
    void testEmptySlugEndpointReturnsResourceWithEmptyTags() {
        String resourceId = "resource-empty";
        TagSlugView slug;
        try (Http1ClientResponse response = client.get("/tagging/slugs/empty")
                .queryParam("resourceId", resourceId)
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertEquals(Status.OK_200, response.status());
            slug = JSON_BINDING.deserialize(response.as(String.class), TagSlugView.class);
            assertEquals(resourceId, slug.resourceId());
            assertTrue(slug.tags().freeformTags().isEmpty());
            assertTrue(slug.tags().definedTags().isEmpty());
            assertTrue(slug.tags().systemTags().isEmpty());
        }

        try (Http1ClientResponse response = client.post("/tagging/tag-sets")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(new TagSlugRequest(resourceId, slug.tagSlug())))) {
            assertEquals(Status.OK_200, response.status());
            TaggedResourceView decoded = JSON_BINDING.deserialize(response.as(String.class), TaggedResourceView.class);
            assertEquals(resourceId, decoded.resourceId());
            assertTrue(decoded.tags().freeformTags().isEmpty());
            assertTrue(decoded.tags().definedTags().isEmpty());
            assertTrue(decoded.tags().systemTags().isEmpty());
        }
    }

    @Test
    void testRejectsInvalidInputs() {
        try (Http1ClientResponse response = client.post("/tagging/slugs")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(new TagSetPayload("", ResourceTags.empty())))) {
            assertEquals(Status.BAD_REQUEST_400, response.status());
        }

        try (Http1ClientResponse response = client.post("/tagging/tag-sets")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(new TagSlugRequest("resource-invalid", "")))) {
            assertEquals(Status.BAD_REQUEST_400, response.status());
        }

        try (Http1ClientResponse response = client.post("/tagging/tag-sets")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(new TagSlugRequest("resource-invalid", "not-base64")))) {
            assertEquals(Status.BAD_REQUEST_400, response.status());
        }
    }
}

/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.kiev;

import io.helidon.microprofile.testing.junit5.HelidonTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test Keiv support.
 */
@HelidonTest
class KievResourceTest {

    @Inject
    private WebTarget webTarget;

    /**
     * Retrieves value from Helidon Test Bucket.
     */
    @Test
    void testGetFromHelidonTestBucket() {
        Response response = webTarget.path("kiev").request().get();
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        //assert bucketContent.isPresent() : "Value not present";
        //assert bucketContent.get().id == fooValue.id : "Did not retrieve same value for buckets";
    }
}
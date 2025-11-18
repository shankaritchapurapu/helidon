/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.kiev;

import java.util.Objects;
import java.util.Optional;

import io.helidon.microprofile.testing.junit5.HelidonTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static com.oracle.tests.integration.kiev.KievResource.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test Keiv support.
 */
@HelidonTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KievResourceTest {

    @Inject
    private WebTarget webTarget;

    /**
     * Set value into Helidon Test Bucket.
     */
    @Test
    @Order(1)
    void testSetIntoHelidonTestBucket() {
        // Populate with the values you wish to populate with.
        Foo fooValue = new Foo();
        fooValue.fooValue = "This is a helidon test";
        fooValue.id = 1L;
        Entity<Foo> fooEntity = Entity.entity(fooValue,MediaType.APPLICATION_JSON);
        try (Response response = webTarget.path("/kiev").request().post(fooEntity)) {
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }
    }

    /**
     * Retrieves value from Helidon Test Bucket.
     */
    @Test
    @Order(2)
    void testGetFromHelidonTestBucket() {
        Response response = webTarget.path("kiev").queryParam("bucketId", 1L).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Optional<Foo> bucketContent = Optional.of(response.readEntity(Foo.class));
        assert Objects.equals(bucketContent.get().fooValue, "This is a helidon test") : "Did not retrieve same value for buckets";
        assert bucketContent.get().id == 1L : "Did not retrieve same id for buckets";
    }

}
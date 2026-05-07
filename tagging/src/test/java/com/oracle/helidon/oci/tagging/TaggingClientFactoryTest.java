/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tagging;

import org.junit.jupiter.api.Test;

import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.entities.TaggingClientImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TaggingClientFactoryTest {
    @Test
    void testFactoryCreatesTaggingClient() {
        TaggingClient client = new TaggingClientFactory(TaggingClientConfig.builder()
                                                                   .emitMetrics(false)
                                                                   .build()).get();

        assertInstanceOf(TaggingClientImpl.class, client);
    }

    @Test
    void testTaggingExceptionMapperCanLoad() {
        assertDoesNotThrow(() -> Class.forName("com.oracle.pic.tagging.client.exceptionMapper.TaggingExceptionMapper"));
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tagging;

import java.util.Map;

import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.entities.TaggingClientImpl;
import com.oracle.pic.tagging.client.tag.TagSet;
import com.oracle.pic.tagging.common.tagset.tagslice.DefinedTags;
import com.oracle.pic.tagging.common.tagset.tagslice.FreeformTags;
import com.oracle.pic.tagging.common.tagset.tagslice.SystemTags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaggingClientRoundTripTest {
    @Test
    void testRoundTripPreservesFreeformDefinedAndSystemTags() {
        TaggingClient client = TaggingClientImpl.builder()
                .emitMetrics(false)
                .build();
        TagSet original = TagSet.builder()
                .freeformTags(FreeformTags.builder()
                                       .tags(Map.of("owner", "platform",
                                                    "environment", "dev"))
                                       .build())
                .definedTags(DefinedTags.builder()
                                      .tags(Map.of("Operations",
                                                   Map.<String, Object>of("CostCenter", "42")))
                                      .build())
                .systemTags(SystemTags.builder()
                                     .tags(Map.of("orcl-cloud",
                                                  Map.<String, Object>of("free-tier-retained", "true")))
                                     .build())
                .build();

        byte[] slug = client.toByteArray(original);
        TagSet decoded = client.extractTagSet(slug);

        assertEquals(Map.of("owner", "platform",
                            "environment", "dev"),
                     decoded.getFreeformTags().orElseThrow().getTags());
        assertEquals(Map.of("Operations",
                            Map.<String, Object>of("CostCenter", "42")),
                     decoded.getDefinedTags().orElseThrow().getTags());
        assertEquals(Map.of("orcl-cloud",
                            Map.<String, Object>of("free-tier-retained", "true")),
                     decoded.getSystemTags().orElseThrow().getTags());
    }

    @Test
    void testEmptySlugExtractsEmptyTagMaps() {
        TaggingClient client = TaggingClientImpl.builder()
                .emitMetrics(false)
                .build();

        TagSet decoded = client.extractTagSet(client.createEmptyTagSlug());

        assertTrue(decoded.getFreeformTags().orElseThrow().getTags().isEmpty());
        assertTrue(decoded.getDefinedTags().orElseThrow().getTags().isEmpty());
        assertTrue(decoded.getSystemTags().orElseThrow().getTags().isEmpty());
    }
}

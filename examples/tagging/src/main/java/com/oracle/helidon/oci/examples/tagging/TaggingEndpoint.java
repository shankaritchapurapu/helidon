/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.json.binding.Json;
import io.helidon.service.registry.Service;
import io.helidon.validation.Validation;
import io.helidon.webserver.http.RestServer;

import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.tag.TagSet;
import com.oracle.pic.tagging.common.exception.BaseTagException;
import com.oracle.pic.tagging.common.tagset.tagslice.DefinedTags;
import com.oracle.pic.tagging.common.tagset.tagslice.FreeformTags;
import com.oracle.pic.tagging.common.tagset.tagslice.SystemTags;

/**
 * HTTP endpoint that demonstrates tag-set slug conversion for a resource.
 */
@RestServer.Endpoint
@Http.Path("/tagging")
@Service.Singleton
class TaggingEndpoint {
    private final TaggingClient taggingClient;

    @Service.Inject
    TaggingEndpoint(TaggingClient taggingClient) {
        this.taggingClient = taggingClient;
    }

    @Http.GET
    @Http.Path("/slugs/empty")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    TagSlugView emptyTagSlug(@Http.QueryParam("resourceId") Optional<String> resourceId) {
        try {
            return new TagSlugView(resourceId.orElse("untagged-resource"),
                                   encode(taggingClient.createEmptyTagSlug()),
                                   ResourceTags.empty());
        } catch (BaseTagException e) {
            throw new HttpException(e.getMessage(), Status.BAD_REQUEST_400, e);
        }
    }

    @Http.POST
    @Http.Path("/slugs")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    TagSlugView createTagSlug(@Validation.NotNull @Validation.Valid @Http.Entity TagSetPayload request) {
        try {
            ResourceTags tags = request.tags();
            return new TagSlugView(request.resourceId(),
                                   encode(taggingClient.toByteArray(toTagSet(tags))),
                                   tags);
        } catch (BaseTagException e) {
            throw new HttpException(e.getMessage(), Status.BAD_REQUEST_400, e);
        }
    }

    @Http.POST
    @Http.Path("/tag-sets")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    TaggedResourceView decodeTagSlug(@Validation.NotNull @Validation.Valid @Http.Entity TagSlugRequest request) {
        try {
            TagSet tagSet = taggingClient.extractTagSet(decode(request.tagSlug()));
            return new TaggedResourceView(request.resourceId(), toResourceTags(tagSet));
        } catch (BaseTagException e) {
            throw new HttpException(e.getMessage(), Status.BAD_REQUEST_400, e);
        }
    }

    private static TagSet toTagSet(ResourceTags tags) {
        TagSet.Builder builder = TagSet.builder();
        if (!tags.freeformTags().isEmpty()) {
            builder.freeformTags(FreeformTags.builder().tags(tags.freeformTags()).build());
        }
        if (!tags.definedTags().isEmpty()) {
            builder.definedTags(DefinedTags.builder().tags(tags.definedTags()).build());
        }
        if (!tags.systemTags().isEmpty()) {
            builder.systemTags(SystemTags.builder().tags(tags.systemTags()).build());
        }
        return builder.build();
    }

    private static ResourceTags toResourceTags(TagSet tagSet) {
        return new ResourceTags(tagSet.getFreeformTags()
                                      .map(FreeformTags::getTags)
                                      .orElse(Map.of()),
                                tagSet.getDefinedTags()
                                      .map(DefinedTags::getTags)
                                      .orElse(Map.of()),
                                tagSet.getSystemTags()
                                      .map(SystemTags::getTags)
                                      .orElse(Map.of()));
    }

    private static byte[] decode(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new HttpException("tagSlug must be valid Base64", Status.BAD_REQUEST_400, e);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

}

@Json.Entity
@Validation.Validated
record TagSetPayload(@Validation.NotNull
                     @Validation.String.NotEmpty
                     String resourceId,
                     ResourceTags tags) {
    TagSetPayload {
        tags = tags == null ? ResourceTags.empty() : tags;
    }
}

@Json.Entity
@Validation.Validated
record TagSlugRequest(@Validation.NotNull
                      @Validation.String.NotEmpty
                      String resourceId,
                      @Validation.NotNull
                      @Validation.String.NotEmpty
                      String tagSlug) {
}

@Json.Entity
record TagSlugView(String resourceId, String tagSlug, ResourceTags tags) {
}

@Json.Entity
record TaggedResourceView(String resourceId, ResourceTags tags) {
}

@Json.Entity
record ResourceTags(Map<String, String> freeformTags,
                    Map<String, Map<String, Object>> definedTags,
                    Map<String, Map<String, Object>> systemTags) {
    ResourceTags {
        freeformTags = freeformTags == null ? Map.of() : freeformTags;
        definedTags = definedTags == null ? Map.of() : definedTags;
        systemTags = systemTags == null ? Map.of() : systemTags;
    }

    static ResourceTags empty() {
        return new ResourceTags(Map.of(), Map.of(), Map.of());
    }
}

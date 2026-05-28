/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.util.Base64;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.json.binding.Json;
import io.helidon.service.registry.Service;
import io.helidon.validation.Validation;
import io.helidon.webserver.http.RestServer;

import com.oracle.bmc.identity.model.CreateTagDetails;
import com.oracle.bmc.identity.model.Tag;
import com.oracle.bmc.identity.requests.CreateTagRequest;
import com.oracle.bmc.identity.responses.CreateTagResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.pic.identity.authentication.error.AuthServerUnavailableException;
import com.oracle.pic.identity.authorization.permissions.ActionKind;
import com.oracle.pic.identity.authorization.permissions.OptionalVariableFactory;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequestFactory;
import com.oracle.pic.identity.authorization.sdk.AuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.IAuthorizationClient;
import com.oracle.pic.identity.authorization.sdk.response.AuthorizationResponseResult.AuthorizationResponseErrorCategory;
import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.tag.TagSet;
import com.oracle.pic.tagging.common.exception.BaseTagException;
import com.oracle.pic.tagging.common.tagset.tagslice.DefinedTags;
import com.oracle.pic.tagging.common.tagset.tagslice.FreeformTags;
import com.oracle.pic.tagging.common.tagset.tagslice.SystemTags;

/**
 * HTTP endpoint that demonstrates tag slug authorization and OCI Identity tag creation.
 */
@RestServer.Endpoint
@Http.Path("/tagging")
@Service.Singleton
class TaggingEndpoint {
    private static final String RESOURCE_KIND = "tagging-example-resource";
    static final String CREATE_PERMISSION = "TAGGING_EXAMPLE_RESOURCE_CREATE";
    static final String CREATE_TAG_PERMISSION = "TAGGING_EXAMPLE_TAG_CREATE";

    private final TaggingClient taggingClient;
    private final IAuthorizationClient authorizationClient;
    private final com.oracle.bmc.identity.Identity identity;

    @Service.Inject
    TaggingEndpoint(TaggingClient taggingClient,
                    IAuthorizationClient authorizationClient,
                    com.oracle.bmc.identity.Identity identity) {
        this.taggingClient = taggingClient;
        this.authorizationClient = authorizationClient;
        this.identity = identity;
    }

    @Http.POST
    @Http.Path("/resources")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    @AuthorizationPermission(CREATE_PERMISSION)
    TaggedResourceView createTaggedResource(@Validation.NotNull
                                            @Validation.Valid
                                            @Http.Entity CreateTaggedResourceRequest request,
                                            AuthorizationRequest authorizationRequest) {
        try {
            ResourceTags tags = request.tags();
            byte[] requestedTagSlug = taggingClient.toByteArray(toTagSet(tags));
            byte[] authorizedTagSlug = authorizeTags(authorizationRequest, request.compartmentId(), requestedTagSlug);
            ResourceTags authorizedTags = toResourceTags(taggingClient.extractTagSet(authorizedTagSlug));

            return new TaggedResourceView(request.resourceId(),
                                          request.compartmentId(),
                                          encode(authorizedTagSlug),
                                          authorizedTags);
        } catch (BaseTagException e) {
            throw new HttpException(e.getMessage(), Status.BAD_REQUEST_400, e);
        } catch (AuthServerUnavailableException e) {
            throw new HttpException("Authorization service is unavailable",
                                    Status.SERVICE_UNAVAILABLE_503,
                                    e);
        }
    }

    @Http.POST
    @Http.Path("/tag-definitions")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    @AuthorizationPermission(CREATE_TAG_PERMISSION)
    TagDefinitionView createTagDefinition(@Validation.NotNull
                                          @Validation.Valid
                                          @Http.Entity CreateTagDefinitionRequest request) {
        try {
            CreateTagResponse createTagResponse = identity.createTag(createTagRequest(request));
            return toTagDefinition(createTagResponse.getTag());
        } catch (BmcException e) {
            throw new HttpException(e.getMessage(), toStatus(e), e);
        }
    }

    private byte[] authorizeTags(AuthorizationRequest authorizationRequest,
                                 String compartmentId,
                                 byte[] requestedTagSlug) throws AuthServerUnavailableException {
        AuthorizationRequest request = AuthorizationRequestFactory.copyOf(authorizationRequest);
        request.setActionKind(ActionKind.CREATE);
        request.addCompartmentId(compartmentId);
        request.addVariable(OptionalVariableFactory.resourceKind(RESOURCE_KIND));

        AuthorizationRequest taggedRequest = AuthorizationRequestFactory.setNewTags(request, requestedTagSlug);
        AuthorizationResponse response = authorizationClient.makeAuthorizationCall(taggedRequest);
        requireAuthorized(response);

        return response.getTagSlug()
                .orElseThrow(() -> new HttpException("Authorization response did not include a tag slug",
                                                     Status.INTERNAL_SERVER_ERROR_500));
    }

    private static void requireAuthorized(AuthorizationResponse response) {
        if (!response.authorizeAllPermissions()) {
            throw new HttpException("Not authorized", Status.NOT_FOUND_404);
        }
        if (!response.authorizeTags()) {
            String message = response.getTagErrorMessage().orElse("Tag authorization failed");
            AuthorizationResponseErrorCategory category = response.getAuthorizationResponseResult().getErrorCategory();
            Status status = category == AuthorizationResponseErrorCategory.TAG_VALIDATION_ERROR
                    ? Status.BAD_REQUEST_400
                    : Status.NOT_FOUND_404;
            throw new HttpException(message, status);
        }
    }

    private static CreateTagRequest createTagRequest(CreateTagDefinitionRequest request) {
        return CreateTagRequest.builder()
                .tagNamespaceId(request.tagNamespaceId())
                .createTagDetails(CreateTagDetails.builder()
                                          .name(request.tagName())
                                          .description(request.description())
                                          .freeformTags(request.freeformTags())
                                          .definedTags(request.definedTags())
                                          .isCostTracking(request.costTracking())
                                          .build())
                .build();
    }

    private static TagDefinitionView toTagDefinition(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new TagDefinitionView(tag.getId(),
                                     tag.getName(),
                                     tag.getDescription(),
                                     tag.getLifecycleState() == null ? null : tag.getLifecycleState().getValue());
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

    private static ResourceTags toResourceTags(TagSet tags) {
        return new ResourceTags(tags.getFreeformTags().map(FreeformTags::getTags).orElseGet(Map::of),
                                tags.getDefinedTags().map(DefinedTags::getTags).orElseGet(Map::of),
                                tags.getSystemTags().map(SystemTags::getTags).orElseGet(Map::of));
    }

    private static Status toStatus(BmcException e) {
        int statusCode = e.getStatusCode();
        return statusCode > 0 ? Status.create(statusCode) : Status.INTERNAL_SERVER_ERROR_500;
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}

@Json.Entity
@Validation.Validated
record CreateTaggedResourceRequest(@Validation.NotNull
                                   @Validation.String.NotEmpty
                                   String resourceId,
                                   @Validation.NotNull
                                   @Validation.String.NotEmpty
                                   String compartmentId,
                                   @Validation.Valid
                                   ResourceTags tags) {
    @Override
    public ResourceTags tags() {
        return tags == null ? ResourceTags.empty() : tags;
    }
}

@Json.Entity
@Validation.Validated
record CreateTagDefinitionRequest(@Validation.NotNull
                                  @Validation.String.NotEmpty
                                  String tagNamespaceId,
                                  @Validation.NotNull
                                  @Validation.String.NotEmpty
                                  String tagName,
                                  @Validation.NotNull
                                  @Validation.String.NotEmpty
                                  String description,
	                                  Boolean costTracking,
	                                  Map<String, String> freeformTags,
	                                  Map<String, Map<String, Object>> definedTags) {
    @Override
    public Boolean costTracking() {
        return costTracking == null ? Boolean.FALSE : costTracking;
    }

    @Override
    public Map<String, String> freeformTags() {
        return freeformTags == null ? Map.of() : freeformTags;
    }

    @Override
    public Map<String, Map<String, Object>> definedTags() {
        return definedTags == null ? Map.of() : definedTags;
    }
}

@Json.Entity
record TaggedResourceView(String resourceId,
                          String compartmentId,
                          String tagSlug,
                          ResourceTags tags) {
}

@Json.Entity
record TagDefinitionView(String id,
                         String name,
                         String description,
                         String lifecycleState) {
}

@Json.Entity
record ResourceTags(Map<String, String> freeformTags,
                    Map<String, Map<String, Object>> definedTags,
                    Map<String, Map<String, Object>> systemTags) {
    @Override
    public Map<String, String> freeformTags() {
        return freeformTags == null ? Map.of() : freeformTags;
    }

    @Override
    public Map<String, Map<String, Object>> definedTags() {
        return definedTags == null ? Map.of() : definedTags;
    }

    @Override
    public Map<String, Map<String, Object>> systemTags() {
        return systemTags == null ? Map.of() : systemTags;
    }

    static ResourceTags empty() {
        return new ResourceTags(Map.of(), Map.of(), Map.of());
    }
}

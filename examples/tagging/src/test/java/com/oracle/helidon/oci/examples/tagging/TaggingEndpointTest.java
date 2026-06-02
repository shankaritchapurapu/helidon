/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.json.binding.JsonBinding;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;

import com.oracle.bmc.identity.model.CreateTagDetails;
import com.oracle.bmc.identity.model.Tag;
import com.oracle.bmc.identity.requests.CreateTagRequest;
import com.oracle.bmc.identity.responses.CreateTagResponse;
import com.oracle.pic.identity.authentication.PrincipalImpl;
import com.oracle.pic.identity.authentication.SecurityContextImpl;
import com.oracle.pic.identity.authorization.permissions.Permission;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.pic.identity.authorization.sdk.AuthorizationResponse;
import com.oracle.pic.identity.authorization.sdk.request.AuthorizationRequestTags.TagActionKind;
import com.oracle.pic.identity.authorization.sdk.response.AuthorizationResponseResult;
import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.entities.TaggingClientImpl;
import com.oracle.pic.tagging.client.tag.TagSet;
import com.oracle.pic.tagging.common.tagset.tagslice.DefinedTags;
import com.oracle.pic.tagging.common.tagset.tagslice.FreeformTags;
import com.oracle.pic.tagging.common.tagset.tagslice.SystemTags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class TaggingEndpointTest {
    private static final JsonBinding JSON_BINDING = JsonBinding.create();
    private static final TaggingClient TAGGING_CLIENT = TaggingClientImpl.builder()
            .emitMetrics(false)
            .build();
    private static final String RESOURCE_ID = "ocid1.exampletaggedresource.oc1..example";
    private static final String COMPARTMENT_ID = "ocid1.compartment.oc1..example";
    private static final String TAG_NAMESPACE_ID = "ocid1.tagnamespace.oc1..example";

    private final Http1Client client;

    TaggingEndpointTest(Http1Client client) {
        this.client = client;
    }

    @BeforeEach
    void setUp() throws Exception {
        TaggingEndpointTestServices.reset();
        var principal = new PrincipalImpl("ocid1.tenancy.oc1..example", "ocid1.user.oc1..example");
        var securityContext = SecurityContextImpl.success(principal);
        TaggingEndpointTestServices.AUTHENTICATOR.securityContext(securityContext);

        AuthorizationResponse authorizationResponse = authorizationResponse(TAGGING_CLIENT.createEmptyTagSlug());
        TaggingEndpointTestServices.AUTHORIZATION.authorizationResponse(authorizationResponse);

        Tag tag = Tag.builder()
                .id("ocid1.tag.oc1..example")
                .tagNamespaceId(TAG_NAMESPACE_ID)
                .name("CostCenter")
                .description("Cost center tag")
                .lifecycleState(Tag.LifecycleState.Active)
                .build();
        TaggingEndpointTestServices.IDENTITY.createTagResponse(CreateTagResponse.builder()
                                                                    .tag(tag)
                                                                    .build());
    }

    @Test
    void testCreatesTaggedResourceThroughAuthorization() throws Exception {
        ResourceTags tags = new ResourceTags(
                Map.of("owner", "platform",
                       "environment", "dev"),
                Map.of("Operations",
                       Map.<String, Object>of("CostCenter", "42")),
                Map.of("orcl-cloud",
                       Map.<String, Object>of("free-tier-retained", "true")));
        ResourceTags authorizedTags = new ResourceTags(
                tags.freeformTags(),
                Map.of("Operations",
                       Map.<String, Object>of("CostCenter", "42",
                                              "DefaultCostCenter", "84")),
                tags.systemTags());
        byte[] authorizedSlug = tagSlug(authorizedTags);
        AuthorizationResponse authorizationResponse = authorizationResponse(authorizedSlug);
        TaggingEndpointTestServices.AUTHORIZATION.authorizationResponse(authorizationResponse);
        CreateTaggedResourceRequest payload = new CreateTaggedResourceRequest(RESOURCE_ID,
                                                                              COMPARTMENT_ID,
                                                                              tags);

        try (Http1ClientResponse response = client.post("/tagging/resources")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(payload))) {
            assertThat(response.status(), is(Status.OK_200));

            TaggedResourceView resource = JSON_BINDING.deserialize(response.as(String.class), TaggedResourceView.class);
            assertThat(resource.resourceId(), is(RESOURCE_ID));
            assertThat(resource.compartmentId(), is(COMPARTMENT_ID));
            assertThat(resource.tagSlug(), is(Base64.getEncoder().encodeToString(authorizedSlug)));
            assertThat(resource.tags(), is(authorizedTags));
        }

        assertThat(TaggingEndpointTestServices.IDENTITY.createTagRequests().isEmpty(), is(true));

        List<AuthorizationRequest> authorizationRequests =
                TaggingEndpointTestServices.AUTHORIZATION.authorizationRequests();
        AuthorizationRequest authorizationRequest = authorizationRequests.get(authorizationRequests.size() - 1);
        assertThat(authorizationRequest.getTargetCompartmentId().orElseThrow(), is(COMPARTMENT_ID));
        assertThat(authorizationRequest.getPermissions(), hasItem(Permission.get(TaggingEndpoint.CREATE_PERMISSION)));
        assertThat(authorizationRequest.mutable().getTags().getTagActionKind(), is(TagActionKind.TAGS_START));
        assertThat(authorizationRequest.mutable().getTags().getPrimarySlug().isPresent(), is(true));
    }

    @Test
    void testCreatesTagDefinitionThroughIdentity() throws Exception {
        CreateTagDefinitionRequest payload = new CreateTagDefinitionRequest(TAG_NAMESPACE_ID,
                                                                            "CostCenter",
                                                                            "Cost center tag",
                                                                            false,
                                                                            Map.of("owner", "platform"),
                                                                            Map.of());

        try (Http1ClientResponse response = client.post("/tagging/tag-definitions")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(payload))) {
            assertThat(response.status(), is(Status.OK_200));

            TagDefinitionView tagDefinition = JSON_BINDING.deserialize(response.as(String.class),
                                                                       TagDefinitionView.class);
            assertThat(tagDefinition.id(), is("ocid1.tag.oc1..example"));
            assertThat(tagDefinition.name(), is("CostCenter"));
            assertThat(tagDefinition.lifecycleState(), is("ACTIVE"));
        }

        List<CreateTagRequest> createTagRequests = TaggingEndpointTestServices.IDENTITY.createTagRequests();
        assertThat(createTagRequests.size(), is(1));
        CreateTagRequest createTagRequest = createTagRequests.get(0);
        CreateTagDetails createTagDetails = createTagRequest.getCreateTagDetails();
        assertThat(createTagRequest.getTagNamespaceId(), is(TAG_NAMESPACE_ID));
        assertThat(createTagDetails.getName(), is("CostCenter"));
        assertThat(createTagDetails.getDescription(), is("Cost center tag"));
        assertThat(createTagDetails.getFreeformTags().get("owner"), is("platform"));
        assertThat(createTagDetails.getIsCostTracking(), is(false));

        List<AuthorizationRequest> authorizationRequests =
                TaggingEndpointTestServices.AUTHORIZATION.preAuthorizeRequests();
        AuthorizationRequest authorizationRequest = authorizationRequests.get(authorizationRequests.size() - 1);
        assertThat(authorizationRequest.getPermissions(),
                   hasItem(Permission.get(TaggingEndpoint.CREATE_TAG_PERMISSION)));
    }

    @Test
    void testRejectsInvalidRequest() {
        CreateTaggedResourceRequest payload = new CreateTaggedResourceRequest("",
                                                                              COMPARTMENT_ID,
                                                                              ResourceTags.empty());

        try (Http1ClientResponse response = client.post("/tagging/resources")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(payload))) {
            assertThat(response.status(), is(Status.BAD_REQUEST_400));
        }
    }

    private static AuthorizationResponse authorizationResponse(byte[] authorizedSlug) {
        return new TestAuthorizationResponse(authorizedSlug);
    }

    private static byte[] tagSlug(ResourceTags tags) {
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
        return TAGGING_CLIENT.toByteArray(builder.build());
    }

    private record TestAuthorizationResponse(byte[] tagSlug) implements AuthorizationResponse {
        @Override
        public boolean authorizeAnyPermission() {
            return true;
        }

        @Override
        public boolean authorizeAllPermissions() {
            return true;
        }

        @Override
        public boolean authorizeSetOfPermissions(Set<Permission> permissions) {
            return true;
        }

        @Override
        public boolean authorizeTags() {
            return true;
        }

        @Override
        public Optional<String> getTagErrorMessage() {
            return Optional.empty();
        }

        @Override
        public Optional<byte[]> getTagSlug() {
            return Optional.of(tagSlug);
        }

        @Override
        public Set<Permission> getPermissions() {
            return Set.of(Permission.get(TaggingEndpoint.CREATE_PERMISSION));
        }

        @Override
        public String getRequestId() {
            return "authz-request-1";
        }

        @Override
        public AuthorizationResponseResult getAuthorizationResponseResult() {
            return AuthorizationResponseResult.noErrorResult();
        }
    }
}

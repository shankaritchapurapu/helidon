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
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        reset(TaggingEndpointTestServices.AUTHENTICATOR,
              TaggingEndpointTestServices.AUTHORIZATION,
              TaggingEndpointTestServices.IDENTITY);

        var principal = new PrincipalImpl("ocid1.tenancy.oc1..example", "ocid1.user.oc1..example");
        var securityContext = SecurityContextImpl.success(principal);
        when(TaggingEndpointTestServices.AUTHENTICATOR.authenticate(anyString(),
                                                                    any(URI.class),
                                                                    anyMap(),
                                                                    any()))
                .thenReturn(securityContext);
        when(TaggingEndpointTestServices.AUTHENTICATOR.authenticate(anyString(),
                                                                    any(URI.class),
                                                                    anyMap(),
                                                                    any(),
                                                                    anyBoolean()))
                .thenReturn(securityContext);

        AuthorizationResponse authorizationResponse = authorizationResponse(TAGGING_CLIENT.createEmptyTagSlug());

        when(TaggingEndpointTestServices.AUTHORIZATION.getServiceName()).thenReturn("helidon-tagging-example");
        when(TaggingEndpointTestServices.AUTHORIZATION.getRegion()).thenReturn("us-ashburn-1");
        when(TaggingEndpointTestServices.AUTHORIZATION.getPhysicalAD()).thenReturn("AD-1");
        when(TaggingEndpointTestServices.AUTHORIZATION.preAuthorize(any())).thenReturn(true);
        when(TaggingEndpointTestServices.AUTHORIZATION.authorizeAll(any())).thenReturn(true);
        when(TaggingEndpointTestServices.AUTHORIZATION.makeAuthorizationCall(any(AuthorizationRequest.class)))
                .thenReturn(authorizationResponse);
        when(TaggingEndpointTestServices.AUTHORIZATION.makeAuthorizationCall(any(AuthorizationRequest.class), anyString()))
                .thenReturn(authorizationResponse);

        when(TaggingEndpointTestServices.IDENTITY.createTag(any(CreateTagRequest.class)))
                .thenReturn(CreateTagResponse.builder()
                                    .tag(Tag.builder()
                                                 .id("ocid1.tag.oc1..example")
                                                 .tagNamespaceId(TAG_NAMESPACE_ID)
                                                 .name("CostCenter")
                                                 .description("Cost center tag")
                                                 .lifecycleState(Tag.LifecycleState.Active)
                                                 .build())
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
        when(TaggingEndpointTestServices.AUTHORIZATION.makeAuthorizationCall(any(AuthorizationRequest.class)))
                .thenReturn(authorizationResponse);
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

        verify(TaggingEndpointTestServices.IDENTITY, never()).createTag(any(CreateTagRequest.class));

        ArgumentCaptor<AuthorizationRequest> authorizationCaptor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(TaggingEndpointTestServices.AUTHORIZATION, atLeastOnce())
                .makeAuthorizationCall(authorizationCaptor.capture());
        AuthorizationRequest authorizationRequest = authorizationCaptor.getAllValues()
                .get(authorizationCaptor.getAllValues().size() - 1);
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

            TagDefinitionView tagDefinition = JSON_BINDING.deserialize(response.as(String.class), TagDefinitionView.class);
            assertThat(tagDefinition.id(), is("ocid1.tag.oc1..example"));
            assertThat(tagDefinition.name(), is("CostCenter"));
            assertThat(tagDefinition.lifecycleState(), is("ACTIVE"));
        }

        ArgumentCaptor<CreateTagRequest> createTagCaptor = ArgumentCaptor.forClass(CreateTagRequest.class);
        verify(TaggingEndpointTestServices.IDENTITY).createTag(createTagCaptor.capture());
        CreateTagRequest createTagRequest = createTagCaptor.getValue();
        CreateTagDetails createTagDetails = createTagRequest.getCreateTagDetails();
        assertThat(createTagRequest.getTagNamespaceId(), is(TAG_NAMESPACE_ID));
        assertThat(createTagDetails.getName(), is("CostCenter"));
        assertThat(createTagDetails.getDescription(), is("Cost center tag"));
        assertThat(createTagDetails.getFreeformTags().get("owner"), is("platform"));
        assertThat(createTagDetails.getIsCostTracking(), is(false));

        ArgumentCaptor<AuthorizationRequest> authorizationCaptor = ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(TaggingEndpointTestServices.AUTHORIZATION, atLeastOnce())
                .preAuthorize(authorizationCaptor.capture());
        AuthorizationRequest authorizationRequest = authorizationCaptor.getAllValues()
                .get(authorizationCaptor.getAllValues().size() - 1);
        assertThat(authorizationRequest.getPermissions(), hasItem(Permission.get(TaggingEndpoint.CREATE_TAG_PERMISSION)));
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

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> anyMap() {
        return any(Map.class);
    }

    private static AuthorizationResponse authorizationResponse(byte[] authorizedSlug) {
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        when(authorizationResponse.authorizeAllPermissions()).thenReturn(true);
        when(authorizationResponse.authorizeTags()).thenReturn(true);
        when(authorizationResponse.getTagSlug()).thenReturn(Optional.of(authorizedSlug));
        when(authorizationResponse.getPermissions()).thenReturn(Set.of(Permission.get(TaggingEndpoint.CREATE_PERMISSION)));
        when(authorizationResponse.getRequestId()).thenReturn("authz-request-1");
        when(authorizationResponse.getAuthorizationResponseResult())
                .thenReturn(AuthorizationResponseResult.noErrorResult());
        return authorizationResponse;
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
}

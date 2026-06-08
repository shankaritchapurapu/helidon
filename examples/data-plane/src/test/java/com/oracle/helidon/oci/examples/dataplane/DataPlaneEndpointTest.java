/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.json.binding.JsonBinding;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import com.oracle.helidon.oci.errorcode.ErrorCodes;
import com.oracle.helidon.oci.errorcode.ErrorDetail;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class DataPlaneEndpointTest {
    private static final JsonBinding JSON_BINDING = JsonBinding.create();
    private static final HeaderName REQUEST_ID_HEADER = HeaderNames.create("opc-request-id");

    private final Http1Client client;

    DataPlaneEndpointTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void testStatusIncludesRequestId() {
        try (Http1ClientResponse response = client.get("/data-plane")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));

            String requestId = response.headers().first(REQUEST_ID_HEADER).orElseThrow();
            DataPlaneStatus status = JSON_BINDING.deserialize(response.as(String.class), DataPlaneStatus.class);

            assertThat(status.serviceName(), is("helidon-data-plane-example"));
            assertThat(status.requestId(), is(requestId));
            assertThat(status.auditEnabled(), is(true));
            assertThat(status.robotCount(), is(1));
        }
    }

    @Test
    void testListAndGetSeedRobot() {
        try (Http1ClientResponse response = client.get("/data-plane/robots")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            RobotCollection collection = JSON_BINDING.deserialize(response.as(String.class), RobotCollection.class);
            assertThat(collection.count(), is(1));
            assertThat(collection.items().stream().map(RobotSummary::id).toList(), hasItem("robot-1"));
        }

        try (Http1ClientResponse response = client.get("/data-plane/robots/robot-1")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            RobotResponse robotResponse = JSON_BINDING.deserialize(response.as(String.class), RobotResponse.class);
            Robot robot = robotResponse.payload();
            assertThat(robotResponse.responseStatus(), is(Status.OK_200.code()));
            assertThat(robotResponse.error(), is(nullValue()));
            assertThat(robot.id(), is("robot-1"));
            assertThat(robot.displayName(), is("Reference Robot"));
            assertThat(robot.compartmentId(), is("ocid1.compartment.oc1..example"));
            assertThat(response.headers().first(REQUEST_ID_HEADER).orElseThrow(), is(not(nullValue())));
        }
    }

    @Test
    void testMissingRobotUsesOciErrorCode() {
        try (Http1ClientResponse response = client.get("/data-plane/robots/missing")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.NOT_FOUND_404));
            RobotResponse robotResponse = JSON_BINDING.deserialize(response.as(String.class), RobotResponse.class);
            assertThat(robotResponse.responseStatus(), is(Status.NOT_FOUND_404.code()));
            assertThat(robotResponse.payload(), is(nullValue()));
            assertThat(robotResponse.error().code(), is("NotAuthorizedOrNotFound"));
            assertThat(robotResponse.error().message(), is("No robot found for id: missing"));
            assertThat(response.headers().first(REQUEST_ID_HEADER).orElseThrow(), is(not(nullValue())));
        }
    }

    @Test
    void testRenderableExceptionUsesAutomaticMapper() {
        try (Http1ClientResponse response = client.get("/data-plane/errors/renderable")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.BAD_REQUEST_400));
            ErrorDetail error = JSON_BINDING.deserialize(response.as(String.class), ErrorDetail.class);

            assertThat(error.getErrorCode(), is(ErrorCodes.InvalidParameter.errorCode()));
            assertThat(error.getMessage(), is("RenderableException mapped by helidon-oci-errorcode-webserver"));
        }
    }

    @Test
    void testProbe() {
        try (Http1ClientResponse response = client.get("/data-plane/probe")
                .header(HeaderValues.ACCEPT_TEXT)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is("Data-plane operational probe passed"));
        }
    }
}

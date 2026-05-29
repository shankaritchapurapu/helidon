/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.net.http.HttpResponse;

import io.helidon.json.binding.JsonBinding;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class DataPlaneEndpointHardCodedTest extends SignedRequestSupport {
    private static final JsonBinding JSON_BINDING = JsonBinding.create();

    DataPlaneEndpointHardCodedTest(WebServer webServer) {
        super(webServer);
    }

    @Test
    void testCreateAndUpdateRobot() throws Exception {
        String createBody = "{\"displayName\":\"Signed Robot\",\"compartmentId\":\"ocid1.compartment.oc1..signed\"}";
        HttpResponse<String> createResponse = signedJson("/data-plane/robots", "POST", createBody);
        assertThat(createResponse.statusCode(), is(200));

        RobotResponse createPayload = JSON_BINDING.deserialize(createResponse.body(), RobotResponse.class);
        assertThat(createPayload.responseStatus(), is(200));
        assertThat(createPayload.error(), is(nullValue()));
        Robot created = createPayload.payload();
        assertThat(created.displayName(), is("Signed Robot"));
        assertThat(created.compartmentId(), is("ocid1.compartment.oc1..signed"));

        String updateBody = "{\"displayName\":\"Updated Signed Robot\"}";
        HttpResponse<String> updateResponse = signedJson("/data-plane/robots/" + created.id(), "PUT", updateBody);
        assertThat(updateResponse.statusCode(), is(200));

        RobotResponse updatePayload = JSON_BINDING.deserialize(updateResponse.body(), RobotResponse.class);
        assertThat(updatePayload.responseStatus(), is(200));
        assertThat(updatePayload.error(), is(nullValue()));
        Robot updated = updatePayload.payload();
        assertThat(updated.id(), is(created.id()));
        assertThat(updated.displayName(), is("Updated Signed Robot"));
    }

    @Test
    void testDeleteRemovesRobot() throws Exception {
        HttpResponse<String> deleteResponse = signedDelete("/data-plane/robots/robot-1");
        assertThat(deleteResponse.statusCode(), is(200));
        RobotResponse deletePayload = JSON_BINDING.deserialize(deleteResponse.body(), RobotResponse.class);
        assertThat(deletePayload.responseStatus(), is(200));
        assertThat(deletePayload.error(), is(nullValue()));
        assertThat(deletePayload.payload().id(), is("robot-1"));

        HttpResponse<String> getResponse = signedJson("/data-plane/robots/robot-1", "GET", null);
        assertThat(getResponse.statusCode(), is(404));

        RobotResponse getPayload = JSON_BINDING.deserialize(getResponse.body(), RobotResponse.class);
        assertThat(getPayload.responseStatus(), is(404));
        assertThat(getPayload.error().code(), is("NotAuthorizedOrNotFound"));
        assertThat(getPayload.error().message(), is("No robot found for id: robot-1"));
    }
}

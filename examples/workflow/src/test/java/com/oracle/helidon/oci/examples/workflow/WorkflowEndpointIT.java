/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.workflow;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.json.binding.JsonBinding;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;
import com.oracle.pic.workflow.client.v1.model.WorkflowStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class WorkflowEndpointIT {
    private static final JsonBinding JSON_BINDING = JsonBinding.create();
    private static final Set<String> VALID_WORKFLOW_STATUSES = Arrays.stream(WorkflowStatus.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private final Http1Client client;

    WorkflowEndpointIT(Http1Client client) {
        this.client = client;
    }

    @Test
    void testLaunchAndReadWorkflowThroughConfiguredClient() {
        Map<String, String> created;
        try (Http1ClientResponse response = client.post("/workflow/instances")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit(JSON_BINDING.serialize(new LaunchWorkflowRequest("ocid1.instance.oc1..example", false)))) {
            assertThat(response.status(), is(Status.OK_200));
            created = parse(response.as(String.class));
        }

        assertWorkflowSnapshot(created);

        try (Http1ClientResponse response = client.get("/workflow/instances/" + created.get("workflowInstanceId"))
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            Map<String, String> fetched = parse(response.as(String.class));
            assertWorkflowSnapshot(fetched);
            assertThat(fetched.get("workflowInstanceId"), is(created.get("workflowInstanceId")));
        }
    }

    private static void assertWorkflowSnapshot(Map<String, String> snapshot) {
        assertThat(snapshot.keySet(), hasItem("workflowInstanceId"));
        assertThat(snapshot.keySet(), hasItem("status"));
        assertThat(snapshot.keySet(), hasItem("tag"));
        assertThat(snapshot.get("workflowInstanceId"), is(not(nullValue())));
        assertThat(snapshot.get("workflowInstanceId").isBlank(), is(false));
        assertThat(snapshot.get("status"), is(not(nullValue())));
        assertThat(VALID_WORKFLOW_STATUSES.contains(snapshot.get("status")), is(true));
        assertThat(snapshot.get("tag"), is(not(nullValue())));
    }

    private static Map<String, String> parse(String body) {
        WorkflowSnapshot snapshot = JSON_BINDING.deserialize(body, WorkflowSnapshot.class);
        return Map.of("workflowInstanceId", snapshot.workflowInstanceId(),
                      "status", snapshot.status(),
                      "tag", snapshot.tag());
    }
}

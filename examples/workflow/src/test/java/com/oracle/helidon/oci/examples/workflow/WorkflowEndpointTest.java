/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.workflow;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import io.helidon.json.binding.JsonBinding;

import com.oracle.pic.workflow.client.v1.model.LaunchWorkflowArguments;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinitionId;
import com.oracle.pic.workflow.client.v1.model.WorkflowInstance;
import com.oracle.pic.workflow.client.v1.model.WorkflowStatus;
import com.oracle.pic.workflow.worker.WorkflowClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RoutingTest
class WorkflowEndpointTest {
    private static final JsonBinding JSON_BINDING = JsonBinding.create();
    private final DirectClient client;

    WorkflowEndpointTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        ProvisioningWorkflowService workflowService =
                new ProvisioningWorkflowService(new DriverStyleWorkflowClient().client());
        WorkflowEndpoint endpoint = new WorkflowEndpoint(workflowService);
        WorkflowResource resource = new WorkflowResource(endpoint);
        router.post("/workflow/instances", resource::launch)
                .get("/workflow/instances/{id}", resource::get);
    }

    @Test
    void testLaunchAndAdvanceHappyWorkflow() {
        Map<String, String> created = assertSnapshot(client.post("/workflow/instances")
                                                             .header(HeaderValues.CONTENT_TYPE_JSON)
                                                             .header(HeaderValues.ACCEPT_JSON)
                                                             .submit(JSON_BINDING.serialize(new LaunchWorkflowRequest(
                                                                     "ocid1.instance.oc1..example",
                                                                     false))),
                                                     Status.OK_200,
                                                     "Created",
                                                     "validate");
        String workflowId = created.get("workflowInstanceId");
        assertSnapshot(client.get("/workflow/instances/" + workflowId)
                               .header(HeaderValues.ACCEPT_JSON)
                               .request(),
                       Status.OK_200,
                       "Running",
                       "provision");
        assertSnapshot(client.get("/workflow/instances/" + workflowId)
                               .header(HeaderValues.ACCEPT_JSON)
                               .request(),
                       Status.OK_200,
                       "Running",
                       "finalize");
        assertSnapshot(client.get("/workflow/instances/" + workflowId)
                               .header(HeaderValues.ACCEPT_JSON)
                               .request(),
                       Status.OK_200,
                       "Complete",
                       "complete");
    }

    @Test
    void testLaunchAndAdvanceFailingWorkflow() {
        Map<String, String> created = assertSnapshot(client.post("/workflow/instances")
                                                             .header(HeaderValues.CONTENT_TYPE_JSON)
                                                             .header(HeaderValues.ACCEPT_JSON)
                                                             .submit(JSON_BINDING.serialize(new LaunchWorkflowRequest(
                                                                     "ocid1.instance.oc1..example",
                                                                     true))),
                                                     Status.OK_200,
                                                     "Created",
                                                     "validate");
        String workflowId = created.get("workflowInstanceId");
        assertSnapshot(client.get("/workflow/instances/" + workflowId)
                               .header(HeaderValues.ACCEPT_JSON)
                               .request(),
                       Status.OK_200,
                       "Running",
                       "provision");
        assertSnapshot(client.get("/workflow/instances/" + workflowId)
                               .header(HeaderValues.ACCEPT_JSON)
                               .request(),
                       Status.OK_200,
                       "Running",
                       "cleanup");
        assertSnapshot(client.get("/workflow/instances/" + workflowId)
                               .header(HeaderValues.ACCEPT_JSON)
                               .request(),
                       Status.OK_200,
                       "Failed",
                       "failed");
    }

    @Test
    void testRejectsMissingResourceId() {
        try (Http1ClientResponse response = client.post("/workflow/instances")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .header(HeaderValues.ACCEPT_JSON)
                .submit("{\"simulateFailure\":false}")) {
            assertEquals(Status.BAD_REQUEST_400, response.status(), "Response status should match");
        }
    }

    @Test
    void testLaunchEscapesJsonPayload() {
        AtomicReference<LaunchWorkflowArguments> capturedLaunchArguments = new AtomicReference<>();
        ProvisioningWorkflowService workflowService = new ProvisioningWorkflowService(capturingClient(capturedLaunchArguments));

        workflowService.launch("ocid1.instance.oc1..exa\"mple\\slash", true);

        String json = new String(capturedLaunchArguments.get().getWorkflowArguments(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\\\""), "Serialized JSON should escape embedded quotes");
        assertTrue(json.contains("\\\\"), "Serialized JSON should escape backslashes");
    }

    private static Map<String, String> assertSnapshot(Http1ClientResponse response,
                                                      Status expectedStatus,
                                                      String expectedWorkflowStatus,
                                                      String expectedTag) {
        try (response) {
            assertEquals(expectedStatus, response.status(), "Response status should match");
            Map<String, String> fields = parse(response.entity().as(String.class));
            assertEquals(expectedWorkflowStatus, fields.get("status"), "Workflow status should match");
            assertEquals(expectedTag, fields.get("tag"), "Workflow tag should match");
            return fields;
        }
    }

    private static WorkflowClient capturingClient(AtomicReference<LaunchWorkflowArguments> capturedLaunchArguments) {
        return (WorkflowClient) Proxy.newProxyInstance(
                WorkflowClient.class.getClassLoader(),
                new Class<?>[]{WorkflowClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDomainId" -> "compute-control-plane";
                    case "launchWorkflow" -> {
                        capturedLaunchArguments.set((LaunchWorkflowArguments) args[0]);
                        yield WorkflowInstance.builder()
                                .id("wf-captured")
                                .status(WorkflowStatus.Created)
                                .workflowDefinitionId(WorkflowDefinitionId.builder()
                                                              .name("instance-create")
                                                              .majorVersion(3)
                                                              .minorVersion(1)
                                                              .build())
                                .build();
                    }
                    case "toString" -> "CapturingWorkflowClient";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                });
    }

    private static Map<String, String> parse(String body) {
        WorkflowSnapshot snapshot = JSON_BINDING.deserialize(body, WorkflowSnapshot.class);
        return Map.of("workflowInstanceId", snapshot.workflowInstanceId(),
                      "status", snapshot.status(),
                      "tag", snapshot.tag());
    }

    private static final class WorkflowResource {
        private final WorkflowEndpoint endpoint;

        private WorkflowResource(WorkflowEndpoint endpoint) {
            this.endpoint = endpoint;
        }

        private void launch(ServerRequest req, ServerResponse res) {
            res.send(endpoint.launch(req.content().as(LaunchWorkflowRequest.class)));
        }

        private void get(ServerRequest req, ServerResponse res) {
            res.send(endpoint.get(req.path().pathParameters().get("id")));
        }
    }

    /**
     * A test-only harness inspired by the WFaaS test-driver pattern: each GET request advances the
     * stored workflow scenario one step further.
     */
    private static final class DriverStyleWorkflowClient {
        private final AtomicInteger idSequence = new AtomicInteger();
        private final Map<String, ScenarioState> scenarios = new ConcurrentHashMap<>();
        private final WorkflowClient client = (WorkflowClient) Proxy.newProxyInstance(
                WorkflowClient.class.getClassLoader(),
                new Class<?>[]{WorkflowClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDomainId" -> "compute-control-plane";
                    case "launchWorkflow" -> launch((LaunchWorkflowArguments) args[0]);
                    case "getWorkflowInstance" -> advance((String) args[0]);
                    case "toString" -> "DriverStyleWorkflowClient";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                });

        private WorkflowClient client() {
            return client;
        }

        private WorkflowInstance launch(LaunchWorkflowArguments arguments) {
            String workflowInstanceId = "wf-" + idSequence.incrementAndGet();
            boolean simulateFailure = new String(arguments.getWorkflowArguments(), StandardCharsets.UTF_8)
                    .contains("\"simulateFailure\":true");
            ScenarioState state = new ScenarioState(arguments.getWorkflowDefinitionId(), simulateFailure);
            scenarios.put(workflowInstanceId, state);
            return state.snapshot(workflowInstanceId);
        }

        private WorkflowInstance advance(String workflowInstanceId) {
            ScenarioState state = scenarios.get(workflowInstanceId);
            if (state == null) {
                throw new IllegalArgumentException("Unknown workflow instance: " + workflowInstanceId);
            }
            state.advance();
            return state.snapshot(workflowInstanceId);
        }
    }

    private static final class ScenarioState {
        private final WorkflowDefinitionId workflowDefinitionId;
        private final boolean simulateFailure;
        private WorkflowStatus status = WorkflowStatus.Created;
        private String stepName = "validate";

        private ScenarioState(WorkflowDefinitionId workflowDefinitionId, boolean simulateFailure) {
            this.workflowDefinitionId = workflowDefinitionId;
            this.simulateFailure = simulateFailure;
        }

        private void advance() {
            if (status == WorkflowStatus.Complete || status == WorkflowStatus.Failed) {
                return;
            }
            if (status == WorkflowStatus.Created) {
                status = WorkflowStatus.Running;
                stepName = "provision";
                return;
            }
            if ("provision".equals(stepName)) {
                stepName = simulateFailure ? "cleanup" : "finalize";
                return;
            }
            if ("finalize".equals(stepName)) {
                status = WorkflowStatus.Complete;
                stepName = "complete";
                return;
            }
            if ("cleanup".equals(stepName)) {
                status = WorkflowStatus.Failed;
                stepName = "failed";
            }
        }

        private WorkflowInstance snapshot(String workflowInstanceId) {
            return WorkflowInstance.builder()
                    .id(workflowInstanceId)
                    .status(status)
                    .tag(stepName)
                    .workflowDefinitionId(workflowDefinitionId)
                    .build();
        }
    }
}

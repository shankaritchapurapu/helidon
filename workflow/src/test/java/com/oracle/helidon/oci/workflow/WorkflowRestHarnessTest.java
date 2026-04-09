/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.oracle.pic.workflow.client.v1.model.LaunchWorkflowArguments;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinitionId;
import com.oracle.pic.workflow.client.v1.model.WorkflowInstance;
import com.oracle.pic.workflow.client.v1.model.WorkflowStatus;
import com.oracle.pic.workflow.worker.WorkflowClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RoutingTest
class WorkflowRestHarnessTest {
    private final DirectClient client;

    WorkflowRestHarnessTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        WorkflowProgressResource resource = new WorkflowProgressResource(new DriverStyleWorkflowClient().client());
        router.post("/workflow/happy", resource::startHappy)
                .post("/workflow/failing", resource::startFailing)
                .get("/workflow/{id}", resource::advanceAndFetch);
    }

    @Test
    void restClientAdvancesHappyWorkflowAcrossRequests() {
        assertSnapshot(client.post("/workflow/happy").submit(""), Status.CREATED_201, "wf-1|Created|validate");
        assertSnapshot(client.get("/workflow/wf-1").request(), Status.OK_200, "wf-1|Running|provision");
        assertSnapshot(client.get("/workflow/wf-1").request(), Status.OK_200, "wf-1|Running|finalize");
        assertSnapshot(client.get("/workflow/wf-1").request(), Status.OK_200, "wf-1|Complete|complete");
    }

    @Test
    void restClientAdvancesFailingWorkflowIntoCleanupAndFailure() {
        assertSnapshot(client.post("/workflow/failing").submit(""), Status.CREATED_201, "wf-2|Created|validate");
        assertSnapshot(client.get("/workflow/wf-2").request(), Status.OK_200, "wf-2|Running|provision");
        assertSnapshot(client.get("/workflow/wf-2").request(), Status.OK_200, "wf-2|Running|cleanup");
        assertSnapshot(client.get("/workflow/wf-2").request(), Status.OK_200, "wf-2|Failed|failed");
    }

    private static void assertSnapshot(Http1ClientResponse response, Status expectedStatus, String expectedBody) {
        assertEquals(expectedStatus, response.status(), "Response status should match");
        assertEquals(expectedBody, response.entity().as(String.class), "Response body should match");
    }

    private static final class WorkflowProgressResource {
        private final WorkflowClient workflowClient;

        private WorkflowProgressResource(WorkflowClient workflowClient) {
            this.workflowClient = workflowClient;
        }

        private void startHappy(ServerRequest req, ServerResponse res) {
            startWorkflow(res, false);
        }

        private void startFailing(ServerRequest req, ServerResponse res) {
            startWorkflow(res, true);
        }

        private void startWorkflow(ServerResponse res, boolean simulateFailure) {
            LaunchWorkflowArguments arguments = LaunchWorkflowArguments.builder()
                    .workflowDefinitionId(WorkflowDefinitionId.builder()
                                                 .name("instance-create")
                                                 .majorVersion(3)
                                                 .minorVersion(1)
                                                 .build())
                    .workflowArguments(("{\"simulateFailure\":" + simulateFailure + "}")
                            .getBytes(StandardCharsets.UTF_8))
                    .build();
            WorkflowInstance instance = workflowClient.launchWorkflow(arguments);
            res.status(Status.CREATED_201).send(render(instance));
        }

        private void advanceAndFetch(ServerRequest req, ServerResponse res) {
            String workflowInstanceId = req.path().pathParameters().get("id");
            WorkflowInstance instance = workflowClient.getWorkflowInstance(workflowInstanceId);
            res.status(Status.OK_200).send(render(instance));
        }

        private static String render(WorkflowInstance instance) {
            return instance.getId() + "|" + instance.getStatus().name() + "|" + instance.getTag();
        }
    }

    /**
     * A test-only harness inspired by the WFaaS WfTestDriver pattern: each poll-like request
     * advances a stored workflow scenario one step further.
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

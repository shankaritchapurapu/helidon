/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.workflow.sample;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.reflect.TypeToken;
import com.oracle.bmc.model.BmcException;
import com.oracle.pic.workflow.client.v1.model.LaunchWorkflowArguments;
import com.oracle.pic.workflow.client.v1.model.WorkflowDefinition;
import com.oracle.pic.workflow.client.v1.model.WorkflowInstance;
import com.oracle.pic.workflow.client.v1.model.WorkflowStatus;
import com.oracle.pic.workflow.worker.JsonTransformer;
import com.oracle.pic.workflow.worker.Loggable;
import com.oracle.pic.workflow.worker.PagedResult;
import com.oracle.pic.workflow.worker.Step;
import com.oracle.pic.workflow.worker.WorkerFunctionInterceptor;
import com.oracle.pic.workflow.worker.WorkerManagerV3;
import com.oracle.pic.workflow.worker.WorkflowClient;
import com.oracle.pic.workflow.worker.WorkflowInfo;
import com.oracle.pic.workflow.worker.core.DefaultLogAndMetricsDecorator;
import com.oracle.pic.workflow.worker.core.Results;
import com.oracle.pic.workflow.worker.core.Results.StepResult;
import com.oracle.pic.workflow.worker.exceptions.SerializationException;
import com.oracle.pic.workflow.worker.util.WorkflowDefinitionBuilder;
import com.oracle.pic.workflow.worker.util.WorkflowDefinitionParams;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@WorkflowDefinitionParams(approximateMaxRuntimeSeconds = 1000,
        approximateStartTimeoutSeconds = 1000,
        firstStep = DemoWorkflow.Steps.GATHER_INGREDIENTS_AND_TOOLS,
        majorVersion = 1,
        minorVersion = 1,
        name = "PeanutButterJellyWorkflow")
public class DemoWorkflow {
    public static final long LONG_POLL_TIMEOUT_MILLIS = 50_000;
    private static final Logger LOGGER = Logger.getLogger(DemoWorkflow.class.getName());

    public static class Steps {
        public static final String GATHER_INGREDIENTS_AND_TOOLS = "GATHER_INGREDIENTS_AND_TOOLS";
        public static final String ASSEMBLE_SANDWICH = "ASSEMBLE_SANDWICH";
        public static final String CONSUME_SANDWICH = "CONSUME_SANDWICH";
    }

    public static final int NUMBER_OF_WORKFLOWS = 1;

    public static final CountDownLatch WORKFLOW_IN_PROGRESS_COUNTER = new CountDownLatch(NUMBER_OF_WORKFLOWS);

    @Step(assignmentTTLMillis = 100_000, leaseTTLMillis = 20_000, name = Steps.GATHER_INGREDIENTS_AND_TOOLS, numAssignmentsBeforeFailure = 5)
    public StepResult<DemoWorkflowState> gatherIngredientsAndTools(DemoWorkflowArguments workflowArgs,
                                                                   DemoWorkflowState workflowState,
                                                                   WorkflowInfo info) throws InterruptedException {
        if (workflowState == null) {
            workflowState = DemoWorkflowState.builder().build();
        }
        // Ingredients list is null
        // Hunger is null
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("############## Initial Args - " + workflowArgs);
            LOGGER.info("############## State step 1 - " + workflowState);
            LOGGER.info("############## Gathering ingredients and butter knife - " + workflowArgs.getWorkflowId());
        }
        List<String> itemList = new ArrayList<>();
        itemList.add("Bread");
        itemList.add("Peanut Butter");
        itemList.add("Jelly");
        itemList.add("Butter Knife");
        DemoWorkflowState updatedworkflowState =
                workflowState.updateState(HungerLevel.GETTING_HUNGRY, itemList);
        updatedworkflowState = updatedworkflowState.copyBuilder().retryCount(0).build();



        Thread.sleep(1000L);
        return Results.completeStep(Steps.ASSEMBLE_SANDWICH, updatedworkflowState);
    }

    @Step(assignmentTTLMillis = 200_000, leaseTTLMillis = 20_000, name = Steps.ASSEMBLE_SANDWICH,
            numAssignmentsBeforeFailure = 5)
    public StepResult<DemoWorkflowState> assembleSandwich(DemoWorkflowArguments workflowArgs,
                                                          DemoWorkflowState workflowState,
                                                          WorkflowInfo info) throws InterruptedException {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("############## Initial Args - " + workflowArgs);
            LOGGER.info("############## State step 2 - " + workflowState);
        }
        // Getting hungry
        // Have bread, peanut butter, jelly, butter knife

        // do logic with bread pb and jelly and knife
        List<String> itemList = workflowState.getItemsList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itemList.size(); i++) {
            sb.append(itemList.get(i));
            if (i != itemList.size() - 1) {
                sb.append(",");
            }
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("############## Sandwich Making Called for Workflow - " + workflowArgs.getWorkflowId());
            LOGGER.info("############## Making the sandwich with the following " + sb);
            LOGGER.info("############## Putting away all items");
        }
        DemoWorkflowState updatedworkflowState = workflowState.updateState(HungerLevel.STARVING, null);
        updatedworkflowState = updatedworkflowState.copyBuilder().retryCount(0).build();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("############## Assembled Called for Workflow - " + workflowArgs.getWorkflowId());
        }
        Thread.sleep(1000L);
        return Results.completeStep(Steps.CONSUME_SANDWICH, updatedworkflowState);
    }

    @Step(assignmentTTLMillis = 200_000, leaseTTLMillis = 20_000, name = Steps.CONSUME_SANDWICH, numAssignmentsBeforeFailure = 3)
    public StepResult<DemoWorkflowState> consumeSandwich(DemoWorkflowArguments workflowArgs,
                                                         DemoWorkflowState workflowState,
                                                         WorkflowInfo info) throws InterruptedException {
        final DemoWorkflowState finalWorkflowState = workflowState.updateState(HungerLevel.FULL, null);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("~~~~~~~~~~~~~~ Consume Sandwich Called for Workflow - " + workflowArgs.getWorkflowId());
        }
        // Starving
        // null
        Thread.sleep(2000L);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("############## Initial Args - " + workflowArgs);
            LOGGER.info("############## State step 3 - " +  workflowState);
        }
        if (workflowState.getRetryCount() == 2) {
            WORKFLOW_IN_PROGRESS_COUNTER.countDown();
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("############## finished step 3 - " + finalWorkflowState);
            }
            // populateObservabilityContext();
            return Results.terminateAsSuccess("Successfully consumed food. I am full now", finalWorkflowState);
        } else {
            if (workflowState.getRetryCount() == 0) {
                return Results.clientDelay(workflowState.copyBuilder().retryCount(1).build(), 2);
            } else {
                return Results.completeStep(Steps.CONSUME_SANDWICH, workflowState.copyBuilder().retryCount(2).build(), 2);
            }
        }
    }

    @Value
    @JsonDeserialize(builder = DemoWorkflowArguments.Builder.class)
    @Builder(builderClassName = "Builder")
    public static class DemoWorkflowArguments implements Loggable {

        @JsonProperty
        long size;
        @JsonProperty
        boolean encrypted;
        @JsonProperty
        int workflowId;

        @Override
        @JsonIgnore
        public Map<String, String> getDecorations() {
            HashMap<String, String> decorations = new HashMap<>();
            decorations.put(DefaultLogAndMetricsDecorator.CORRELATION_ID, "DemoWorkflowCorrelationId");
            return decorations;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
        }
    }

    enum HungerLevel {
        GETTING_HUNGRY,
        STARVING,
        FULL
    }

    @Value
    @JsonDeserialize(builder = DemoWorkflowState.Builder.class)
    @Builder(builderClassName = "Builder")
    public static class DemoWorkflowState {

        @JsonProperty
        HungerLevel hungerLevel;

        @JsonProperty
        List<String> itemsList;

        @JsonProperty
        int retryCount;

        private Builder copyBuilder() {
            return builder().itemsList(itemsList)
                            .hungerLevel(hungerLevel);
        }


        public DemoWorkflowState updateState(HungerLevel newHungerLevel, List<String> itemList) {
            return copyBuilder().itemsList(itemList)
                                .hungerLevel(newHungerLevel)
                                .build();
        }



        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
        }
    }

    /**
     * Workflow Definition
     *
     * @return
     */
    public static WorkflowDefinition createWorkflowDef() {
        return WorkflowDefinitionBuilder.buildFromAnnotations(DemoWorkflow.class);
    }

    public static void demoWorkflow(WorkflowClient workerClient, WorkflowClient pollerClient) throws SerializationException, InterruptedException, BmcException {
        final long startTimestamp = Instant.now().toEpochMilli();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Workflow client: " + workerClient);
            LOGGER.info("Creating workflow definition");
        }
        WorkflowDefinition definition = createWorkflowDef();
        workerClient.postWorkflowDefinition(definition);

        WorkerManagerV3<DemoWorkflowArguments, DemoWorkflowState> workerManager;

        String tag = "tag";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Creating WorkerManager with tag: \"" + tag + "\"");
            LOGGER.info("Poller client: " + pollerClient);
        }

        DemoWorkflow wfDefInstance = new DemoWorkflow();
        workerManager = WorkerManagerV3.<DemoWorkflowArguments, DemoWorkflowState>builder()
                .fromAnnotations(wfDefInstance, new TypeToken<DemoWorkflowArguments>() {
                }, new TypeToken<DemoWorkflowState>() {
                })
                .workerClient(workerClient)
                .pollerClient(pollerClient)
                .longPollTimeoutMillis(LONG_POLL_TIMEOUT_MILLIS)
                .registerInterceptorForEachStep(stepInterceptor)
                .numWorkerThreads(200)
                .workerTag(tag)
                .workerProject("workerProject")
                .workerFleet("workerFleet")
                .build();

        JsonTransformer<DemoWorkflowArguments> workflowArgsTransformer =
                new JsonTransformer<>(new TypeReference<DemoWorkflowArguments>() {
                });
        for (int i = 0; i < NUMBER_OF_WORKFLOWS; i++) {
            String surrogateKey = String.format("Workflow-%d-%d", startTimestamp, i);
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Launching Workflow with surrogate key " + surrogateKey);
            }
            workerClient.launchWorkflow(LaunchWorkflowArguments.builder()
                                                         .workflowArguments(workflowArgsTransformer.serialize(
                                                                 DemoWorkflowArguments.builder()
                                                                                              .encrypted(false)
                                                                                              .size(1000)
                                                                                              .workflowId(i)
                                                                                              .build()))
                                                         .surrogateKey(surrogateKey)
                                                         .workflowDefinitionId(definition.getId())
                                                         .tag(tag)
                                                         .build());
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Sleeping");
            }
            Thread.sleep(2);
        }

        Instant start = Instant.now();
        workerManager.start();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Launched All workflows");
        }

        PagedResult<WorkflowInstance> workflowInstances;
        List<WorkflowInstance> wfInstances;
        /**
        // 2 iterations:
        // 1. Wait until a workflow with non-terminal state exist
        // 2. Wait until all of non-terminal workflow states are not terminal anymore

        for (int x = 1; x <= 2; x++) {
            boolean loop = false;
            do {
                LOGGER.info("Launched All workflows");
                workflowInstances = workerClient.getWorkflowInstances(true, false, null);
                wfInstances = workflowInstances.getResult().stream().filter(
                        i -> (StringUtils.isBlank(tag) && i.getTag() == null) || i.getTag().equals(tag)
                ).toList();
                Thread.sleep(2000);
                LOGGER.info("Retrieving non-terminal workflowInstances with instance count: " + wfInstances.size() +
                                    " for iteration: " + x);
                loop = x == 1 == wfInstances.isEmpty();
            } while (loop);
        }
         **/
        do {
            LOGGER.info("About to retrieve workflow instances");
            workflowInstances = workerClient.getWorkflowInstances(false, true, null);
            wfInstances = workflowInstances.getResult().stream().filter(
                    i -> (StringUtils.isBlank(tag) && i.getTag() == null) || i.getTag().equals(tag)
            ).toList();
            Thread.sleep(2000);
            LOGGER.info("Retrieving workflowInstances with instance count: " + wfInstances.size());
        // } while (wfInstances.isEmpty()); // != NUMBER_OF_WORKFLOWS);
        } while (wfInstances.size() != NUMBER_OF_WORKFLOWS);

        workerManager.shutdown();
        LOGGER.info("workerManager.shutdown() with instance count: " + wfInstances.size());
        int completedInstances = 0;
        for (WorkflowInstance instance : wfInstances) {
            LOGGER.info("wfInstances id: " + instance.getId());
            if (instance.getStatus() != WorkflowStatus.Complete && instance.getStatus() != WorkflowStatus.WorkflowTimedOut) {
                LOGGER.info("Instance failed !!!");
                break;
            }

            completedInstances++;
        }

        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        LOGGER.info("Exiting. Instances completed successfully: "
                            + completedInstances + " of " + NUMBER_OF_WORKFLOWS + " in " + timeElapsed);
    }

    private static WorkerFunctionInterceptor<DemoWorkflowArguments, DemoWorkflowState> stepInterceptor = (delegate, workflowArguments, stateData, workflowInfo) -> {
        LOGGER.info("Interceptor called!!!!!!!!!!");
        return delegate.apply(workflowArguments, stateData, workflowInfo);
    };
}

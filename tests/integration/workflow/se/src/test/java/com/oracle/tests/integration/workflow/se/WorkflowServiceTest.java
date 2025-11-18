/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.workflow.se;

import java.time.Duration;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.Status;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Services;
import io.helidon.testing.junit5.Testing;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Testing.Test
class WorkflowServiceTest {
    private static final DockerImageName IMAGE =
            DockerImageName.parse("odo-docker-signed-local.artifactory.oci.oraclecorp.com/wfaas-in-memory:6000.1.1017");
    private static final int WORKFLOW_INMEMORY_PORT = 39000;
    private static Http1Client client;

    @BeforeAll
    static void setup() {
        Config config = Config.builder()
                .addSource(ConfigSources.create(
                        Map.of("oci.workflow.endpoint-details.server-endpoint", "http://localhost:" + getEndpointPort())))
                .metaConfig()
                .build();
        Services.set(Config.class, config);

        // invoke post construct on startup service, to make sure the server starts
        // if more than one service has a RunLevel, you can also lookup based on the run levels
        // no need for @AfterAll, as the registry will be shut down thanks to the Testing.Test annotation
        StartupService startupService = GlobalServiceRegistry.registry()
                .get(StartupService.class);

        // and create a client
        client = Http1Client.builder()
                .baseUri("http://localhost:" + startupService.serverPort())
                .readTimeout(Duration.ofMinutes(5))
                .build();
    }

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> CONTAINER = new GenericContainer<>(IMAGE)
            .withEnv("WORKFLOW_INMEMORY_PORT", String.valueOf(WORKFLOW_INMEMORY_PORT))   //"39000")
            .withExposedPorts(WORKFLOW_INMEMORY_PORT)
            .withStartupAttempts(3)
            .waitingFor(Wait.forLogMessage(".*org.eclipse.jetty.server.Server: Started Server.*\\n", 1)
                                .withStartupTimeout(Duration.ofMinutes(5)));

    static String getEndpointPort() {
        return CONTAINER.getMappedPort(WORKFLOW_INMEMORY_PORT).toString();
    }

    @Test
    void TestDemoWorkflow() {
        try (Http1ClientResponse response = client.get("/workflow").request()) {
            assertThat(response.status(), is(Status.OK_200));
        }
    }
}

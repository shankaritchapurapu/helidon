/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.workflow.microprofile;

import java.time.Duration;
import java.util.Map;

// import io.helidon.config.Config;
import io.helidon.config.mp.MpConfigSources;
import io.helidon.microprofile.testing.AddConfigSource;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@HelidonTest
class WorkflowResourceTest {
    private static final DockerImageName IMAGE =
            DockerImageName.parse("odo-docker-signed-local.artifactory.oci.oraclecorp.com/wfaas-in-memory:6000.1.1017");
    private static final int WORKFLOW_INMEMORY_PORT = 39000;
    @Inject
    private WebTarget webTarget;

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

    @AddConfigSource
    static ConfigSource config() {
        return MpConfigSources.create(Map.of(
                "oci.workflow.endpoint-details.server-endpoint", "http://localhost:" + getEndpointPort()));
    }

    @Test
    void TestDemoWorkflow() {
        Response response = webTarget.path("/workflow").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }
}

/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.workflow.se;

import java.util.function.Supplier;

import io.helidon.common.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

/**
 * This service starts the webserver based on the injected service.
 * <p>
 * This is a singleton (a service annotation), and it has a run level defined.
 * The existence of RunLevel annotation will make this service auto-started when using the generated main class.
 */
@Service.Singleton
@Service.RunLevel(Service.RunLevel.SERVER)
class StartupService {
    private final Supplier<WorkflowService> workflowService;
    private final Supplier<Config> config;

    private volatile WebServer server;

    /**
     * Constructor that will be injected with values from the service registry.
     *
     * @param config root configuration
     * @param greetService the {@link io.helidon.examples.quickstart.inject.GreetService}
     */
    @Service.Inject
    StartupService(Supplier<Config> config, Supplier<WorkflowService> workflowService) {
        this.config = config;
        this.workflowService = workflowService;
    }

    /**
     * Invoked after the Helidon service registry creates an instance of this singleton.
     */
    @Service.PostConstruct
    void init() {
        server = WebServer.builder()
                .config(config.get().get("server"))
                .routing(this::routing)
                .build()
                .start();
    }

    /**
     * Invoked during shutdown of the service registry. This is when the process ends for production runtime,
     * and when a test class is finished when used from JUnit.
     */
    @Service.PreDestroy
    void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    // a testing method to obtain the current server port
    int serverPort() {
        if (server == null) {
            throw new IllegalStateException("Server not started");
        }
        return server.port();
    }

    private void routing(HttpRouting.Builder routing) {
        routing.register("/workflow", workflowService.get());
    }
}

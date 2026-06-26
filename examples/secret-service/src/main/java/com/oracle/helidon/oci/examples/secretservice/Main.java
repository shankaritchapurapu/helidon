/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.secretservice;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;

/**
 * Main class responsible for starting the Secret Service example.
 */
@Service.GenerateBinding
public class Main {
    static {
        LogConfig.initClass();
    }

    private Main() {
    }

    /**
     * Application entry point.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        LogConfig.configureRuntime();

        ServiceRegistryManager.start(ApplicationBinding.create());

        WebServer webServer = Services.get(WebServer.class);
        System.out.println("Server started on: http://localhost:" + webServer.port() + "/secret");
    }
}

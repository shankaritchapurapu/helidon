/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;

/**
 * Main class responsible for starting the data-plane example.
 */
@Service.GenerateBinding
public class Main {
    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());

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
        if (webServer.isRunning()) {
            LOGGER.log(System.Logger.Level.INFO,
                       "Server started on: http://localhost:" + webServer.port() + "/data-plane");
        } else {
            LOGGER.log(System.Logger.Level.ERROR, "Server failed to start.");
        }
    }
}

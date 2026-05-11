/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.tagging;

import java.security.Security;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;

import com.oracle.jipher.provider.JipherJCE;

/**
 * Main class responsible for starting the tagging example.
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
        Security.addProvider(new JipherJCE());
        LogConfig.configureRuntime();

        ServiceRegistryManager.start(ApplicationBinding.create());

        WebServer webServer = Services.get(WebServer.class);
        System.out.println("Server started on: http://localhost:" + webServer.port() + "/tagging");
    }
}

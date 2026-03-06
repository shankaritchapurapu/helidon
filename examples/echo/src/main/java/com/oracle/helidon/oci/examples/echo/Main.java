/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.echo;

import java.security.Security;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;

import com.oracle.jipher.provider.JipherJCE;

/**
 * Main class responsible for starting the service registry.
 * <p>
 * This class is annotated with {@link io.helidon.service.registry.Service.GenerateBinding}, which (when used in combination
 * with Helidon Maven Plugin) generates a binding class that can be used to bootstrap Helidon without usage of reflection
 * and classpath lookup during discovery of services.
 */
// annotation is required to generate application binding
@Service.GenerateBinding
public class Main {
    static {
        // used when building with GraalVM native image to configure logging during build
        LogConfig.initClass();
    }

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        // add Jipher as a provider
        Security.addProvider(new JipherJCE());

        // used to configure logging
        LogConfig.configureRuntime();

        ServiceRegistryManager.start(ApplicationBinding.create());

        WebServer webServer = Services.get(WebServer.class);
        System.out.println("Server started on: http://localhost:" + webServer.port() + "/echo");
    }
}

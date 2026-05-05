/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.examples.metrics;

import java.util.List;

import io.helidon.common.LazyValue;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.metrics.api.Metrics;
import io.helidon.metrics.api.Timer;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

@RestServer.Endpoint
@Http.Path("/hello")
@Service.Singleton
class HelloService {

    private final LazyValue<Timer> personalizedGreetingTimer = LazyValue.create(() -> Metrics.globalRegistry()
            .timer("personalized-greeting", List.of())
            .orElse(null));

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    @Metrics.Timed(value = "personalized-greeting", absoluteName = true)
    @Http.Path("/{name}")
    String personalizedGreeting(@Http.PathParam("name") String name) {

        return "Hello, " + name + "! (count: " + (
                personalizedGreetingTimer.get() == null
                        ? "?"
                        : personalizedGreetingTimer.get().count() + ")");
    }

    @Http.GET
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    @Metrics.Timed(value = "greeting", absoluteName = true)
    String greeting() {
        return "Hello, World!";
    }

}

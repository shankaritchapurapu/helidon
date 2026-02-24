/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.examples.limits;

import io.helidon.service.registry.Services;

/**
 * Entry point for the Helidon OCI Limits example.
 * <p>
 * This class is intentionally small: it boots the Helidon {@code ServiceRegistry}, looks up the
 * {@link LimitsExample} service, and delegates to {@link LimitsExample#run()}.
 * <p>
 * The example relies on Helidon dependency injection to provide all required OCI components
 * (for example, OCI configuration and the OCI SDK limits client). See {@link LimitsExample} for
 * the actual API call.
 */
public class Main {

    private Main() {
    }

    /**
     * Starts the example.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        // Obtain the example "runner" from Helidon's service registry.
        LimitsExample limitsExample = Services.get(LimitsExample.class);

        // Execute the sample logic (prints configuration info and performs a limits API call).
        limitsExample.run();
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import io.helidon.builder.api.Prototype;

import com.oracle.pic.kiev.Transaction;

/**
 * A bounded control plane metering event.
 */
@Prototype.Blueprint
interface MeteringEventBlueprint {
    /**
     * Meter name.
     *
     * @return meter name
     */
    String meterName();

    /**
     * Compartment OCID.
     *
     * @return compartment OCID
     */
    String compartmentId();

    /**
     * Metered resource ID.
     *
     * @return resource ID
     */
    String resourceId();

    /**
     * Event start time.
     *
     * @return event start time
     */
    Instant from();

    /**
     * Event end time.
     *
     * @return event end time
     */
    Instant to();

    /**
     * Metered amount.
     *
     * @return metered amount
     */
    double amount();

    /**
     * Event tags.
     *
     * @return tags
     */
    Map<String, String> tags();

    /**
     * Kiev transaction for agent-backed recording.
     *
     * @return Kiev transaction
     */
    Optional<Transaction> transaction();
}

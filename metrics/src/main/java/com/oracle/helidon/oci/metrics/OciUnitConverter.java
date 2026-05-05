/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Unit normalization helpers for emitted OCI metric values.
 */
final class OciUnitConverter {

    private OciUnitConverter() {
    }

    static double normalizeDuration(long duration, TimeUnit sourceUnit, TimeUnit targetUnit) {
        return (double) sourceUnit.toNanos(duration) / targetUnit.toNanos(1);
    }

    static double normalizeAmount(Optional<String> baseUnit, double amount) {
        return baseUnit.map(unit -> normalizeStorageAmount(unit, amount)).orElse(amount);
    }

    private static double normalizeStorageAmount(String baseUnit, double amount) {
        return switch (baseUnit.toLowerCase(Locale.ROOT)) {
            case "b", "byte", "bytes" -> amount;
            case "kb", "kilobyte", "kilobytes" -> amount * 1024D;
            case "mb", "megabyte", "megabytes" -> amount * 1024D * 1024D;
            case "gb", "gigabyte", "gigabytes" -> amount * 1024D * 1024D * 1024D;
            case "tb", "terabyte", "terabytes" -> amount * 1024D * 1024D * 1024D * 1024D;
            default -> amount;
        };
    }
}

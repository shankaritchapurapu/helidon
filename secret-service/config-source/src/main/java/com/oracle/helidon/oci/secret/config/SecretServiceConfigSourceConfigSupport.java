/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.time.Duration;

import io.helidon.builder.api.Prototype;

final class SecretServiceConfigSourceConfigSupport
        implements Prototype.BuilderDecorator<SecretServiceConfigSourceConfig.BuilderBase<?, ?>> {
    SecretServiceConfigSourceConfigSupport() {
    }

    @Override
    public void decorate(SecretServiceConfigSourceConfig.BuilderBase<?, ?> builder) {
        String prefix = builder.prefix().trim();
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        builder.prefix(prefix);

        if (builder.cacheTtl().isNegative()) {
            throw new IllegalArgumentException("cacheTtl must not be negative");
        }
        builder.pollInterval().ifPresent(pollInterval -> {
            if (pollInterval.isNegative() || pollInterval.isZero()) {
                throw new IllegalArgumentException("pollInterval must be positive");
            }
        });
    }

    @Prototype.PrototypeMethod
    static Duration effectivePollInterval(SecretServiceConfigSourceConfig prototype) {
        return prototype.pollInterval()
                .orElse(SecretServiceConfigSource.DEFAULT_POLL_INTERVAL);
    }
}

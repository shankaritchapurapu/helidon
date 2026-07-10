/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

import io.helidon.metrics.api.Meter;

/**
 * Common neutral meter behavior shared by all OCI-backed meters.
 */
abstract class AbstractOciMeter implements Meter {

    private final OciMeterRegistry registry;
    private final Meter delegate;
    private final boolean enabled;
    private final boolean accumulationEligible;

    AbstractOciMeter(OciMeterRegistry registry,
                     Meter delegate,
                     boolean enabled,
                     boolean accumulationEligible) {
        this.registry = registry;
        this.delegate = delegate;
        this.enabled = enabled;
        this.accumulationEligible = accumulationEligible;
    }

    @Override
    public Meter.Id id() {
        return delegate.id();
    }

    @Override
    public Optional<String> baseUnit() {
        return delegate.baseUnit();
    }

    @Override
    public Optional<String> description() {
        return delegate.description();
    }

    @Override
    public Meter.Type type() {
        return delegate.type();
    }

    @Override
    public Optional<String> scope() {
        return delegate.scope();
    }

    boolean enabled() {
        return enabled;
    }

    boolean accumulationEnabled() {
        return accumulationEligible && registry.publisher().acceptingUpdates();
    }

    OciMeterRegistry registry() {
        return registry;
    }

    Meter delegate() {
        return delegate;
    }

    @Override
    public <R> R unwrap(Class<? extends R> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (type.isInstance(delegate)) {
            return type.cast(delegate);
        }
        return delegate.unwrap(type);
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import io.helidon.metrics.api.Clock;

/**
 * One-minute EWMA rate matching Dropwizard's timer rate calculation.
 */
final class OciOneMinuteRate {

    private static final long TICK_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final double ALPHA = 1 - Math.exp(-5.0 / 60.0);

    private final Clock clock;
    private final AtomicLong lastTick;
    private final LongAdder uncounted = new LongAdder();
    private final Object tickLock = new Object();
    private volatile boolean initialized;
    private volatile double rate;

    OciOneMinuteRate(Clock clock) {
        this.clock = clock;
        this.lastTick = new AtomicLong(clock.monotonicTime());
    }

    void mark() {
        tickIfNecessary();
        uncounted.increment();
    }

    double rate() {
        tickIfNecessary();
        return rate * TimeUnit.SECONDS.toNanos(1);
    }

    private void tickIfNecessary() {
        long oldTick = lastTick.get();
        long newTick = clock.monotonicTime();
        long age = newTick - oldTick;
        if (age <= TICK_INTERVAL_NANOS) {
            return;
        }

        synchronized (tickLock) {
            oldTick = lastTick.get();
            newTick = clock.monotonicTime();
            age = newTick - oldTick;
            if (age <= TICK_INTERVAL_NANOS) {
                return;
            }
            long newIntervalStartTick = newTick - age % TICK_INTERVAL_NANOS;
            lastTick.set(newIntervalStartTick);
            long requiredTicks = age / TICK_INTERVAL_NANOS;
            for (long i = 0; i < requiredTicks; i++) {
                tick();
            }
        }
    }

    private void tick() {
        long count = uncounted.sumThenReset();
        double instantRate = count / (double) TICK_INTERVAL_NANOS;
        if (initialized) {
            double oldRate = rate;
            rate = oldRate + ALPHA * (instantRate - oldRate);
        } else {
            rate = instantRate;
            initialized = true;
        }
    }
}

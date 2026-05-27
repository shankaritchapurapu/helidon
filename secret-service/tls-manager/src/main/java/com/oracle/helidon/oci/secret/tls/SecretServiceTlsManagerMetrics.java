/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.api.Timer;
import io.helidon.service.registry.Services;

import static io.helidon.metrics.api.Meter.Scope.VENDOR;

final class SecretServiceTlsManagerMetrics {
    static final String RELOADS = "helidon.oci.secret_service.tls.reloads";
    static final String RELOAD_DURATION = "helidon.oci.secret_service.tls.reload.duration";
    static final String CONSECUTIVE_RELOAD_FAILURES = "helidon.oci.secret_service.tls.reload.consecutive_failures";

    static final String RESULT_SUCCESS = "success";
    static final String RESULT_FAILURE = "failure";
    static final String RESULT_SKIPPED = "skipped";

    private static final String TAG_MANAGER = "manager";
    private static final String TAG_RESULT = "result";
    private static final System.Logger LOGGER = System.getLogger(SecretServiceTlsManagerMetrics.class.getName());
    private static final Timer.Sample NOOP_SAMPLE = ignored -> 0L;
    private static final ConcurrentMap<String, AtomicInteger> CONSECUTIVE_FAILURES = new ConcurrentHashMap<>();

    private final boolean enabled;
    private final MetricsFactory metricsFactory;
    private final MeterRegistry registry;
    private final AtomicInteger consecutiveFailures;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter skippedCounter;
    private final Timer reloadDuration;

    private SecretServiceTlsManagerMetrics(String managerName) {
        this.metricsFactory = Services.get(MetricsFactory.class);
        this.registry = metricsFactory.globalRegistry();
        Tag managerTag = metricsFactory.tagCreate(TAG_MANAGER, managerName);

        this.consecutiveFailures = CONSECUTIVE_FAILURES.computeIfAbsent(managerName, ignored -> new AtomicInteger());
        this.successCounter = counter(managerTag, RESULT_SUCCESS);
        this.failureCounter = counter(managerTag, RESULT_FAILURE);
        this.skippedCounter = counter(managerTag, RESULT_SKIPPED);
        this.reloadDuration = timer(managerTag);
        this.enabled = true;

        registry.getOrCreate(metricsFactory.gaugeBuilder(CONSECUTIVE_RELOAD_FAILURES, consecutiveFailures::get)
                                     .scope(VENDOR)
                                     .tags(List.of(managerTag)));
    }

    private SecretServiceTlsManagerMetrics() {
        this.enabled = false;
        this.metricsFactory = null;
        this.registry = null;
        this.consecutiveFailures = null;
        this.successCounter = null;
        this.failureCounter = null;
        this.skippedCounter = null;
        this.reloadDuration = null;
    }

    static SecretServiceTlsManagerMetrics create(String managerName) {
        try {
            return new SecretServiceTlsManagerMetrics(managerName);
        } catch (RuntimeException e) {
            if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                LOGGER.log(System.Logger.Level.DEBUG, "Secret Service TLS reload metrics disabled", e);
            }
            return new SecretServiceTlsManagerMetrics();
        }
    }

    Timer.Sample reloadStarted() {
        if (!enabled) {
            return NOOP_SAMPLE;
        }
        return metricsFactory.timerStart(registry);
    }

    void reloadSucceeded(Timer.Sample sample) {
        if (!enabled) {
            return;
        }
        consecutiveFailures.set(0);
        successCounter.increment();
        sample.stop(reloadDuration);
    }

    void reloadSkipped(Timer.Sample sample) {
        if (!enabled) {
            return;
        }
        consecutiveFailures.set(0);
        skippedCounter.increment();
        sample.stop(reloadDuration);
    }

    void reloadFailed(Timer.Sample sample) {
        if (!enabled) {
            return;
        }
        consecutiveFailures.incrementAndGet();
        failureCounter.increment();
        sample.stop(reloadDuration);
    }

    private Counter counter(Tag managerTag, String result) {
        List<Tag> tags = List.of(managerTag, metricsFactory.tagCreate(TAG_RESULT, result));
        return registry.getOrCreate(metricsFactory.counterBuilder(RELOADS)
                                            .scope(VENDOR)
                                            .tags(tags));
    }

    private Timer timer(Tag managerTag) {
        return registry.getOrCreate(metricsFactory.timerBuilder(RELOAD_DURATION)
                                            .scope(VENDOR)
                                            .tags(List.of(managerTag)));
    }
}

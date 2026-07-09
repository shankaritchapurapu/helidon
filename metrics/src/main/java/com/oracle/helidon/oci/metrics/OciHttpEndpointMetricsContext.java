/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Objects;

/**
 * Request-scoped metadata used by automatic HTTP metrics.
 * <p>
 * This type is public so generated endpoint interceptors in application packages can create and register the context.
 */
public final class OciHttpEndpointMetricsContext {
    /**
     * Request context key for suppressing status-code/status-family automatic HTTP metrics.
     * <p>
     * Register {@code true} under this key in the Helidon request context to suppress
     * {@code ResponseOut.StatusCode.*.Count}, {@code ResponseOut.StatusFamily.*.Count}, and status-family
     * {@code Request.Client.*.*XX.Count} metrics for that request.
     */
    public static final String SKIP_RESPONSE_STATUS_METRICS = "SkipResponseStatusMetrics";

    private final String primaryScope;
    private final List<String> secondaryScopes;
    private final String fullyQualifiedResourceClassName;
    private long resourceStartNanos;
    private long resourceEndNanos;
    private boolean failed;

    /**
     * Creates endpoint metrics context.
     *
     * @param primaryScope primary metric scope
     * @param secondaryScopes secondary metric scopes
     * @param fullyQualifiedResourceClassName fully qualified resource class name
     */
    public OciHttpEndpointMetricsContext(String primaryScope,
                                         List<String> secondaryScopes,
                                         String fullyQualifiedResourceClassName) {
        this.primaryScope = Objects.requireNonNull(primaryScope, "primaryScope");
        this.secondaryScopes = List.copyOf(secondaryScopes);
        this.fullyQualifiedResourceClassName = Objects.requireNonNull(fullyQualifiedResourceClassName,
                                                                      "fullyQualifiedResourceClassName");
    }

    /**
     * Primary metric scope.
     *
     * @return primary scope
     */
    public String primaryScope() {
        return primaryScope;
    }

    /**
     * Secondary metric scopes.
     *
     * @return secondary scopes
     */
    public List<String> secondaryScopes() {
        return secondaryScopes;
    }

    /**
     * Fully qualified resource class name.
     *
     * @return fully qualified resource class name
     */
    public String fullyQualifiedResourceClassName() {
        return fullyQualifiedResourceClassName;
    }

    /**
     * Marks resource method execution start.
     */
    public void markResourceStart() {
        resourceStartNanos = System.nanoTime();
    }

    /**
     * Marks resource method execution end.
     *
     * @param failed whether the endpoint failed by throwing
     */
    public void markResourceEnd(boolean failed) {
        resourceEndNanos = System.nanoTime();
        this.failed = failed;
    }

    /**
     * Whether resource timing is available.
     *
     * @return {@code true} if resource start and end were captured
     */
    public boolean hasResourceTiming() {
        return resourceStartNanos > 0 && resourceEndNanos >= resourceStartNanos;
    }

    /**
     * Resource method elapsed nanoseconds.
     *
     * @return elapsed nanoseconds
     * @throws IllegalStateException if resource timing is not available
     */
    public long resourceElapsedNanos() {
        if (!hasResourceTiming()) {
            throw new IllegalStateException("Resource timing is not available");
        }
        return resourceEndNanos - resourceStartNanos;
    }

    /**
     * Whether the endpoint method threw.
     *
     * @return {@code true} if the endpoint threw
     */
    public boolean failed() {
        return failed;
    }
}

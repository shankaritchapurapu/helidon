/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the primary automatic HTTP metrics scope for REST endpoint classes.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
public @interface MetricPrefix {
    /**
     * Custom automatic HTTP metrics scope.
     *
     * @return metric prefix
     */
    String value();

    /**
     * Whether the Java method name should be appended to {@link #value()}.
     *
     * @return {@code true} to append the method name
     */
    boolean appendMethodName() default false;
}

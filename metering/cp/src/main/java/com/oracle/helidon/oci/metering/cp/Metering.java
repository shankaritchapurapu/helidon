/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.service.registry.Interception;

/**
 * Entry point for control plane metering annotations.
 */
public final class Metering {
    private Metering() {
    }

    /**
     * Records one metering point when the annotated method is invoked.
     */
    @Interception.Intercepted
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    public @interface Point {
        /**
         * Meter name.
         *
         * @return meter name
         */
        String value();

        /**
         * Static compartment ID.
         *
         * @return compartment ID, or empty to use configuration or parameters
         */
        String compartmentId() default "";

        /**
         * Static resource ID.
         *
         * @return resource ID, or empty to use configuration or parameters
         */
        String resourceId() default "";

        /**
         * Recorded value.
         *
         * @return value
         */
        float amount() default 1.0F;

        /**
         * Static tags for this measurement.
         *
         * @return tags
         */
        Tag[] tags() default {};
    }

    /**
     * Records elapsed time for the annotated method invocation.
     */
    @Interception.Intercepted
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    public @interface Timed {
        /**
         * Meter name.
         *
         * @return meter name
         */
        String value();

        /**
         * Static compartment ID.
         *
         * @return compartment ID, or empty to use configuration or parameters
         */
        String compartmentId() default "";

        /**
         * Static resource ID.
         *
         * @return resource ID, or empty to use configuration or parameters
         */
        String resourceId() default "";

        /**
         * Static tags for this measurement.
         *
         * @return tags
         */
        Tag[] tags() default {};
    }

    /**
     * Marks the beginning of an explicitly bounded metered region.
     */
    @Interception.Intercepted
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    public @interface Start {
        /**
         * Meter name.
         *
         * @return meter name
         */
        String value();

        /**
         * Static compartment ID.
         *
         * @return compartment ID, or empty to use configuration or parameters
         */
        String compartmentId() default "";

        /**
         * Static resource ID.
         *
         * @return resource ID, or empty to use configuration or parameters
         */
        String resourceId() default "";

        /**
         * Static tags for this measurement.
         *
         * @return tags
         */
        Tag[] tags() default {};
    }

    /**
     * Marks the end of an explicitly bounded metered region.
     */
    @Interception.Intercepted
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    public @interface End {
        /**
         * Optional meter name override. If empty, the name from the matching {@link Start} region is used.
         *
         * @return meter name
         */
        String value() default "";

        /**
         * Static tags for this measurement.
         *
         * @return tags
         */
        Tag[] tags() default {};
    }

    /**
     * Static metering tag.
     */
    @Repeatable(Tags.class)
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Tag {
        /**
         * Tag key.
         *
         * @return key
         */
        String key();

        /**
         * Tag value.
         *
         * @return value
         */
        String value();
    }

    /**
     * Container for repeatable {@link Tag} annotations.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Tags {
        /**
         * Tags.
         *
         * @return tags
         */
        Tag[] value();
    }

    /**
     * Identifies a method parameter containing the metering compartment ID.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    public @interface CompartmentId {
    }

    /**
     * Identifies a method parameter containing the metered resource ID.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    public @interface ResourceId {
    }

    /**
     * Identifies a method parameter containing the measurement value.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    public @interface Amount {
    }

    /**
     * Identifies a method parameter to include as a dynamic tag.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    public @interface TagValue {
        /**
         * Tag key.
         *
         * @return key
         */
        String value();
    }
}

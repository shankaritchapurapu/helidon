/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.Map;

import io.helidon.json.JsonObject;

/**
 * Helpers for metering tags.
 */
public final class Tags {
    private Tags() {
    }

    /**
     * Combines static tag values with tag values resolved from intercepted method arguments.
     *
     * @param staticTags static tags
     * @param tagParameterIndexes map of tag names to argument indexes
     * @param args intercepted method arguments
     * @return effective tags
     */
    public static Map<String, String> from(Map<String, String> staticTags,
                                           Map<String, Integer> tagParameterIndexes,
                                           Object[] args) {
        if (tagParameterIndexes.isEmpty()) {
            return staticTags;
        }
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>(staticTags);
        tagParameterIndexes.forEach((key, index) -> {
            Object value = args[index];
            if (value != null) {
                result.put(key, value.toString());
            }
        });
        return Map.copyOf(result);
    }

    /**
     * Merges two tag maps with values from the second map winning.
     *
     * @param first first tags
     * @param second second tags
     * @return merged tags
     */
    public static Map<String, String> merge(Map<String, String> first, Map<String, String> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>(first);
        result.putAll(second);
        return Map.copyOf(result);
    }

    /**
     * Converts tags to the JSON text expected by the native metering libraries.
     *
     * @param tags tags
     * @return JSON text, or {@code null} if tags are empty
     */
    public static String toJson(Map<String, String> tags) {
        if (tags.isEmpty()) {
            return null;
        }

        JsonObject.Builder builder = JsonObject.builder();
        tags.forEach(builder::set);
        return builder.build().toString();
    }
}

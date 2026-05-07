/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.Map;

final class Tags {
    private Tags() {
    }

    static Map<String, String> from(Map<String, String> staticTags,
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

    static Map<String, String> merge(Map<String, String> first, Map<String, String> second) {
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

    static String toJson(Map<String, String> tags) {
        if (tags.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append('"');
        }
        return builder.append('}').toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

record ParsedKey(String configKey, String path) {
    static ParsedKey parse(String prefix, String configKey) {
        if (!configKey.startsWith(prefix)) {
            return null;
        }
        if (configKey.length() == prefix.length()) {
            return null;
        }

        char separator = configKey.charAt(prefix.length());
        if (separator != '/') {
            return null;
        }

        String path = configKey.substring(prefix.length());
        if (path.isBlank() || path.length() == 1) {
            return null;
        }

        return new ParsedKey(configKey, path);
    }
}

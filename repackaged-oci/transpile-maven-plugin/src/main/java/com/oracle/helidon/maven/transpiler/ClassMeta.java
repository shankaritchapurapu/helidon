/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.maven.transpiler;

record ClassMeta(String simpleName, String pkg, String fqdn) {
    static ClassMeta parse(String fqdn) {
        int lastDot = fqdn.lastIndexOf(".");
        String pkg = fqdn.substring(0, lastDot);
        String simpleName = fqdn.substring(lastDot + 1);
        return new ClassMeta(simpleName, pkg, fqdn);
    }
}

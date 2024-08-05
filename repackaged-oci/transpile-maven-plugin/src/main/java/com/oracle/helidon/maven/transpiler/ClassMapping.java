/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.maven.transpiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ClassMapping(ClassMeta from, ClassMeta to) {
    private static final Pattern PATTERN = Pattern.compile("([^:]*):([^:]*)");

    static ClassMapping parse(String mapping) {
        Matcher m = PATTERN.matcher(mapping);
        if (!m.find()) {
            throw new RuntimeException("Bad class mapping! " + mapping);
        }
        var fromFqdn = m.group(1);
        var toFqdn = m.group(2);
        return new ClassMapping(ClassMeta.parse(fromFqdn), ClassMeta.parse(toFqdn));
    }
}

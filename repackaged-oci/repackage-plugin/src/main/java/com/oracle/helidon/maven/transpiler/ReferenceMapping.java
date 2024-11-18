/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.maven.transpiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.bytecode.Descriptor;

/**
 * For changing class references in repackaged classes.
 */
public class ReferenceMapping {
    private String name;
    private String pkg;
    private String targetPkg;

    static Map<String, String> toMap(List<ReferenceMapping> references) {
        return references.stream()
                .reduce(new HashMap<>(), (m, e) -> {
                    m.put(e.getSrcFqdn(), e.getTargetFqdn());
                    m.put(Descriptor.toJvmName(e.getSrcFqdn()), Descriptor.toJvmName(e.getTargetFqdn()));
                    return m;
                }, (m1, m2) -> m1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getSrcFqdn() {
        return getPkg() + "." + getName();
    }

    public String getTargetFqdn() {
        return getTargetPkg() + "." + getName();
    }

    /**
     * Target package name.
     * @return target package name
     */
    public String getTargetPkg() {
        if (targetPkg == null) {
            return getPkg();
        }
        return targetPkg;
    }

    public void setTargetPkg(String targetPkg) {
        this.targetPkg = targetPkg;
    }
}



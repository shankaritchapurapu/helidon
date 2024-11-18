/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.maven.transpiler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Class mapping, for moving the class in to another package and/or changing its contents.
 */
public class ClassMapping {
    private String name;
    private String pkg;
    private String targetPkg;
    private String newParentClassName;
    private List<ReferenceMapping> referencesMapping = List.of();
    private List<Change> changes = List.of();

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

    public String getSrcFqdn() {
        return getPkg() + "." + getName();
    }

    public String getTargetFqdn() {
        return getTargetPkg() + "." + getName();
    }

    public List<Change> getModifierChanges() {
        return changes;
    }

    public Optional<String> getParentClassName() {
        return Optional.ofNullable(newParentClassName);
    }

    public void setParentClassName(String parentClassName) {
        this.newParentClassName = parentClassName;
    }

    public List<ReferenceMapping> getReferenceMapping() {
        return referencesMapping;
    }

    public Map<String, String> getRefMap() {
        return ReferenceMapping.toMap(getReferenceMapping());
    }
}

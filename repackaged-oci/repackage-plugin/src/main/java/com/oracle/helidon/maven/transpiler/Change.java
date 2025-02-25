/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.maven.transpiler;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class change executed during repackaging.
 */
public class Change {
    private String name;
    private String newName;
    private String newReturnType;
    private String newBody;
    private List<String> params = List.of();
    private List<String> newParams = List.of();
    private List<Substitution> substitutions = List.of();
    private String modifier;
    private boolean override;
    private boolean remove;
    private String type;

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public List<String> getNewParams() {
        return newParams;
    }

    public void setNewParams(List<String> newParams) {
        this.newParams = newParams;
    }

    public Optional<String> getNewBody() {
        return Optional.ofNullable(newBody);
    }

    public void setNewBody(String newBody) {
        this.newBody = newBody;
    }

    public Optional<String> getNewName() {
        return Optional.ofNullable(newName);
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public Map<String, Substitution> getSubstitutions() {
        return substitutions.stream().collect(Collectors.toMap(Substitution::getMethodCall, Function.identity()));
    }

    public void setSubstitutions(List<Substitution> substitutions) {
        this.substitutions = substitutions;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public Optional<String> getNewReturnType() {
        return Optional.ofNullable(newReturnType);
    }

    public void setNewReturnType(String newReturnType) {
        this.newReturnType = newReturnType;
    }

    @Override
    public String toString() {
        return "Change{"
                + "name='" + name + '\''
                + ", newName='" + newName + '\''
                + ", newReturnType='" + newReturnType + '\''
                + ", newBody='" + newBody + '\''
                + ", newParams=" + newParams
                + ", modifier='" + modifier + '\''
                + ", override=" + override
                + '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    boolean isModifierChange() {
        return modifier != null;
    }

    int computeFlags(int originalFlags) {
        if (override) {
            return getFlags();
        } else {
            return originalFlags | getFlags();
        }
    }

    private int getFlags() {
        switch (Optional.ofNullable(modifier).orElse("").toLowerCase(Locale.ROOT)) {
        case "public":
            return Modifier.PUBLIC;
        case "protected":
            return Modifier.PROTECTED;
        case "private":
            return Modifier.PRIVATE;
        case "volatile":
            return Modifier.VOLATILE;
        case "synchronized":
            return Modifier.SYNCHRONIZED;
        case "final":
            return Modifier.FINAL;
        default:
            try {
                return Integer.parseInt(modifier);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Unknown modifier: " + modifier);
            }
        }
    }
}

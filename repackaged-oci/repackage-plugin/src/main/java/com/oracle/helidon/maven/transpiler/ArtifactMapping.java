/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.maven.transpiler;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Artifacts for repackaging.
 */
public class ArtifactMapping {
    private String groupId;
    private String artifactId;
    private ArrayList<String> exclusions;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public Optional<String> getGroupId() {
        return Optional.ofNullable(groupId);
    }

    public Optional<String> getArtifactId() {
        return Optional.ofNullable(artifactId);
    }

    public void setExclude(ArrayList<String> exclude) {
        this.exclusions = exclude;
    }

    public ArrayList<String> getExclude() {
        return Optional.ofNullable(exclusions).orElseGet(ArrayList::new);
    }
}

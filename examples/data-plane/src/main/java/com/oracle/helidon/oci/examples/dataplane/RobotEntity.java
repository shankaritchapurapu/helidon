/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import com.oracle.pic.kiev.mapping.annotations.Column;
import com.oracle.pic.kiev.mapping.annotations.HashKey;
import com.oracle.pic.kiev.mapping.annotations.KievEntity;

import static com.oracle.pic.kiev.mapping.annotations.ColumnType.STRING;

/**
 * Kiev-mapped robot record stored by the example service.
 */
@KievEntity
class RobotEntity {
    @HashKey
    @Column(type = STRING, name = "id", length = 128)
    public String id;

    @Column(type = STRING, name = "compartment_id", length = 256)
    public String compartmentId;

    @Column(type = STRING, name = "display_name", length = 512)
    public String displayName;

    @Column(type = STRING, name = "lifecycle_state", length = 32)
    public String lifecycleState;

    RobotEntity() {
    }

    RobotEntity(String id, String compartmentId, String displayName, String lifecycleState) {
        this.id = id;
        this.compartmentId = compartmentId;
        this.displayName = displayName;
        this.lifecycleState = lifecycleState;
    }
}

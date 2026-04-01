/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import com.oracle.pic.kiev.mapping.annotations.Column;
import com.oracle.pic.kiev.mapping.annotations.HashKey;
import com.oracle.pic.kiev.mapping.annotations.KievEntity;

import static com.oracle.pic.kiev.mapping.annotations.ColumnType.STRING;

/**
 * Simple Kiev-mapped entity used by the store example.
 */
@KievEntity
class StoreItem {
    @HashKey
    @Column(type = STRING, name = "id", length = 128)
    public String id;

    @Column(type = STRING, name = "value", length = 2048)
    public String value;

    StoreItem() {
    }

    StoreItem(String id, String value) {
        this.id = id;
        this.value = value;
    }
}

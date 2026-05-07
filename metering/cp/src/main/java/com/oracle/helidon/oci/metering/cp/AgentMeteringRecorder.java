/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.bling.emit.store.MeteringLogStore;
import com.oracle.pic.kiev.Transaction;

/**
 * Agent control plane recorder backed by metering-agent Kiev metering log stores.
 */
class AgentMeteringRecorder implements MeteringRecorder {
    private final String name;
    private final MeteringLogStoreLookup logStores;
    private final Object delegate;

    AgentMeteringRecorder(String name, MeteringLogStores logStores) {
        this(name, logStores::getByMeterName, logStores);
    }

    AgentMeteringRecorder(String name, MeteringLogStoreLookup logStores) {
        this(name, logStores, logStores);
    }

    private AgentMeteringRecorder(String name, MeteringLogStoreLookup logStores, Object delegate) {
        this.name = name;
        this.logStores = logStores;
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return "agent";
    }

    @Override
    public void record(MeteringEvent event) throws Exception {
        Transaction transaction = event.transaction()
                .orElseThrow(() -> new IllegalStateException("Agent metering recorder requires a Kiev transaction"));
        MeteringLogStore logStore = logStores.getByMeterName(event.meterName());
        if (logStore == null) {
            throw new IllegalStateException("No metering log store configured for meter " + event.meterName());
        }
        logStore.addMeter(transaction,
                          event.resourceId(),
                          event.compartmentId(),
                          event.from(),
                          event.to(),
                          event.amount(),
                          Tags.toJson(event.tags()));
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(delegate)) {
            return type.cast(delegate);
        }
        return MeteringRecorder.super.unwrap(type);
    }

    @FunctionalInterface
    interface MeteringLogStoreLookup {
        MeteringLogStore getByMeterName(String meterName);
    }
}

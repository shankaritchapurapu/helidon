/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.List;
import java.util.UUID;

import com.oracle.pic.bling.clients.ingest.model.Meter;
import com.oracle.pic.bling.clients.ingest.model.Meters;
import com.oracle.pic.bling.emit.client.MeteringClient;

/**
 * Direct control plane recorder backed by the metering-agent {@link MeteringClient}.
 */
class DirectMeteringRecorder implements MeteringRecorder {
    private final String name;
    private final MeteringClient client;

    DirectMeteringRecorder(String name, MeteringClient client) {
        this.name = name;
        this.client = client;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return "direct";
    }

    @Override
    public void record(MeteringEvent event) {
        long fromMillis = event.from().toEpochMilli();
        long toMillis = event.to().toEpochMilli();
        Meter meter = Meter.builder()
                .mtr(event.meterName())
                .compartmentId(event.compartmentId())
                .resourceId(event.resourceId())
                .from(fromMillis)
                .to(toMillis)
                .value((float) event.amount())
                .tags(Tags.toJson(event.tags()))
                .build();
        Meters meters = Meters.builder()
                .compartmentId(event.compartmentId())
                .reportId(UUID.randomUUID().toString())
                .timestamp(toMillis)
                .from(fromMillis)
                .to(toMillis)
                .metrics(List.of(meter))
                .build();

        client.send(meters);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(client)) {
            return type.cast(client);
        }
        return MeteringRecorder.super.unwrap(type);
    }
}

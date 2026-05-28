/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;

import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.bling.emit.store.MeteringLogStore;
import com.oracle.pic.kiev.Transaction;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeteringSupportTest {
    @Test
    void supportsOverlappingSections() throws Exception {
        RecordingMeteringLogStores recording = new RecordingMeteringLogStores();
        MeteringSupport support = new MeteringSupport(recording.logStores());
        Context context = Context.create();
        context.register(mock(Transaction.class));

        support.startMeteredSection(context, "outer", "compartment-1", "resource-1", Map.of("phase", "outer"));
        support.startMeteredSection(context, "inner", "compartment-2", "resource-2", Map.of("phase", "inner"));
        support.endMeteredSection(context, "outer", Map.of("result", "outer-ok"));
        support.endMeteredSection(context, "inner", Map.of("result", "inner-ok"));

        assertThat(recording.records(), hasSize(2));
        assertThat(recording.records().getFirst().meterName(), is("outer"));
        assertThat(recording.records().getFirst().compartmentId(), is("compartment-1"));
        assertThat(recording.records().getFirst().resourceId(), is("resource-1"));
        assertThat(recording.records().getFirst().tags(), containsString("\"phase\":\"outer\""));
        assertThat(recording.records().getFirst().tags(), containsString("\"result\":\"outer-ok\""));
        assertThat(recording.records().get(1).meterName(), is("inner"));
        assertThat(recording.records().get(1).compartmentId(), is("compartment-2"));
        assertThat(recording.records().get(1).resourceId(), is("resource-2"));
        assertThat(recording.records().get(1).tags(), containsString("\"phase\":\"inner\""));
        assertThat(recording.records().get(1).tags(), containsString("\"result\":\"inner-ok\""));
    }

    @Test
    void propagatesSnapshotToIndependentSectionStack() throws Exception {
        RecordingMeteringLogStores recording = new RecordingMeteringLogStores();
        MeteringSupport sourceSupport = new MeteringSupport(recording.logStores());
        MeteringSupport targetSupport = new MeteringSupport(recording.logStores());
        MeteredSectionContextPropagationProvider provider = new MeteredSectionContextPropagationProvider();
        Context source = Context.create();
        Context target = Context.create();
        source.register(mock(Transaction.class));
        target.register(mock(Transaction.class));

        Optional<MeteringSupport.MeteredSectionsSnapshot> snapshot = Contexts.runInContextWithThrow(source, () -> {
            sourceSupport.startMeteredSection(source, "source", "compartment-1", "resource-1", Map.of("phase", "source"));
            return provider.data();
        });

        Contexts.runInContextWithThrow(target, () -> {
            provider.propagateData(snapshot);
            targetSupport.startMeteredSection(target, "target", "compartment-2", "resource-2", Map.of("phase", "target"));
            targetSupport.endMeteredSection(target, "source", Map.of("result", "source-ok"));
            targetSupport.endMeteredSection(target, "target", Map.of("result", "target-ok"));
            provider.clearData(snapshot);
            return null;
        });

        sourceSupport.endMeteredSection(source, "source", Map.of("result", "source-local"));

        assertThat(recording.records(), hasSize(3));
        assertThat(recording.records().getFirst().meterName(), is("source"));
        assertThat(recording.records().getFirst().tags(), containsString("\"phase\":\"source\""));
        assertThat(recording.records().getFirst().tags(), containsString("\"result\":\"source-ok\""));
        assertThat(recording.records().get(1).meterName(), is("target"));
        assertThat(recording.records().get(1).tags(), containsString("\"phase\":\"target\""));
        assertThat(recording.records().get(1).tags(), containsString("\"result\":\"target-ok\""));
        assertThat(recording.records().get(2).meterName(), is("source"));
        assertThat(recording.records().get(2).tags(), containsString("\"phase\":\"source\""));
        assertThat(recording.records().get(2).tags(), containsString("\"result\":\"source-local\""));
    }

    private static final class RecordingMeteringLogStores {
        private final java.util.List<Record> records = new java.util.ArrayList<>();
        private final java.util.Queue<String> meterNames = new java.util.ArrayDeque<>();
        private final MeteringLogStores logStores;

        private RecordingMeteringLogStores() throws Exception {
            MeteringLogStore logStore = mock(MeteringLogStore.class);
            logStores = mock(MeteringLogStores.class);
            when(logStores.getByMeterName(anyString())).thenAnswer(invocation -> {
                meterNames.add(invocation.getArgument(0));
                return logStore;
            });
            doAnswer(invocation -> {
                records.add(new Record(meterNames.remove(),
                                       invocation.getArgument(1),
                                       invocation.getArgument(2),
                                       invocation.getArgument(6)));
                return null;
            }).when(logStore).addMeter(org.mockito.ArgumentMatchers.any(),
                                       org.mockito.ArgumentMatchers.anyString(),
                                       org.mockito.ArgumentMatchers.anyString(),
                                       org.mockito.ArgumentMatchers.any(),
                                       org.mockito.ArgumentMatchers.any(),
                                       org.mockito.ArgumentMatchers.anyDouble(),
                                       org.mockito.ArgumentMatchers.anyString());
        }

        private MeteringLogStores logStores() {
            return logStores;
        }

        private List<Record> records() {
            return records;
        }
    }

    private record Record(String meterName, String resourceId, String compartmentId, String tags) {
    }
}

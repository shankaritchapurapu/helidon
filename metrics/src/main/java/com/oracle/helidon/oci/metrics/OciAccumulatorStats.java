/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.concurrent.atomic.LongAdder;

final class OciAccumulatorStats {

    private final LongAdder compactions = new LongAdder();
    private final LongAdder compactedSamples = new LongAdder();
    private final LongAdder droppedBuckets = new LongAdder();
    private final LongAdder droppedSamples = new LongAdder();

    void recordCompaction(long samples) {
        compactions.increment();
        compactedSamples.add(samples);
    }

    void recordDrop(long buckets, long samples) {
        droppedBuckets.add(buckets);
        droppedSamples.add(samples);
    }

    Snapshot drain() {
        return new Snapshot(compactions.sumThenReset(),
                            compactedSamples.sumThenReset(),
                            droppedBuckets.sumThenReset(),
                            droppedSamples.sumThenReset());
    }

    record Snapshot(long compactions, long compactedSamples, long droppedBuckets, long droppedSamples) {
        boolean isEmpty() {
            return compactions == 0L && compactedSamples == 0L && droppedBuckets == 0L && droppedSamples == 0L;
        }
    }
}

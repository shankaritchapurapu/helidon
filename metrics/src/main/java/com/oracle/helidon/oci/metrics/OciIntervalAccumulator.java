/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.oracle.pic.telemetry.commons.metrics.model.Observation;

final class OciIntervalAccumulator {
    private static final System.Logger LOGGER = System.getLogger(OciIntervalAccumulator.class.getName());
    private static final long MILLIS_PER_SECOND = 1_000L;

    private final String meterName;
    private final int maxPendingSeconds;
    private final int maxRawSamplesPerSecond;
    private final long pressureLogIntervalNanos;
    private final OciAccumulatorStats stats;
    private final NavigableMap<Long, Bucket> buckets = new TreeMap<>();
    private final AtomicLong nextCompactionLogNanos = new AtomicLong();
    private final AtomicLong nextDropLogNanos = new AtomicLong();
    private final AtomicBoolean loggedCompactionMetricNames = new AtomicBoolean();
    private final AtomicBoolean loggedDropMetricNames = new AtomicBoolean();

    OciIntervalAccumulator(String meterName,
                           OciMetricsAccumulatorConfig config,
                           int maxRawSamplesPerSecond,
                           OciAccumulatorStats stats) {
        this.meterName = meterName;
        this.maxPendingSeconds = config.maxPendingSeconds();
        this.maxRawSamplesPerSecond = maxRawSamplesPerSecond;
        this.pressureLogIntervalNanos = config.pressureLogInterval().toNanos();
        this.stats = stats;
    }

    void record(long timestampMillis, double value) {
        long secondMillis = Math.floorDiv(timestampMillis, MILLIS_PER_SECOND) * MILLIS_PER_SECOND;
        synchronized (this) {
            Bucket bucket = buckets.computeIfAbsent(secondMillis, Bucket::new);
            bucket.record(value);
            dropOldestIfNeeded();
        }
    }

    synchronized void restore(List<Observation> observations) {
        observations.forEach(this::restore);
    }

    synchronized List<Observation> drainClosed(long nowMillis) {
        long currentSecondMillis = Math.floorDiv(nowMillis, MILLIS_PER_SECOND) * MILLIS_PER_SECOND;
        return drain(bucketSecond -> bucketSecond < currentSecondMillis);
    }

    synchronized List<Observation> drainAll() {
        return drain(bucketSecond -> true);
    }

    synchronized int pendingBucketCount() {
        return buckets.size();
    }

    private List<Observation> drain(BucketPredicate predicate) {
        if (buckets.isEmpty()) {
            return List.of();
        }
        List<Observation> observations = new ArrayList<>();
        var iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!predicate.test(entry.getKey())) {
                continue;
            }
            observations.addAll(entry.getValue().observations());
            iterator.remove();
        }
        return observations.isEmpty() ? List.of() : observations;
    }

    private void restore(Observation observation) {
        Bucket bucket = buckets.computeIfAbsent(observation.getTimestamp(), Bucket::new);
        bucket.restore(observation);
        dropOldestIfNeeded();
    }

    private void dropOldestIfNeeded() {
        while (buckets.size() > maxPendingSeconds) {
            var oldest = buckets.pollFirstEntry();
            if (oldest == null) {
                return;
            }
            long droppedSamples = oldest.getValue().count();
            stats.recordDrop(1L, droppedSamples);
            logDropRateLimited(oldest.getKey(), droppedSamples);
        }
    }

    private void recordCompaction(long samples) {
        stats.recordCompaction(samples);
        logCompactionRateLimited(samples);
    }

    private void logDropRateLimited(long secondMillis, long samples) {
        if (claimLogSlot(nextDropLogNanos)) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "Dropping old OCI metrics accumulator bucket; meter={0}, secondMillis={1}, samples={2}{3}",
                       meterName,
                       secondMillis,
                       samples,
                       dropMetricNamesHint());
        }
    }

    private void logCompactionRateLimited(long samples) {
        if (claimLogSlot(nextCompactionLogNanos)) {
            LOGGER.log(System.Logger.Level.INFO,
                       "Compacting OCI metrics accumulator bucket; meter={0}, samples={1}{2}",
                       meterName,
                       samples,
                       compactionMetricNamesHint());
        }
    }

    private String compactionMetricNamesHint() {
        if (!loggedCompactionMetricNames.compareAndSet(false, true)) {
            return "";
        }
        return "; pressureMetrics=["
                + OciMetricsPublisher.ACCUMULATOR_COMPACTIONS + ", "
                + OciMetricsPublisher.ACCUMULATOR_COMPACTED_SAMPLES + ", "
                + OciMetricsPublisher.ACCUMULATOR_PENDING_BUCKETS + "]";
    }

    private String dropMetricNamesHint() {
        if (!loggedDropMetricNames.compareAndSet(false, true)) {
            return "";
        }
        return "; pressureMetrics=["
                + OciMetricsPublisher.ACCUMULATOR_DROPPED_BUCKETS + ", "
                + OciMetricsPublisher.ACCUMULATOR_DROPPED_SAMPLES + ", "
                + OciMetricsPublisher.ACCUMULATOR_PENDING_BUCKETS + "]";
    }

    private boolean claimLogSlot(AtomicLong nextLogNanos) {
        long now = System.nanoTime();
        long next = nextLogNanos.get();
        if (now < next) {
            return false;
        }
        return nextLogNanos.compareAndSet(next, now + pressureLogIntervalNanos);
    }

    private interface BucketPredicate {
        boolean test(long secondMillis);
    }

    private final class Bucket {
        private final long timestampMillis;
        private final List<Observation> restoredObservations = new ArrayList<>();
        private List<Double> rawSamples = new ArrayList<>();
        private boolean compacted;
        private long restoredCount;
        private long count;
        private double sum;
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;

        private Bucket(long timestampMillis) {
            this.timestampMillis = timestampMillis;
        }

        private void record(double value) {
            if (!compacted && rawSamples.size() < maxRawSamplesPerSecond) {
                rawSamples.add(value);
                return;
            }
            if (!compacted) {
                compactRawSamples();
            }
            count++;
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        private void restore(Observation observation) {
            restoredObservations.add(observation);
            restoredCount += observation.getCount();
        }

        private long count() {
            return restoredCount + (compacted ? count : rawSamples.size());
        }

        private List<Observation> observations() {
            List<Observation> restored = List.copyOf(restoredObservations);
            if (!compacted) {
                if (rawSamples.isEmpty()) {
                    return restored;
                }
                List<Observation> result = new ArrayList<>(restored.size() + rawSamples.size());
                result.addAll(restored);
                rawSamples.forEach(value -> result.add(new Observation(timestampMillis, value, 1)));
                return result;
            }

            if (count == 0L) {
                return restored;
            }
            List<Observation> result = new ArrayList<>(restored.size() + 3);
            result.addAll(restored);
            if (count == 1L) {
                addObservation(result, min, 1L);
            } else if (count == 2L) {
                addObservation(result, min, 1L);
                addObservation(result, max, 1L);
            } else {
                long middleCount = count - 2L;
                double middleMean = (sum - min - max) / middleCount;
                addObservation(result, min, 1L);
                addObservation(result, max, 1L);
                addObservation(result, middleMean, middleCount);
            }
            return result;
        }

        private void compactRawSamples() {
            recordCompaction(rawSamples.size());
            rawSamples.forEach(value -> {
                count++;
                sum += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            });
            rawSamples = List.of();
            compacted = true;
        }

        private void addObservation(List<Observation> result, double value, long observationCount) {
            long remaining = observationCount;
            while (remaining > 0L) {
                int chunk = (int) Math.min(Integer.MAX_VALUE, remaining);
                result.add(new Observation(timestampMillis, value, chunk));
                remaining -= chunk;
            }
        }
    }
}

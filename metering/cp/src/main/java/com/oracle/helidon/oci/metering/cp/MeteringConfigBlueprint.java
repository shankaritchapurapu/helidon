/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Control plane metering configuration mapped from {@code oci.metering}.
 * <p>
 * This blueprint adapts Helidon config to native metering-agent configuration. For native-backed settings, prefer
 * optional blueprint values over Helidon defaults so the native configuration defaults remain authoritative.
 */
@Prototype.Blueprint
@Prototype.Configured
interface MeteringConfigBlueprint {
    /**
     * Whether to start the native metering agent.
     *
     * @return {@code true} to start the agent
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean enabled();

    /**
     * Bling ingest endpoint.
     *
     * @return endpoint
     */
    @Option.Configured
    String endpoint();

    /**
     * Metering client ID.
     *
     * @return client ID
     */
    @Option.Configured
    String clientId();

    /**
     * Maximum workers for the control plane metering path.
     *
     * @return max workers
     */
    @Option.Configured
    Optional<Integer> maxWorkers();

    /**
     * Period used by the emitter for metering work.
     *
     * @return metering period
     */
    @Option.Configured
    Optional<Duration> meteringPeriod();

    /**
     * Duration of an archive lease.
     *
     * @return lease duration
     */
    @Option.Configured
    Optional<Duration> leaseDuration();

    /**
     * Whether canary behavior is disabled.
     *
     * @return {@code true} when disabled
     */
    @Option.Configured
    Optional<Boolean> canaryDisabled();

    /**
     * Bucket configurations for the control plane metering path.
     *
     * @return bucket configurations
     */
    @Option.Configured
    List<MeteringBucketConfig> bucketConfigs();

    /**
     * Version 3 bucket configurations for the control plane metering path.
     *
     * @return version 3 bucket configurations
     */
    @Option.Configured
    List<MeteringBucketConfigV3> bucketV3Configs();

    /**
     * OCI region used by the metering agent. If not specified, uses the region from the current environment.
     *
     * @return region
     */
    @Option.Configured
    Optional<String> region();

    /**
     * Host name to report in generated metering payloads.
     *
     * @return host name
     */
    @Option.Configured
    Optional<String> hostName();

    /**
     * Maximum archive workers for the control plane metering path.
     *
     * @return max archive workers
     */
    @Option.Configured
    Optional<Integer> maxArchiveWorkers();

    /**
     * Scan page size for the control plane metering path.
     *
     * @return scan page size
     */
    @Option.Configured
    Optional<Integer> scanPageSize();

    /**
     * Maximum writes per transaction for the control plane metering path.
     *
     * @return max writes per transaction
     */
    @Option.Configured
    Optional<Integer> maxWritesPerTransaction();

    /**
     * Retention period for archived data.
     *
     * @return retention period
     */
    @Option.Configured
    Optional<Duration> retentionPeriod();

    /**
     * Whether archive lease checking should be skipped.
     *
     * @return {@code true} to skip archive lease checks
     */
    @Option.Configured
    Optional<Boolean> skipArchiveLeaseCheck();

    /**
     * Lease DAO scan page size.
     *
     * @return page size
     */
    @Option.Configured
    Optional<Integer> leaseDaoScanPageSize();

    /**
     * Whether fast catchup mode is enabled.
     *
     * @return {@code true} when enabled
     */
    @Option.Configured
    Optional<Boolean> fastCatchupModeEnabled();

    /**
     * Whether duplicate writes to version 2 buckets are disabled.
     *
     * @return {@code true} when duplicate version 2 writes are disabled
     */
    @Option.Configured
    Optional<Boolean> duplicateV2WritesDisabled();

    /**
     * Maximum time allowed for archiving.
     *
     * @return archiver timeout
     */
    @Option.Configured
    Optional<Duration> archiverTimeout();
}

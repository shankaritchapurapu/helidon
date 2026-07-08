/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.helidon.service.registry.Service;

import com.oracle.helidon.oci.kiev.Kiev;
import com.oracle.helidon.oci.kiev.KievTransactionSupport;
import com.oracle.pic.kiev.Bucket;
import com.oracle.pic.kiev.Transaction;
import com.oracle.pic.kiev.exceptions.DuplicateKeyException;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import com.oracle.pic.kiev.mapping.MappedHashBucket;
import com.oracle.pic.kiev.mapping.Page;

/**
 * Application service used by the reference endpoint.
 */
@Service.Singleton
class DataPlaneReferenceService {
    static final String DATA_STORE_NAME = "helidon-data-plane-example";
    private static final String ACTIVE = "ACTIVE";
    private static final String BUCKET_NAME = "data_plane_robots";
    private static final String BUCKET_DESCRIPTION = "Helidon OCI data-plane example robots";
    private static final String HASH_KEY_COLUMN = "id";
    private static final String SEED_ROBOT_ID = "robot-1";
    private static final int PAGE_SIZE = 100;

    private final MappedHashBucket<String, RobotEntity> bucket;

    @Service.Inject
    DataPlaneReferenceService(@Service.Named(DATA_STORE_NAME) MappedDataStore mappedDataStore,
                              @Service.Named(DATA_STORE_NAME) KievTransactionSupport transactionSupport) {
        this.bucket = mappedDataStore.getOrCreateBucket(BUCKET_NAME,
                                                        BUCKET_DESCRIPTION,
                                                        String.class,
                                                        RobotEntity.class);
        initializeBucket(transactionSupport);
    }

    RobotCollection list(String compartmentId, String displayName) {
        return list(null, compartmentId, displayName);
    }

    @Kiev.Transaction(value = DATA_STORE_NAME, name = "data-plane-list", readOnly = true)
    RobotCollection list(Transaction tx, String compartmentId, String displayName) {
        List<RobotSummary> items = readAll(tx).stream()
                .filter(robot -> compartmentId == null || compartmentId.equals(robot.compartmentId))
                .filter(robot -> displayName == null || displayName.equals(robot.displayName))
                .map(DataPlaneReferenceService::toSummary)
                .toList();
        return new RobotCollection(items, items.size());
    }

    Optional<Robot> get(String id) {
        return get(null, id);
    }

    @Kiev.Transaction(value = DATA_STORE_NAME, name = "data-plane-get", readOnly = true)
    Optional<Robot> get(Transaction tx, String id) {
        return bucket.get(tx, id)
                .map(DataPlaneReferenceService::toRobot);
    }

    Robot create(String displayName, String compartmentId) {
        return create(null, displayName, compartmentId);
    }

    @Kiev.Transaction(value = DATA_STORE_NAME, name = "data-plane-create")
    Robot create(Transaction tx, String displayName, String compartmentId) {
        RobotEntity robot = new RobotEntity(newRobotId(),
                                            compartmentId,
                                            displayName,
                                            ACTIVE);
        try {
            return toRobot(bucket.insert(tx, robot));
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to create a unique robot id", e);
        }
    }

    Optional<Robot> update(String id, String displayName) {
        return update(null, id, displayName);
    }

    @Kiev.Transaction(value = DATA_STORE_NAME, name = "data-plane-update")
    Optional<Robot> update(Transaction tx, String id, String displayName) {
        return bucket.get(tx, id)
                .map(existing -> {
                    RobotEntity updated = new RobotEntity(existing.id,
                                                          existing.compartmentId,
                                                          displayName,
                                                          existing.lifecycleState);
                    return toRobot(bucket.put(tx, updated));
                });
    }

    Optional<Robot> delete(String id) {
        return delete(null, id);
    }

    @Kiev.Transaction(value = DATA_STORE_NAME, name = "data-plane-delete")
    Optional<Robot> delete(Transaction tx, String id) {
        Optional<RobotEntity> existing = bucket.get(tx, id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        bucket.delete(tx, id);
        return existing.map(DataPlaneReferenceService::toRobot);
    }

    int count() {
        return count(null);
    }

    @Kiev.Transaction(value = DATA_STORE_NAME, name = "data-plane-count", readOnly = true)
    int count(Transaction tx) {
        return readAll(tx).size();
    }

    private void initializeBucket(KievTransactionSupport transactionSupport) {
        try {
            transactionSupport.execute("data-plane-initialize", false, tx -> {
                if (bucket.get(tx, SEED_ROBOT_ID).isEmpty()) {
                    bucket.insert(tx, seedRobot());
                }
                return null;
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize the Kiev robot bucket", e);
        }
    }

    private List<RobotEntity> readAll(Transaction tx) {
        List<RobotEntity> robots = new ArrayList<>();
        Page<RobotEntity> page = bucket.rangeGet(tx, PAGE_SIZE, Bucket.Direction.ASCENDING);
        robots.addAll(page.results());
        while (page.hasNext()) {
            String nextId = page.next().keys().getString(HASH_KEY_COLUMN);
            page = bucket.rangeGet(tx,
                                   nextId,
                                   PAGE_SIZE,
                                   Bucket.Direction.ASCENDING,
                                   Bucket.Bounding.EXCLUSIVE);
            robots.addAll(page.results());
        }
        return robots;
    }

    private static RobotEntity seedRobot() {
        return new RobotEntity(SEED_ROBOT_ID,
                               "ocid1.compartment.oc1..example",
                               "Reference Robot",
                               ACTIVE);
    }

    private static String newRobotId() {
        return "robot-" + UUID.randomUUID();
    }

    private static Robot toRobot(RobotEntity robot) {
        return new Robot(robot.id, robot.compartmentId, robot.displayName, robot.lifecycleState);
    }

    private static RobotSummary toSummary(RobotEntity robot) {
        return new RobotSummary(robot.id, robot.compartmentId, robot.displayName, robot.lifecycleState);
    }
}

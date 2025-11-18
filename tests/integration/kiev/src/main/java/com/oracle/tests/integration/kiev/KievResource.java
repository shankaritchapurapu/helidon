/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.kiev;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.oracle.pic.kiev.DataStoreConfig;
import com.oracle.pic.kiev.DirectDbStoreConfig;
import com.oracle.pic.kiev.Transaction;
import com.oracle.pic.kiev.exceptions.CommitConflictException;
import com.oracle.pic.kiev.mapping.InMemoryDataStoreConfig;
import com.oracle.pic.kiev.mapping.MappedDataStore;
import com.oracle.pic.kiev.mapping.MappedHashBucket;
import com.oracle.pic.kiev.mapping.annotations.Column;
import com.oracle.pic.kiev.mapping.annotations.ColumnType;
import com.oracle.pic.kiev.mapping.annotations.HashKey;
import com.oracle.pic.kiev.mapping.annotations.KievEntity;

import java.util.logging.Logger;
import java.util.Optional;

import io.helidon.config.Config;

/**
 * A simple JAX-RS resource to show Kiev integration.
 */
@Path("/kiev")
@ApplicationScoped
public class KievResource {
    private static final Logger LOGGER = Logger.getLogger(KievResource.class.getName());

    MappedDataStore mappedDataStore;

    @Inject
    Config config;

    @KievEntity
    public static class Foo {
        @HashKey
        @Column(type = ColumnType.LONG)
        public Long id;

        @Column(type = ColumnType.STRING, length = 99)
        public String fooValue;
    }

    /**
     * Set a value into Helidon Test Bucket.
     *
     * @return {@link Response}
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setIntoHelidonTestBucket(Foo fooValue) throws CommitConflictException {
        mappedDataStore = setupKievDataStore();
        // Based on the entity defined above, you'll get the mapped bucket for that entity.
        MappedHashBucket<Long, Foo> fooBucket =
                mappedDataStore.getOrCreateBucket("helidon_bucket", "helidon test bucket", Long.class, Foo.class);
        // Insert
        try (Transaction txn = mappedDataStore.beginTransaction("Insert helidon test transaction")) {
            fooBucket.insert(txn, fooValue);
            txn.commit();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Return a value from Helidon Test Bucket.
     *
     * @return {@link Response}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFromHelidonTestBucket(@QueryParam("bucketId") long itemId) throws CommitConflictException {
        mappedDataStore = setupKievDataStore();
        // Based on the entity defined above, you'll get the mapped bucket for that entity.
        MappedHashBucket<Long, Foo> fooBucket =
                mappedDataStore.getOrCreateBucket("helidon_bucket", "helidon test bucket", Long.class, Foo.class);
        Optional<Foo> bucketContent;

        // Retrieve
        try (Transaction txn = mappedDataStore.beginTransaction("Get helidon test transaction")) {
            bucketContent = fooBucket.get(txn, itemId);
            txn.commit();
        }

        closeKievDataStore(mappedDataStore);
        return Response.status(Response.Status.OK).entity(bucketContent).build();
    }

    /*
     * This is how you can get an instance of a MappedDataStore.
     */
    MappedDataStore setupKievDataStore() {
        if(this.mappedDataStore != null) {
            return mappedDataStore;
        }
        /*
         * First, create a DataStoreConfig for Kiev to use to connect to Oracle or in-memory.
         *
         * The first argument is the data store name, which identifies the data store.
         * Kiev makes sure this name matches the configured name of the data store you
         * reach in order to prevent accidental connections; you think you're connected
         * to accounting, but the URL really connects you to inventory.
         *
         * The second argument is the application name; it is completely free, though it
         * can't be null and must be less than 80 characters. A well-behaved Kiev
         * application will use this parameter to identify itself in logs and transaction histories.
         *
         * The next three arguments are connection info for Kiev to use to connect
         * to Oracle: the jdbc url, username, and password.
         */
        DataStoreConfig dsc = null;
        if("in-memory".equals(config.get("kiev.clientType").asString().get())) {
            dsc = new InMemoryDataStoreConfig(
                    config.get("kiev.storeName").asString().get(),
                    config.get("kiev.appName").asString().get());
        } else {
            dsc = new DirectDbStoreConfig(
                    config.get("kiev.storeName").asString().get(),
                    config.get("kiev.appName").asString().get(),
                    config.get("kiev.jdbcURL").asString().get(),  // something like "jdbc:oracle:thin:@//localhost:1521/DevDB"
                    config.get("kiev.userName").asString().get(),
                    config.get("kiev.password").asString().get());
        }

        /*
         * Calling initialize() on the DataStoreConfig will connect to Oracle and
         * create all tables Kiev uses to keep track of internal state.  You only
         * have to call this once (but it's safe to call over and over).
         */
        dsc.initialize();

        /*
         * Calling connect() on the DataStoreConfig connects to Oracle, verifies that
         * Kiev has been initialized there, and returns a com.oracle.pic.kiev.DataStore,
         * which here is wrapped with a MappedDataStore that our application can use.
         */
        return new MappedDataStore(dsc.connect());
    }

    /*
     * When you're done playing with a MappedDataStore, it's good hygiene to close it,
     * which will do things like release connection pools to Oracle.
     */
    void closeKievDataStore(MappedDataStore mappedDataStore) {
        mappedDataStore.close();
    }

}

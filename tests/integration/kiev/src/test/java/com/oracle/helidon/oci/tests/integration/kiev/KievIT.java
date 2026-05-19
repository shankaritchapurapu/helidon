/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tests.integration.kiev;

import java.security.Security;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.helidon.service.registry.Services;
import io.helidon.webserver.testing.junit5.ServerTest;

import com.oracle.jipher.provider.JipherJCE;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@ServerTest
@TestMethodOrder(OrderAnnotation.class)
public class KievIT {
    private static final String TEST_ID = "key-" + UUID.randomUUID();
    private static final String FIRST_KEY = TEST_ID + "-1";
    private static final String SECOND_KEY = TEST_ID + "-2";
    private static final String DELETE_KEY = TEST_ID + "-delete";
    private static final String ORIGINAL_VALUE = "value";
    private static final String UPDATED_VALUE = "updated-value";
    private static final String SECOND_VALUE = "second-value";
    private static final String DELETE_VALUE = "delete-value";

    static {
        System.setProperty("useJipherJceProvider", "true");
        Security.addProvider(new JipherJCE());
    }

    @BeforeAll
    static void cleanupExistingData() {
        KievStoreService service = Services.get(KievStoreService.class);
        deleteCreatedItems(service);
        assertThat(createdItems(service), hasSize(0));
    }

    @Order(1)
    @Test
    void testPostItems() {
        KievStoreService service = Services.get(KievStoreService.class);
        assertThat(service.post(FIRST_KEY, ORIGINAL_VALUE), is(ORIGINAL_VALUE));
        assertThat(service.post(SECOND_KEY, SECOND_VALUE), is(SECOND_VALUE));
    }

    @Order(2)
    @Test
    void testGetItem() {
        assertThat(Services.get(KievStoreService.class).get(FIRST_KEY), is(Optional.of(ORIGINAL_VALUE)));
    }

    @Order(3)
    @Test
    void testPutItem() {
        KievStoreService service = Services.get(KievStoreService.class);
        assertThat(service.put(FIRST_KEY, UPDATED_VALUE), is(UPDATED_VALUE));
        assertThat(service.get(FIRST_KEY), is(Optional.of(UPDATED_VALUE)));
    }

    @Order(4)
    @Test
    void testListItems() {
        List<StoreItem> items = createdItems(Services.get(KievStoreService.class));
        assertThat(items, hasSize(2));
        assertThat(value(items, FIRST_KEY), is(Optional.of(UPDATED_VALUE)));
        assertThat(value(items, SECOND_KEY), is(Optional.of(SECOND_VALUE)));
    }

    @Order(5)
    @Test
    void testDeleteItem() {
        KievStoreService service = Services.get(KievStoreService.class);
        assertThat(service.post(DELETE_KEY, DELETE_VALUE), is(DELETE_VALUE));
        assertThat(service.delete(DELETE_KEY), is(Optional.of(DELETE_VALUE)));
        assertThat(service.get(DELETE_KEY), is(Optional.empty()));
        assertThat(service.delete(DELETE_KEY), is(Optional.empty()));
    }

    @AfterAll
    static void cleanupData() {
        KievStoreService service = Services.get(KievStoreService.class);
        deleteCreatedItems(service);
        assertThat(service.get(FIRST_KEY), is(Optional.empty()));
        assertThat(service.get(SECOND_KEY), is(Optional.empty()));
        assertThat(service.get(DELETE_KEY), is(Optional.empty()));
    }

    private static void deleteCreatedItems(KievStoreService service) {
        service.delete(FIRST_KEY);
        service.delete(SECOND_KEY);
        service.delete(DELETE_KEY);
    }

    private static List<StoreItem> createdItems(KievStoreService service) {
        return service.list()
                .stream()
                .filter(item -> item.id.startsWith(TEST_ID))
                .toList();
    }

    private static Optional<String> value(List<StoreItem> items, String key) {
        return items.stream()
                .filter(item -> item.id.equals(key))
                .map(item -> item.value)
                .findFirst();
    }
}

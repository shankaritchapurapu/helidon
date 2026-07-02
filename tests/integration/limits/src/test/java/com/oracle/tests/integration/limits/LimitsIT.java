/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.limits;

import java.util.List;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;

import com.oracle.helidon.oci.limits.LimitsAuthConfig;
import com.oracle.helidon.oci.limits.LimitsConfig;
import com.oracle.oci.limits.LimitsDPClient;
import com.oracle.oci.limits.model.ServiceGroup;
import com.oracle.oci.limits.model.ServiceLimitsItems;
import com.oracle.oci.limits.requests.GetServiceLimitServiceGroupsRequest;
import com.oracle.oci.limits.requests.GetServiceLimitsRequest;
import com.oracle.oci.limits.responses.GetServiceLimitServiceGroupsResponse;
import com.oracle.oci.limits.responses.GetServiceLimitsResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitsIT {
    private static final System.Logger LOGGER = System.getLogger(LimitsIT.class.getName());
    private static final String DATABASE_GROUP = "database";
    private static final String DATABASE_TAG = "database";

    @BeforeAll
    static void beforeAll() {
        LogConfig.configureRuntime();
    }

    @Test
    void moduleLocalAuthConfigIsSelected() {
        LimitsConfig config = Services.get(LimitsConfig.class);
        LimitsAuthConfig auth = config.auth().orElseThrow();
        assertEquals("service-principal", auth.authenticationMethod(),
                     "Limits auth method should be configured locally");
        assertTrue(auth.servicePrincipal().orElseThrow().usePlatformProvided(),
                   "Limits integration test should use platform-provided service-principal config");
    }

    @Test
    void clientUsesPhoenixEndpoint() {
        LimitsDPClient client = Services.get(LimitsDPClient.class);
        assertEquals("https://limits.us-phoenix-1.oci.oraclecloud.com", client.getEndpoint(),
                     "Limits endpoint should be derived from the test instance region");
    }

    @Test
    void getsServiceLimitServiceGroups() {
        LOGGER.log(System.Logger.Level.DEBUG, "Resolving LimitsDPClient");
        LimitsDPClient client = Services.get(LimitsDPClient.class);
        assertNotNull(client, "LimitsDPClient instance should not be null");

        LOGGER.log(System.Logger.Level.DEBUG, "Calling Limits DP getServiceLimitServiceGroups");
        GetServiceLimitServiceGroupsResponse response = client.getServiceLimitServiceGroups(
                GetServiceLimitServiceGroupsRequest.builder().build());

        assertNotNull(response, "getServiceLimitServiceGroups response should not be null");
        assertEquals(200, response.get__httpStatusCode__(), "getServiceLimitServiceGroups should succeed");

        List<ServiceGroup> groups = response.getItems();
        assertNotNull(groups, "service groups should not be null");
        assertFalse(groups.isEmpty(), "service groups should not be empty");
        LOGGER.log(System.Logger.Level.DEBUG, () -> "Received " + groups.size() + " service limit service groups");

        boolean allGroupsHaveNames = true;
        for (ServiceGroup group : groups) {
            String name = group.getName();
            LOGGER.log(System.Logger.Level.DEBUG, "Group Name: " + name);
            allGroupsHaveNames = allGroupsHaveNames && name != null && !name.isBlank();
        }
        assertTrue(allGroupsHaveNames, "service group names should be present");
    }

    @Test
    void getsDatabaseServiceLimits() {
        LimitsDPClient client = client();
        List<ServiceGroup> groups = serviceGroups(client);
        assertTrue(groups.stream().anyMatch(group -> DATABASE_GROUP.equals(group.getName())),
                () -> "Expected visible service group " + DATABASE_GROUP + " in " + groupNames(groups));

        LOGGER.log(System.Logger.Level.DEBUG,
                () -> "Calling Limits DP getServiceLimits for group " + DATABASE_GROUP + " and tag " + DATABASE_TAG);
        GetServiceLimitsResponse response = client.getServiceLimits(GetServiceLimitsRequest.builder()
                .group(DATABASE_GROUP)
                .tag(DATABASE_TAG)
                .build());

        assertNotNull(response, "getServiceLimits response should not be null");
        assertEquals(200, response.get__httpStatusCode__(), "getServiceLimits should succeed");

        List<ServiceLimitsItems> limits = response.getItems();
        assertNotNull(limits, "database service limits should not be null");
        assertFalse(limits.isEmpty(), "database service limits should not be empty");
        LOGGER.log(System.Logger.Level.DEBUG, () -> "Received " + limits.size() + " database service limits");

        boolean allLimitsHaveNames = true;
        for (ServiceLimitsItems limit : limits) {
            String name = limit.getName();
            LOGGER.log(System.Logger.Level.DEBUG, "Database Limit Name: " + name);
            allLimitsHaveNames = allLimitsHaveNames && name != null && !name.isBlank();
        }
        assertTrue(allLimitsHaveNames, "database service limit names should be present");
    }

    private static LimitsDPClient client() {
        LOGGER.log(System.Logger.Level.DEBUG, "Resolving LimitsDPClient");
        LimitsDPClient client = Services.get(LimitsDPClient.class);
        assertNotNull(client, "LimitsDPClient instance should not be null");
        return client;
    }

    private static List<ServiceGroup> serviceGroups(LimitsDPClient client) {
        LOGGER.log(System.Logger.Level.DEBUG, "Calling Limits DP getServiceLimitServiceGroups");
        GetServiceLimitServiceGroupsResponse response = client.getServiceLimitServiceGroups(
                GetServiceLimitServiceGroupsRequest.builder().build());

        assertNotNull(response, "getServiceLimitServiceGroups response should not be null");
        assertEquals(200, response.get__httpStatusCode__(), "getServiceLimitServiceGroups should succeed");

        List<ServiceGroup> groups = response.getItems();
        assertNotNull(groups, "service groups should not be null");
        assertFalse(groups.isEmpty(), "service groups should not be empty");
        return groups;
    }

    private static List<String> groupNames(List<ServiceGroup> groups) {
        return groups.stream()
                .map(ServiceGroup::getName)
                .toList();
    }
}

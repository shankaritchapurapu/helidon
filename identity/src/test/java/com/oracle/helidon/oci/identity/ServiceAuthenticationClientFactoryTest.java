/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

class ServiceAuthenticationClientFactoryTest extends BaseAuthenticationClientTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testClientCreation(boolean hardCodedKeys) {
        ServiceAuthenticationClient client = serviceAuthenticationClient(hardCodedKeys);
        assertThat(client, notNullValue());
    }
}


/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.integrations.oci.requestid;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import io.helidon.logging.jul.HelidonFormatter;
import io.helidon.microprofile.tests.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static io.helidon.integrations.oci.requestid.OciHeaderNames.OPC_REQUEST_ID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test request ID support. It requires dependency {@code com.oracle.helidon.oci:helidon-oci-request-id}
 * to be in classpath.
 */
@HelidonTest
class OciResourceTest {

    @Inject
    private WebTarget webTarget;

    /**
     * Checks opc-request-id is returned and matches the one in {@link OciResource}.
     */
    @Test
    void testRequestId() {
        Response response = webTarget.path("oci").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        String responseId = response.getHeaderString(OPC_REQUEST_ID);
        assertThat(responseId.split("/").length, is(3));
        String requestId = response.readEntity(String.class);
        assertThat(requestId, is(responseId));
    }

    /**
     * Checks returned opc-request-id is a downstream ID using JAX-RS Client (server side).
     */
    @Test
    void testClientRequestId() {
        testClientRequestIdAt("oci/client");
    }

    /**
     * Checks returned opc-request-id is a downstream ID using MP Client (server side).
     */
    @Test
    void testMpClientRequestId() {
        testClientRequestIdAt("oci/mpclient");
    }

    /**
     * Verifies that request ID is logged by server via MDC.
     */
    @Test
    void testLoggingMdc() {
        TestStreamHandler.output.reset();
        String requestId = webTarget.path("oci").request().get(String.class);
        String log = TestStreamHandler.output.toString();
        assertThat(log, containsString(requestId));
    }

    /**
     * Handler that logs to a byte array stream.
     */
    static public class TestStreamHandler extends StreamHandler {

        private static final ByteArrayOutputStream output = new ByteArrayOutputStream();

        public TestStreamHandler() {
            setOutputStream(output);
            setLevel(Level.ALL);
            setFormatter(new HelidonFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

        @Override
        public void close() {
            flush();
        }
    }

    /**
     * Verifies that returned request ID is a downstream ID.
     *
     * @param path resource path
     */
    private void testClientRequestIdAt(String path) {
        Response response = webTarget.path(path).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        String requestId = response.readEntity(String.class);
        assertThat(requestId.split("/").length, is(2));        // downstream ID
    }
}

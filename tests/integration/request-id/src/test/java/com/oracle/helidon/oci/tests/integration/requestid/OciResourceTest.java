/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.tests.integration.requestid;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import io.helidon.logging.jul.HelidonFormatter;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.common.requestid.OciRequestId.OCI_REQUEST_ID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * Test request ID support. It requires dependency {@code com.oracle.helidon.oci:helidon-oci-request-id}
 * to be in classpath.
 */
@HelidonTest
class OciResourceTest {

    @Inject
    private WebTarget webTarget;

    /**
     * Checks opc-request-id is returned and matches the one in
     * {@link com.oracle.helidon.oci.tests.integration.requestid.OciResource}.
     */
    @Test
    void testRequestId() {
        Response response = webTarget.path("oci").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        String responseId = response.getHeaderString(OCI_REQUEST_ID);
        assertThat(responseId.split("/").length, is(3));
        String requestId = response.readEntity(String.class);
        assertThat(requestId, is(responseId));
    }

    /**
     * Checks returned opc-request-id is a downstream ID using JAX-RS Client (server side).
     */
    @Test
    void testClientRequestId() {
        testClientRequestNoCustomerId("oci/client");
    }

    /**
     * Checks returned opc-request-id is a downstream ID using JAX-RS Client (server side).
     */
    @Test
    void testClientRequestIdWithCustomerId() {
        String customerId = "customerId";
        testClientRequestCustomerId("oci/client", customerId, customerId);
    }

    /**
     * Checks returned opc-request-id is a downstream ID using JAX-RS Client (server side).
     */
    @Test
    void testClientRequestIdWithCustomerIdInvalidChars() {
        testClientRequestCustomerId("oci/client", "customer{Id", "customerId");
    }

    /**
     * Checks returned opc-request-id is a downstream ID using JAX-RS Client (server side).
     */
    @Test
    void testClientRequestIdWithCustomerIdAndTraceId() {
        testClientRequestCustomerIdAndTraceId("oci/client", "customer-id", "trace-id");
    }

    /**
     * Checks returned opc-request-id is a downstream ID using MP Client (server side).
     */
    @Test
    void testMpClientRequestId() {
        testClientRequestNoCustomerId("oci/mpclient");
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
     * Verifies that returned request ID is a downstream ID.
     *
     * @param path resource path
     */
    private void testClientRequestNoCustomerId(String path) {
        // customer request (no id) ""
        Response response = webTarget.path(path).request().get();

        verifyResponse(response, "", null);
    }

    private void verifyResponse(Response response, String expectedCustomerId, String expectedTraceId) {
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        // response id must be the same as upstream request id (x-oci-upstream)
        String serverRequest = response.getHeaderString("x-oci-upstream");
        assertThat("The same id must be returned to the caller, that was generated for the request",
                   response.getHeaderString(OCI_REQUEST_ID),
                   is(serverRequest));

        String[] idParts = serverRequest.split("/");
        assertThat("ID should always have all three parts: " + serverRequest, idParts.length, is(3));
        String customerId = idParts[0];
        String traceId = idParts[1];
        String spanId = idParts[2];
        assertThat("Customer id must be preserved across all requests", customerId, is(expectedCustomerId));
        if (expectedTraceId != null) {
            assertThat("Trace id must be preserved across all requests", traceId, is(expectedTraceId));
        }
        assertThat(spanId, not(isEmptyString()));

        serverRequest = response.getHeaderString("x-oci-downstream-request");
        idParts = serverRequest.split("/");
        // downstream request and response ids must be the same as well
        assertThat("The same id must be returned to the caller, that was generated for the request (downstream)",
                   response.getHeaderString("x-oci-downstream-request"),
                   is(response.getHeaderString("x-oci-downstream-response")));
        assertThat("ID should always have all three parts: " + serverRequest, idParts.length, is(3));
        assertThat("Customer id must be preserved across all requests", idParts[0], is(customerId));
        assertThat("Trace id must be preserved across all requests", idParts[1], is(traceId));
        assertThat("Span ID must change for each request/response exchange", idParts[2], not(spanId));
    }

    private void testClientRequestCustomerId(String path, String customerId, String expectedCustomerId) {
        // customer request (no id) ""
        Response response = webTarget.path(path)
                .request()
                .header(OCI_REQUEST_ID, customerId)
                .get();
        verifyResponse(response, expectedCustomerId, null);
    }

    /**
     * Verifies that returned request ID is a downstream ID.
     *
     * @param path resource path
     */
    private void testClientRequestCustomerIdAndTraceId(String path, String customerId, String traceId) {
        // customer request (no id) ""
        Response response = webTarget.path(path)
                .request()
                .header(OCI_REQUEST_ID, customerId + "/" + traceId)
                .get();
        verifyResponse(response, customerId, traceId);
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
}

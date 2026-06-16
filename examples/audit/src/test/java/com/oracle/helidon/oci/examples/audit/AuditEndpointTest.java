/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.audit;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

@ServerTest
class AuditEndpointTest {
    private static final HeaderName AUDIT_SUMMARY_HEADER = HeaderNames.create("oci-splat-audit-event-summary");
    private static final HeaderName OPC_REQUEST_ID_HEADER = HeaderNames.create("opc-request-id");
    private static final HeaderName SKIP_AUDIT_HEADER = HeaderNames.create("oci-splat-audited");
    private static final HeaderName TENANT_HEADER = HeaderNames.create("x-audit-example-tenant");
    private static final HeaderName VERIFY_AUDIT_HEADER = HeaderNames.create("oci-splat-audit-verify");
    private static final HeaderName VERSION_HEADER = HeaderNames.create("x-audit-example-version");
    private static final String TENANT_ID = "ocid1.tenancy.oc1..aaaaaaaahelidonauditexample";

    private final Http1Client client;

    AuditEndpointTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void testApproveRouteReturnsSummaryWhenRequested() {
        try (Http1ClientResponse response = client.post("/audit/orders/approve")
                .queryParam("orderId", "order-789")
                .header(HeaderValues.CONTENT_TYPE_TEXT_PLAIN)
                .header(HeaderValues.ACCEPT_JSON)
                .header(OPC_REQUEST_ID_HEADER, "customer-123/trace-456")
                .header(TENANT_HEADER, TENANT_ID)
                .header(VERIFY_AUDIT_HEADER, "true")
                .submit("alice")) {
            assertThat(response.status(), is(Status.OK_200));
            assertSummary(response.headers().first(AUDIT_SUMMARY_HEADER).orElseThrow());
            assertThat(response.headers().first(VERSION_HEADER).orElseThrow(), is("v1"));

            String body = response.as(String.class);
            assertThat(body, containsString("\"orderId\":\"order-789\""));
            assertThat(body, containsString("\"approvedBy\":\"alice\""));
            assertThat(body, containsString("\"status\":\"APPROVED\""));
        }
    }

    @Test
    void testGetRequiresOrderId() {
        try (Http1ClientResponse response = client.get("/audit/orders")
                .header(HeaderValues.ACCEPT_JSON)
                .request()) {
            assertThat(response.status(), is(Status.BAD_REQUEST_400));
        }
    }

    @Test
    void testSkipAuditHeaderSuppressesSummary() {
        try (Http1ClientResponse response = client.get("/audit/orders")
                .queryParam("orderId", "order-456")
                .header(HeaderValues.ACCEPT_JSON)
                .header(VERIFY_AUDIT_HEADER, "true")
                .header(SKIP_AUDIT_HEADER, "true")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers().first(AUDIT_SUMMARY_HEADER).isPresent(), is(false));
            assertThat(response.headers().first(VERSION_HEADER).orElseThrow(), is("v1"));
        }
    }

    @Test
    void testVerificationHeaderAddsAuditSummary() {
        try (Http1ClientResponse response = client.get("/audit/orders")
                .queryParam("orderId", "order-123")
                .queryParam("expand", "details")
                .header(HeaderValues.ACCEPT_JSON)
                .header(OPC_REQUEST_ID_HEADER, "customer-123/trace-456")
                .header(TENANT_HEADER, TENANT_ID)
                .header(VERIFY_AUDIT_HEADER, "true")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertSummary(response.headers().first(AUDIT_SUMMARY_HEADER).orElseThrow());
            assertThat(response.headers().first(VERSION_HEADER).orElseThrow(), is("v1"));

            String body = response.as(String.class);
            assertThat(body, containsString("\"orderId\":\"order-123\""));
            assertThat(body, containsString("\"expand\":\"details\""));
            assertThat(body, containsString("\"status\":\"READY_FOR_APPROVAL\""));
        }
    }

    private static void assertSummary(String summary) {
        assertThat(summary, startsWith("[{\"eventId\":\""));
        assertThat(summary, not(containsString("\"eventGroupingId\"")));
    }
}

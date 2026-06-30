/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import io.helidon.webserver.testing.junit5.Socket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.oracle.pic.sherlock.collector.AuditLogger;
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.OperationSynchronousType;
import com.oracle.pic.sherlock.collector.AuditEventV2Validator;
import com.oracle.pic.sherlock.common.event.AuditEventV2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@RoutingTest
class AuditV2FeatureTest {

    private static final AtomicReference<AuditLogger> AUDIT_LOGGER_REF = new AtomicReference<>();
    private static final AuditLogger DELEGATING_LOGGER = event -> AUDIT_LOGGER_REF.get().log(event);
    private static final HeaderName AUDIT_SUMMARY_HEADER = HeaderNames.create("oci-splat-audit-event-summary");
    private static final HeaderName OPC_REQUEST_ID_HEADER = HeaderNames.create("opc-request-id");
    private static final HeaderName VERIFY_AUDIT_HEADER = HeaderNames.create("oci-splat-audit-verify");
    private static final String DEFAULT_TENANT_ID = "ocid1.tenancy.oc1..aaaaaaaahelidonaudittest";
    private static final String DEFAULT_COMPARTMENT_ID = "ocid1.compartment.oc1..aaaaaaaahelidonaudittest";
    private static final String DEFAULT_RESOURCE_ID = "audit-test-resource";
    private static final String SPECIAL_GROUPING_ID = "grouping \"quoted\" \\ slash \n tab\t ctrl\u0001";

    private AuditLogger auditLogger; // per-test mock

    private final DirectClient client;
    private final DirectClient adminClient;

    AuditV2FeatureTest(DirectClient client, @Socket("admin") DirectClient adminClient) {
        this.client = client;
        this.adminClient = adminClient;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        router.get("/get", (req, res) -> {
            var optional = AuditPayloadAppenders.current(req);
            if (optional.isEmpty()) {
                res.status(Status.NOT_ACCEPTABLE_406).send("AuditPayloadAppender not found");
            } else {
                AuditPayloadAppender appender = optional.get();
                appender.setEventName("test", OperationSynchronousType.None);
                appender.appendToAuditRios(List.of(new AuditRIO(DEFAULT_COMPARTMENT_ID, "resource-3")));
                appender.appendToAuditRios(List.of(new AuditRIO(DEFAULT_COMPARTMENT_ID, "resource-2")));
                res.status(Status.OK_200).send();
            }
        });
        router.get("/default", (req, res) -> res.status(Status.OK_200).send());
        router.get("/special-summary", (req, res) -> {
            AuditPayloadAppenders.current(req).orElseThrow().setEventGroupingId(SPECIAL_GROUPING_ID);
            res.status(Status.OK_200).send();
        });
        router.get("/accessor", (req, res) -> {
            boolean appenderPresent = AuditPayloadAppenders.current(req).isPresent()
                    && req.context().get(AuditPayloadAppender.class.getName(), AuditPayloadAppender.class).isPresent();
            if (appenderPresent) {
                res.status(Status.OK_200).send();
            } else {
                res.status(Status.NOT_ACCEPTABLE_406).send("AuditPayloadAppender not found");
            }
        });
    }

    @SetUpRoute("admin")
    static void setUpAdmin(HttpRouting.Builder router) {
        router.get("/named", (req, res) -> res.status(Status.OK_200).send());
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        AuditV2Config config = AuditV2Config.builder()
                .enabled(true)
                .respectSplatAuditedFlag(false)
                .eventSource("test-source")
                .tenantId(DEFAULT_TENANT_ID)
                .compartmentId(DEFAULT_COMPARTMENT_ID)
                .resourceId(DEFAULT_RESOURCE_ID)
                .resourceName("Audit test resource")
                .requestParameterRules(List.of(RuleConfig.builder()
                        .actions("GET")
                        .resources("/get")
                        .values("reqparam1")
                        .build()))
                .requestHeaderRules(List.of(
                        RuleConfig.builder()
                                .actions("GET")
                                .resources("/get")
                                .values("reqheader1")
                                .build(),
                        RuleConfig.builder()
                                .actions("GET")
                                .resources("/get")
                                .values("opc-request-id")
                                .build()))
                .responseHeaderRules(List.of(RuleConfig.builder()
                        .actions("GET")
                        .resources("/get")
                        .values("respheader1")
                        .build()))
                .buildPrototype();
        return List.of(AuditV2Feature.create(config, DELEGATING_LOGGER));
    }

    @BeforeEach
    void setUp() {
        auditLogger = Mockito.mock(AuditLogger.class);
        AUDIT_LOGGER_REF.set(auditLogger);
    }

    @Test
    void testMultiRioEventsUseUniqueIdsSharedGroupingAndJsonSummary() throws Exception {
        var captor = ArgumentCaptor.forClass(AuditEventV2.class);

        String summary;
        try (Http1ClientResponse response = client.get("/get")
                .queryParam("reqparam1", "param1")
                .queryParam("reqparam2", "param2")
                .header(HeaderNames.X_FORWARDED_FOR, "localhost")
                .header(OPC_REQUEST_ID_HEADER, "ID 1")
                .header(VERIFY_AUDIT_HEADER, "true")
                .header(HeaderNames.create("reqheader1"), "header1")
                .header(HeaderNames.create("x"), "header2")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            summary = response.headers().first(AUDIT_SUMMARY_HEADER).orElseThrow();
        }

        // tolerate async/different-thread completion
        Mockito.verify(auditLogger, Mockito.timeout(2000).times(2))
               .log(captor.capture());
        List<AuditEventV2> events = captor.getAllValues();
        List<SummaryEntry> summaryEntries = parseSummary(summary);

        assertThat(summaryEntries.size(), is(2));

        Set<String> eventIds = new HashSet<>();
        Set<String> groupingIds = new HashSet<>();
        List<String> resources = new ArrayList<>();
        for (AuditEventV2 event : events) {
            eventIds.add(event.getEventId());
            groupingIds.add(event.getData().getEventGroupingId());
            resources.add(event.getData().getResourceId());
            assertThat(event.getData().getIdentity().getTenantId(), is(DEFAULT_TENANT_ID));
            assertThat(event.getData().getCompartmentId(), is(DEFAULT_COMPARTMENT_ID));
            assertThat(event.getData().getEventName(), is("test"));
        }

        assertThat(eventIds.size(), is(2));
        assertThat(groupingIds.size(), is(1));
        String groupingId = groupingIds.iterator().next();
        assertThat(groupingId, notNullValue());
        assertThat(eventIds.contains(groupingId), is(false));
        assertThat(resources, containsInAnyOrder("resource-3", "resource-2"));
        assertThat(summaryEntries.stream().map(SummaryEntry::eventId).toList(),
                   is(events.stream().map(AuditEventV2::getEventId).toList()));
        assertThat(new HashSet<>(summaryEntries.stream().map(SummaryEntry::eventId).toList()), is(eventIds));
    }

    @Test
    void testSummaryJsonEscapesSpecialCharacters() {
        String summary;
        try (Http1ClientResponse response = client.get("/special-summary")
                .header(VERIFY_AUDIT_HEADER, "true")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            summary = response.headers().first(AUDIT_SUMMARY_HEADER).orElseThrow();
        }

        JsonArray entries = JsonParser.create(summary).readJsonArray();
        assertThat(entries.size(), is(1));
        JsonObject entry = entries.get(0).orElseThrow().asObject();
        UUID.fromString(entry.stringValue("eventId").orElseThrow());
        assertThat(entry.value("summaryHash").isPresent(), is(false));
    }

    @Test
    void testDefaultEventShapeIsSherlockValid() throws Exception {
        var captor = ArgumentCaptor.forClass(AuditEventV2.class);

        try (Http1ClientResponse response = client.get("/default")
                .header(VERIFY_AUDIT_HEADER, "true")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(parseSummary(response.headers().first(AUDIT_SUMMARY_HEADER).orElseThrow()).size(), is(1));
        }

        Mockito.verify(auditLogger, Mockito.timeout(2000).atLeastOnce()).log(captor.capture());
        AuditEventV2 event = captor.getAllValues()
                .stream()
                .filter(it -> "/default".equals(it.getData().getRequest().getPath()))
                .findFirst()
                .orElseThrow();
        assertThat(AuditEventV2Validator.validateAuditEvent(event), is(true));
        assertThat(AuditEventV2Validator.getPartitionId(event), is(DEFAULT_COMPARTMENT_ID));
        assertThat(event.getEventType(), is("com.oraclecloud.test-source.HttpRequest"));
        assertThat(event.getData().getEventName(), is("HttpRequest"));
        assertThat(event.getData().getIdentity().getTenantId(), is(DEFAULT_TENANT_ID));
        assertThat(event.getData().getCompartmentId(), is(DEFAULT_COMPARTMENT_ID));
        assertThat(event.getData().getResourceId(), is(DEFAULT_RESOURCE_ID));
        assertThat(event.getData().getResourceName(), is("Audit test resource"));
        assertThat(event.getData().getRequest().getPath(), is("/default"));
        assertThat(event.getData().getResponse().getStatus(), is("200 OK"));
    }

    @Test
    void testAuditPayloadAppenderAccessorFindsCurrentRequestAppender() {
        try (Http1ClientResponse response = client.get("/accessor").request()) {
            assertThat(response.status(), is(Status.OK_200));
        }
    }

    @Test
    void testConfigDefaults() {
        AuditV2Config config = AuditV2Config.builder().buildPrototype();

        assertThat(config.eventName(), is("HttpRequest"));
        assertThat(config.tenantId(), is(""));
        assertThat(config.compartmentId(), is(""));
        assertThat(config.resourceId(), is(""));
        assertThat(config.resourceName(), is(""));
    }

    @Test
    void testGenerateEventTypeFallsBackForBlankInputs() {
        assertThat(AuditV2Filter.generateEventType("service", "GetResource", OperationSynchronousType.None),
                   is("com.oraclecloud.service.GetResource"));
        assertThat(AuditV2Filter.generateEventType(null, "GetResource", OperationSynchronousType.None),
                   is(AuditV2Filter.EVENT_TYPE));
        assertThat(AuditV2Filter.generateEventType("", "GetResource", OperationSynchronousType.None),
                   is(AuditV2Filter.EVENT_TYPE));
        assertThat(AuditV2Filter.generateEventType("service", null, OperationSynchronousType.None),
                   is(AuditV2Filter.EVENT_TYPE));
        assertThat(AuditV2Filter.generateEventType("service", "", OperationSynchronousType.None),
                   is(AuditV2Filter.EVENT_TYPE));
    }

    @Test
    void testAuditRunsOnNamedSocket() throws Exception {
        var captor = ArgumentCaptor.forClass(AuditEventV2.class);

        try (Http1ClientResponse response = adminClient.get("/named")
                .header(VERIFY_AUDIT_HEADER, "true")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(parseSummary(response.headers().first(AUDIT_SUMMARY_HEADER).orElseThrow()).size(), is(1));
        }

        Mockito.verify(auditLogger, Mockito.timeout(2000).atLeastOnce()).log(captor.capture());
        AuditEventV2 event = captor.getAllValues()
                .stream()
                .filter(it -> "/named".equals(it.getData().getRequest().getPath()))
                .findFirst()
                .orElseThrow();
        assertThat(event.getData().getRequest().getPath(), is("/named"));
    }

    @Test
    void testCustomLoggerWritesAuditEventsToFile(@TempDir Path tempDir) throws Exception {
        Path auditFile = tempDir.resolve("custom.audit");
        AUDIT_LOGGER_REF.set(event -> Files.writeString(auditFile,
                                                         auditLine(event),
                                                         StandardCharsets.UTF_8,
                                                         StandardOpenOption.CREATE,
                                                         StandardOpenOption.APPEND));

        try (Http1ClientResponse response = client.get("/get")
                .queryParam("reqparam1", "param1")
                .header(OPC_REQUEST_ID_HEADER, "ID 1")
                .header(HeaderNames.create("reqheader1"), "header1")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
        }

        List<String> lines = Files.readAllLines(auditFile, StandardCharsets.UTF_8);
        assertThat(lines.size(), is(2));

        String content = Files.readString(auditFile, StandardCharsets.UTF_8);
        assertThat(content, containsString("test-source|com.oraclecloud.test-source.test|test|"
                                                   + DEFAULT_COMPARTMENT_ID + "|resource-3|"));
        assertThat(content, containsString("test-source|com.oraclecloud.test-source.test|test|"
                                                   + DEFAULT_COMPARTMENT_ID + "|resource-2|"));
        assertThat(content, containsString("|/get|GET|200 OK|param1|header1"));
        assertThat(content, not(containsString("ID 1")));
    }

    @Test
    void shouldIgnoreUntrustedRequestIdWithoutRequestIdContext() throws Exception {
        var captor = ArgumentCaptor.forClass(AuditEventV2.class);

        try (Http1ClientResponse response = client.get("/get")
                .header(OPC_REQUEST_ID_HEADER, "csidINJECTED!/trace?$")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
        }

        Mockito.verify(auditLogger, Mockito.timeout(2000).atLeast(2))
                .log(captor.capture());
        AuditEventV2.Data data = captor.getAllValues().get(0).getData();
        String safeRequestId = data.getRequest().getId();

        UUID.fromString(safeRequestId);
        assertThat(data.getIdentity().getConsoleSessionId(), nullValue());

        String[] requestIdHeader = data.getRequest().getHeaders().get(OPC_REQUEST_ID_HEADER.lowerCase());
        assertThat(requestIdHeader, notNullValue());
        assertThat(requestIdHeader[0], is(safeRequestId));
        assertThat(requestIdHeader[0], not(containsString("!")));
        assertThat(requestIdHeader[0], not(containsString("?")));
        assertThat(requestIdHeader[0], not(containsString("$")));
    }

    private static String auditLine(AuditEventV2 event) {
        AuditEventV2.Data data = event.getData();
        return String.join("|",
                           event.getSource(),
                           event.getEventType(),
                           data.getEventName(),
                           data.getCompartmentId(),
                           data.getResourceId(),
                           data.getRequest().getId(),
                           data.getRequest().getPath(),
                           data.getRequest().getAction(),
                           data.getResponse().getStatus(),
                           first(data.getRequest().getParameters(), "reqparam1"),
                           first(data.getRequest().getHeaders(), "reqheader1"))
                + System.lineSeparator();
    }

    private static String first(Map<String, String[]> values, String key) {
        String[] result = values.get(key);
        if (result == null || result.length == 0) {
            return "";
        }
        return result[0];
    }

    private static List<SummaryEntry> parseSummary(String summary) {
        List<SummaryEntry> entries = new ArrayList<>();
        JsonArray jsonEntries = JsonParser.create(summary).readJsonArray();
        for (int i = 0; i < jsonEntries.size(); i++) {
            JsonObject entry = jsonEntries.get(i).orElseThrow().asObject();
            String eventId = entry.stringValue("eventId").orElseThrow();
            UUID.fromString(eventId);
            assertThat(entry.value("eventGroupingId").isPresent(), is(false));
            entries.add(new SummaryEntry(eventId));
        }
        return entries;
    }

    private record SummaryEntry(String eventId) {
    }
}

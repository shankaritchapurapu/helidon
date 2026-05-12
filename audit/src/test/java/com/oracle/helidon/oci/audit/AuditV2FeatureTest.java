/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.DirectClient;
import io.helidon.webserver.testing.junit5.RoutingTest;
import io.helidon.webserver.testing.junit5.SetUpFeatures;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.oracle.pic.sherlock.collector.AuditLogger;
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.OperationSynchronousType;
import com.oracle.pic.sherlock.common.event.AuditEventV2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@RoutingTest
class AuditV2FeatureTest {

    private static final AtomicReference<AuditLogger> AUDIT_LOGGER_REF = new AtomicReference<>();
    private static final AuditLogger DELEGATING_LOGGER = event -> AUDIT_LOGGER_REF.get().log(event);

    private AuditLogger auditLogger; // per-test mock

    private final DirectClient client;

    AuditV2FeatureTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder router) {
        router.get("/get", (req, res) -> {
            Optional<AuditPayloadAppender> optional =
                    req.context().get(AuditPayloadAppender.class.getName(), AuditPayloadAppender.class);
            if (optional.isEmpty()) {
                res.status(Status.NOT_ACCEPTABLE_406).send("AuditPayloadAppender not found");
            } else {
                AuditPayloadAppender appender = optional.get();
                appender.setEventName("test", OperationSynchronousType.None);
                appender.appendToAuditRios(List.of(new AuditRIO("1", "3")));
                appender.appendToAuditRios(List.of(new AuditRIO("1", "2")));
                res.status(Status.OK_200).send();
            }
        });
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        AuditV2Config config = AuditV2Config.builder()
                .enabled(true)
                .respectSplatAuditedFlag(false)
                .eventSource("test-source")
                .requestParameterRules(List.of(RuleConfig.builder()
                        .actions("GET")
                        .resources("/get")
                        .values("reqparam1")
                        .build()))
                .requestHeaderRules(List.of(RuleConfig.builder()
                        .actions("GET")
                        .resources("/get")
                        .values("reqheader1")
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
    void test() throws Exception {
        var captor = ArgumentCaptor.forClass(AuditEventV2.class);

        Http1ClientResponse response = client.get("/get")
                .queryParam("reqparam1", "param1")
                .queryParam("reqparam2", "param2")
                .header(HeaderNames.X_FORWARDED_FOR, "localhost")
                .header(HeaderNames.create("opc-request-id"), "ID 1")
                .header(HeaderNames.create("oci-splat-audit-verify"), "true")
                .header(HeaderNames.create("reqheader1"), "header1")
                .header(HeaderNames.create("x"), "header2")
                .request();

        assertThat(response.status(), is(Status.OK_200));

        // tolerate async/different-thread completion
        Mockito.verify(auditLogger, Mockito.timeout(2000).atLeast(2))
               .log(captor.capture());
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
                .header(HeaderNames.create("opc-request-id"), "ID 1")
                .header(HeaderNames.create("reqheader1"), "header1")
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
        }

        List<String> lines = Files.readAllLines(auditFile, StandardCharsets.UTF_8);
        assertThat(lines.size(), is(2));

        String content = Files.readString(auditFile, StandardCharsets.UTF_8);
        assertThat(content, containsString("test-source|com.oraclecloud.test-source.test|test|1|3|ID 1|/get|GET|200 OK"
                                                   + "|param1|header1"));
        assertThat(content, containsString("test-source|com.oraclecloud.test-source.test|test|1|2|ID 1|/get|GET|200 OK"
                                                   + "|param1|header1"));
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
}

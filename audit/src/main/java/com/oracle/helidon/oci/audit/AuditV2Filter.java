/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.http.Header;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import com.oracle.pic.sherlock.collector.AuditConfig.Whitelist;
import com.oracle.pic.sherlock.collector.AuditConfig.Whitelist.Rule;
import com.oracle.pic.sherlock.collector.AuditLogger;
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.Whitelister;
import com.oracle.pic.sherlock.common.event.AuditEventV2;
import com.oracle.pic.sherlock.common.event.AuditEventV2.Data;
import com.oracle.pic.sherlock.common.event.AuditEventV2.Data.Identity;
import com.oracle.pic.sherlock.common.event.AuditEventV2.Data.InternalDetails;
import com.oracle.pic.sherlock.common.event.AuditEventV2.Data.Request;
import com.oracle.pic.sherlock.common.event.AuditEventV2.Data.Response;
import com.oracle.pic.sherlock.common.event.AuditEventV2.Data.StateChange;
import com.oracle.pic.sherlock.common.util.Environment;

/**
 * Audits the requests.
 *
 * The main purposes of this filter are:
 *   1. Setting an audit event attribute within the request, so it can be modified by downstream handlers.
 *   2. Notify the audit event with the AuditLogger.
 *
 * This class assumes that the request attributes are propagated to asynchronous calls,
 * so the same instance of AuditPayloadAppender can be used for that scenario.
 *
 * More information here:
 * https://confluence.oraclecorp.com/confluence/display/CLEV/Audit+v2+Schemas
 *
 * This class took Micronaut Audit as an example.
 */
class AuditV2Filter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AuditV2Filter.class.getName());
    private static final HeaderName REQUEST_OPC_PRINCIPAL_NAME = HeaderNames.create("opc-principal");
    private static final HeaderName REQUEST_ID_HEADER_NAME = HeaderNames.create("opc-request-id");
    private static final HeaderName REQUEST_SPLAT_AUDITED_NAME = HeaderNames.create("oci-splat-audited");
    private static final HeaderName EVENT_SUMMARY_HEADER_NAME = HeaderNames.create("oci-splat-audit-event-summary");
    private static final HeaderName VERIFICATION_HEADER_NAME = HeaderNames.create("oci-splat-audit-verify");
    private static final String APPENDER_ATTRIBUTE_NAME = AuditPayloadAppender.class.getName();
    static final String EVENT_TYPE = "com.oraclecloud";

    private final AuditLogger auditLogger;
    private final AuditV2Config auditConfig;
    private final Whitelister whitelister;

    AuditV2Filter(AuditV2Config config, AuditLogger auditLogger) {
        this.auditConfig = config;
        this.auditLogger = auditLogger;
        this.whitelister = whitelister();
        LOGGER.info("AuditV2Filter will be invoked on each request");
    }

    @Override
    public void filter(FilterChain filterChain, RoutingRequest request, RoutingResponse response) {
        if (skipAuditDueToSplat(request)) {
            filterChain.proceed();
        } else {
            AuditEventV2 event = auditEventV2(UUID.randomUUID().toString(), request);
            AuditPayloadAppenderImpl appender = new AuditPayloadAppenderImpl(event);
            // Allow subsequent handlers to modify the event via the appender
            request.context().register(APPENDER_ATTRIBUTE_NAME, appender);
            try {
                filterChain.proceed();
            } finally {
                addResponse(response, event.getData().getRequest(), event.getData().getResponse());
                List<AuditEventV2> events = generateEvents(appender, request, response);
                attachSummary(request, response, events);
                for (AuditEventV2 ev : events) {
                    try {
                        auditLogger.log(ev);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to log audit event " + event, e);
                    }
                }
            }
        }
    }

    private void attachSummary(RoutingRequest request, RoutingResponse response, List<AuditEventV2> events) {
        if (attachSummary(request) && !events.isEmpty()) {
            List<Map.Entry<String, Integer>> summary = new ArrayList<>();
            for (AuditEventV2 ev : events) {
                int hash = Objects.hash(ev.getData().getCompartmentId(),
                    ev.getData().getCompartmentName(),
                    ev.getData().getEventName(),
                    ev.getSource(),
                    ev.getEventType(),
                    ev.getData().getIdentity().getPrincipalId(),
                    ev.getData().getRequest().getAction(),
                    ev.getData().getIdentity().getUserAgent(),
                    ev.getData().getRequest().getId(),
                    ev.getData().getResponse().getStatus(),
                    ev.getData().getIdentity().getTenantId());
                summary.add(new AbstractMap.SimpleEntry<>(ev.getEventId(), hash));
            }
            String[] jsonList = summary.stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(e -> String.format("{\"%s\",\"%s\"}", e.getKey(), e.getValue()))
                    .toArray(String[]::new);
            response.headers().set(EVENT_SUMMARY_HEADER_NAME, jsonList);
        }
    }

    private Whitelister whitelister() {
        Whitelist whitelist = new Whitelist();
        whitelist.setResponseHeaderRules(auditConfig.responseHeaderRules()
                .stream().map(rule -> new Rule(rule.resources(), rule.actions(), rule.values()))
                .collect(Collectors.toList()));
        whitelist.setRequestHeaderRules(auditConfig.requestHeaderRules()
                .stream().map(rule -> new Rule(rule.resources(), rule.actions(), rule.values()))
                .collect(Collectors.toList()));
        whitelist.setRequestParameterRules(auditConfig.requestParameterRules()
                .stream().map(rule -> new Rule(rule.resources(), rule.actions(), rule.values()))
                .collect(Collectors.toList()));
        return new Whitelister(whitelist);
    }

    private boolean skipAuditDueToSplat(RoutingRequest request) {
        if (!auditConfig.respectSplatAuditedFlag()) {
            return false;
        }
        return request.headers().first(REQUEST_SPLAT_AUDITED_NAME).map(Boolean::parseBoolean).orElse(false);
    }


    private boolean attachSummary(RoutingRequest request) {
        return request.headers().first(VERIFICATION_HEADER_NAME).map(Boolean::parseBoolean).orElse(false);
    }

    private List<AuditEventV2> generateEvents(AuditPayloadAppenderImpl appender,
            RoutingRequest request, RoutingResponse response) {
        List<AuditEventV2> events = List.of();
        if (appender.isDoNotLog()) {
            // Downstream programmatically wants to skip
            LOGGER.fine("Event has DoNotLog set, skipping logging");
        } else {
            events = new ArrayList<>();
            List<AuditRIO> rios = appender.getAuditRios();
            if (rios.isEmpty()) {
                events.add(appender.getEvent());
            } else {
                events.addAll(convertAuditRIOs(rios.get(0).getResourceId(), appender, request, response));
            }
        }
        return events;
    }

    private List<AuditEventV2> convertAuditRIOs(String mainResourceId,
            AuditPayloadAppenderImpl appender, RoutingRequest request, RoutingResponse response) {
        // If service did not set EventGroupingId then we'll generate one to link these events
        if (appender.getEvent().getData().getEventGroupingId() == null) {
            appender.setEventGroupingId(UUID.randomUUID().toString());
        }
        List<AuditEventV2> events = new ArrayList<>();
        List<AuditRIO> auditRIOS = appender.getAuditRios();
        /*
         *  We need to clone new events for each AuditRIO, and update Data with the AuditRIO info.
         *  For performance reasons, to avoid multiple copies when it is not necessary,
         *  we will reuse some references like request, response, etc.
         */
        for (int i = 0; i < auditRIOS.size(); i++) {
            AuditRIO rio = auditRIOS.get(i);
            AuditEventV2 merged = auditEventV2FromAuditRIO(appender.getEvent(), rio);
            /*
             * The following fields only apply to the primary resource,
             * unless the resourceId is the same (for example move compartment).
             */
            if (!mainResourceId.equals(rio.getResourceId())) {
                merged.getData().setResourceName(null);
                merged.getData().setResourceVersion(null);
                merged.getData().setTagSlug(null);
                merged.getData().setAvailabilityDomain(null);
            }
            events.add(merged);
        }
        LOGGER.fine("Found multiple RIO events: " + appender.getEventId());
        return events;
    }

    private AuditEventV2 auditEventV2FromAuditRIO(AuditEventV2 eventToCopy, AuditRIO auditRio) {
        Data dataToCopy = eventToCopy.getData();
        Data data = Data.builder()
                // Reuse some references
                .identity(dataToCopy.getIdentity())
                .request(dataToCopy.getRequest())
                .response(dataToCopy.getResponse())
                .stateChange(dataToCopy.getStateChange())
                .internalDetails(dataToCopy.getInternalDetails())
                .availabilityDomain(dataToCopy.getAvailabilityDomain())
                .compartmentName(dataToCopy.getCompartmentName())
                .tagSlug(dataToCopy.getTagSlug())
                .compartmentId(auditRio.getCompartmentId())
                .resourceId(auditRio.getResourceId())
                .eventGroupingId(dataToCopy.getEventGroupingId())
                .build();
        AuditEventV2 event = AuditEventV2.builder()
                .cloudEventsVersion(eventToCopy.getCloudEventsVersion())
                .contentType(eventToCopy.getContentType())
                .data(data)
                .eventId(eventToCopy.getEventId())
                .eventTime(eventToCopy.getEventTime())
                .eventType(eventToCopy.getEventType())
                .eventTypeVersion(eventToCopy.getEventTypeVersion())
                .source(eventToCopy.getSource())
                .build();
        return event;
    }

    private AuditEventV2 auditEventV2(String eventId, RoutingRequest request) {
        Data data = data(eventId, request);
        AuditEventV2 event = AuditEventV2.builder()
                .cloudEventsVersion(AuditEventV2.CLOUD_EVENTS_VERSION)
                .contentType(AuditEventV2.CONTENT_TYPE)
                .data(data)
                .eventId(eventId)
                .eventTime(Date.from(Instant.now()))
                .eventType(EVENT_TYPE)
                .eventTypeVersion(AuditEventV2.EVENT_TYPE_VERSION)
                .source(auditConfig.eventSource())
                .build();
        return event;
    }

    private Data data(String eventId, RoutingRequest request) {
        Request req = new Request();
        Response resp = new Response();
        Identity identity = new Identity();
        Data data = Data.builder()
                .identity(identity)
                .request(req)
                .response(resp)
                .stateChange(new StateChange())
                .internalDetails(new InternalDetails())
                .availabilityDomain(Environment.AVAILABILITY_DOMAIN)
                .build();
        ServerRequestHeaders headers = request.headers();
        // Obtain IP from header or from the request itself
        headers.find(HeaderNames.X_FORWARDED_FOR).ifPresentOrElse(
                header -> identity.setIpAddress(header.get()),
                () -> {
                    if (request.remotePeer().address() instanceof InetSocketAddress isa) {
                        if (isa.getAddress() != null) {
                            identity.setIpAddress(isa.getAddress().getHostAddress());
                        }
                    }
                });
        headers.find(HeaderNames.USER_AGENT).ifPresent(header -> identity.setUserAgent(header.get()));
        req.setPath(request.requestedUri().path().path());
        req.setAction(request.prologue().method().text());
        headers.find(REQUEST_ID_HEADER_NAME).ifPresentOrElse(
                header -> {
                    String id = header.get();
                    req.setId(id);
                    if (id.toLowerCase().startsWith("csid")) {
                        int idx = id.indexOf('/');
                        if (idx > 0) {
                            identity.setConsoleSessionId(id.substring(0, idx));
                        }
                    }
                }, () -> req.setId(eventId));
        addRequestHeaders(request, req);
        addRequestParameters(request, req);
        return data;
    }

    private void addRequestHeaders(RoutingRequest request, Request req) {
        Map<String, String[]> headers = Map.of();
        if (whitelister.requestHeadersWhitelisted(req.getPath(), req.getAction())) {
            headers = new HashMap<>();
            for (Header header : request.headers()) {
                if (whitelister.requestHeaderAllowed(req.getPath(), req.getAction(), header.name())) {
                    String[] values = header.allValues().toArray(new String[0]);
                    // Even if white listed, mask sensitive headers
                    if (HeaderNames.AUTHORIZATION.lowerCase().equals(header.name())
                            || REQUEST_OPC_PRINCIPAL_NAME.lowerCase().equals(header.name())) {
                        values = new String[] {"*****"};
                    }
                    headers.put(header.name().toString(), values);
                }
            }
        }
        req.setHeaders(headers);
    }

    private void addResponse(RoutingResponse response, Request req, Response resp) {
        Map<String, String[]> headers = Map.of();
        if (whitelister.responseHeadersWhitelisted(req.getPath(), req.getAction())) {
            headers = new HashMap<>();
            for (Header header : response.headers()) {
                if (whitelister.responseHeaderAllowed(req.getPath(), req.getAction(), header.name())) {
                    String[] values = header.allValues().toArray(new String[0]);
                    headers.put(header.name(), values);
                }
            }
        }
        resp.setHeaders(headers);
        resp.setResponseTime(Date.from(Instant.now()));
        resp.setStatus(response.status().text());
    }

    private void addRequestParameters(RoutingRequest request, Request req) {
        Map<String, String[]> parameters = Map.of();
        if (whitelister.requestParametersWhitelisted(req.getPath(), req.getAction())) {
            parameters = new HashMap<>();
            Map<String, List<String>> allParams = request.query().toMap();
            for (Entry<String, List<String>> entry : allParams.entrySet()) {
                if (whitelister.requestParameterAllowed(req.getPath(), req.getAction(), entry.getKey())) {
                    parameters.put(entry.getKey(), entry.getValue().toArray(new String[0]));
                }
            }
        }
        req.setParameters(parameters);
    }
}

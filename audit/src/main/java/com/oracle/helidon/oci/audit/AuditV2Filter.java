/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.http.Header;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.json.JsonObject;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import com.oracle.helidon.oci.requestid.OciRequestId;
import com.oracle.pic.sherlock.collector.AuditConfig.Whitelist;
import com.oracle.pic.sherlock.collector.AuditConfig.Whitelist.Rule;
import com.oracle.pic.sherlock.collector.AuditLogger;
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.OperationSynchronousType;
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
    private static final String SPLAT_REQUEST_VALIDATED_CONTEXT_KEY =
            "com.oracle.helidon.oci.splat.requestValidated";
    private static final Set<String> SENSITIVE_REQUEST_HEADER_NAMES = Set.of(
            HeaderNames.AUTHORIZATION.lowerCase(),
            HeaderNames.PROXY_AUTHORIZATION.lowerCase(),
            HeaderNames.COOKIE.lowerCase(),
            REQUEST_OPC_PRINCIPAL_NAME.lowerCase(),
            "x-auth-token",
            "x-id-token",
            "x-obo-token",
            "x-security-token",
            "x-subject-token",
            "x-cross-tenancy-request");
    static final String EVENT_TYPE = "com.oraclecloud";

    private final AuditLogger auditLogger;
    private final AuditV2Config auditConfig;
    private final Whitelister whitelister;
    private final List<TrustedProxyCidr> trustedProxyCidrs;

    AuditV2Filter(AuditV2Config config, AuditLogger auditLogger) {
        this.auditConfig = config;
        this.auditLogger = auditLogger;
        this.whitelister = whitelister();
        this.trustedProxyCidrs = config.trustedProxyCidrs().stream()
                .map(TrustedProxyCidr::parse)
                .toList();
        LOGGER.info("AuditV2Filter will be invoked on each request");
    }

    @Override
    public void filter(FilterChain filterChain, RoutingRequest request, RoutingResponse response) {
        AuditEventV2 event = auditEventV2(UUID.randomUUID().toString(), request);
        AuditPayloadAppenderImpl appender = new AuditPayloadAppenderImpl(event);
        AtomicBoolean chainCompleted = new AtomicBoolean();
        AtomicBoolean responseSent = new AtomicBoolean();
        AtomicBoolean auditEmitted = new AtomicBoolean();
        // Keep the appender injectable even when emission is skipped.
        request.context().register(APPENDER_ATTRIBUTE_NAME, appender);
        if (attachSummary(request)) {
            response.beforeSend(() -> {
                if (!skipAuditDueToSplat(request)) {
                    addResponse(response, event.getData().getRequest(), event.getData().getResponse());
                    attachSummary(response, generateEvents(appender, request, response));
                }
            });
        }
        Runnable emitAudit = () -> {
            if (!chainCompleted.get() || !auditEmitted.compareAndSet(false, true) || skipAuditDueToSplat(request)) {
                return;
            }
            addResponse(response, event.getData().getRequest(), event.getData().getResponse());
            List<AuditEventV2> events = generateEvents(appender, request, response);
            for (AuditEventV2 ev : events) {
                try {
                    auditLogger.log(ev);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to log audit event " + ev.getEventId(), e);
                }
            }
        };
        response.whenSent(() -> {
            responseSent.set(true);
            emitAudit.run();
        });
        try {
            filterChain.proceed();
        } finally {
            chainCompleted.set(true);
            // A response write can fail after the entity is started but before whenSent callbacks run.
            if (responseSent.get() || response.isSent() || response.hasEntity()) {
                emitAudit.run();
            }
        }
    }

    private void attachSummary(RoutingResponse response, List<AuditEventV2> events) {
        if (events.isEmpty()) {
            return;
        }
        String json = events.stream()
                .map(ev -> new AuditEventSummary(ev.getEventId()))
                .map(AuditEventSummary::toJson)
                .collect(Collectors.joining(",", "[", "]"));
        response.headers().set(EVENT_SUMMARY_HEADER_NAME, json);
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
        boolean splatAudited = request.headers()
                .first(REQUEST_SPLAT_AUDITED_NAME)
                .map(Boolean::parseBoolean)
                .orElse(false);
        if (!splatAudited) {
            return false;
        }
        // Only server-validated SPLAT provenance makes it trustworthy.
        return request.context()
                .get(SPLAT_REQUEST_VALIDATED_CONTEXT_KEY, Boolean.class)
                .orElse(false);
    }


    private boolean attachSummary(RoutingRequest request) {
        return request.headers().first(VERIFICATION_HEADER_NAME).map(Boolean::parseBoolean).orElse(false);
    }

    private List<AuditEventV2> generateEvents(AuditPayloadAppenderImpl appender,
            RoutingRequest request, RoutingResponse response) {
        if (appender.getGeneratedEvents() != null) {
            return appender.getGeneratedEvents();
        }
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
        appender.setGeneratedEvents(events);
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
            String eventId = i == 0 ? appender.getEventId() : UUID.randomUUID().toString();
            AuditEventV2 merged = auditEventV2FromAuditRIO(appender.getEvent(), rio, eventId);
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

    private AuditEventV2 auditEventV2FromAuditRIO(AuditEventV2 eventToCopy, AuditRIO auditRio, String eventId) {
        Data dataToCopy = eventToCopy.getData();
        Data data = Data.builder()
                // Reuse some references
                .eventName(dataToCopy.getEventName())
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
                .eventId(eventId)
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
                .eventType(generateEventType(auditConfig.eventSource(),
                                             auditConfig.eventName(),
                                             OperationSynchronousType.None))
                .eventTypeVersion(AuditEventV2.EVENT_TYPE_VERSION)
                .source(auditConfig.eventSource())
                .build();
        return event;
    }

    private Data data(String eventId, RoutingRequest request) {
        Request req = new Request();
        Response resp = new Response();
        Identity identity = new Identity();
        String path = request.requestedUri().path().path();
        String action = request.prologue().method().text();
        Data data = Data.builder()
                .eventName(auditConfig.eventName())
                .compartmentId(auditConfig.compartmentId())
                .resourceId(auditConfig.resourceId())
                .resourceName(auditConfig.resourceName())
                .identity(identity)
                .request(req)
                .response(resp)
                .stateChange(new StateChange())
                .internalDetails(new InternalDetails())
                .availabilityDomain(Environment.AVAILABILITY_DOMAIN)
                .build();
        identity.setTenantId(auditConfig.tenantId());
        ServerRequestHeaders headers = request.headers();
        remotePeerAddress(request).ifPresent(remoteAddress -> {
            // X-Forwarded-For is trusted only when the direct peer is explicitly configured.
            if (isTrustedProxy(remoteAddress)) {
                forwardedForAddress(headers).ifPresentOrElse(
                        forwardedAddress -> identity.setIpAddress(forwardedAddress.getHostAddress()),
                        () -> identity.setIpAddress(remoteAddress.getHostAddress()));
            } else {
                identity.setIpAddress(remoteAddress.getHostAddress());
            }
        });
        headers.find(HeaderNames.USER_AGENT).ifPresent(header -> identity.setUserAgent(header.get()));
        req.setPath(path);
        req.setAction(action);
        String requestIdHeaderValue = request.context()
                .get(OciRequestId.class)
                .map(OciRequestId::upstreamHeaderValue)
                .orElse(eventId);
        req.setId(requestIdHeaderValue);
        addRequestHeaders(request, req, requestIdHeaderValue);
        addRequestParameters(request, req);
        return data;
    }

    private boolean isTrustedProxy(InetAddress remoteAddress) {
        return trustedProxyCidrs.stream().anyMatch(cidr -> cidr.contains(remoteAddress));
    }

    private static Optional<InetAddress> remotePeerAddress(RoutingRequest request) {
        if (request.remotePeer().address() instanceof InetSocketAddress isa) {
            return Optional.ofNullable(isa.getAddress());
        }
        return Optional.empty();
    }

    private static Optional<InetAddress> forwardedForAddress(ServerRequestHeaders headers) {
        return headers.find(HeaderNames.X_FORWARDED_FOR)
                .filter(header -> header.valueCount() == 1)
                .map(Header::get)
                .flatMap(AuditV2Filter::literalIpAddress);
    }

    private static Optional<InetAddress> literalIpAddress(String value) {
        String candidate = value.trim();
        if (candidate.contains(",") || candidate.contains("%")) {
            return Optional.empty();
        }
        try {
            if (candidate.matches("[0-9.]+") && isIpv4Literal(candidate)) {
                return Optional.of(InetAddress.ofLiteral(candidate));
            }
            if (candidate.contains(":")) {
                return Optional.of(InetAddress.ofLiteral(candidate));
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static boolean isIpv4Literal(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            try {
                if (octet.isEmpty() || Integer.parseInt(octet) > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private record TrustedProxyCidr(InetAddress network, int prefixLength) {
        private static TrustedProxyCidr parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
            }
            InetAddress network = literalIpAddress(parts[0])
                    .orElseThrow(() -> new IllegalArgumentException("Invalid trusted proxy CIDR: " + value));
            try {
                int prefixLength = Integer.parseInt(parts[1]);
                int maxPrefixLength = network.getAddress().length * Byte.SIZE;
                if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                    throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value);
                }
                return new TrustedProxyCidr(network, prefixLength);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, e);
            }
        }

        private boolean contains(InetAddress address) {
            byte[] networkBytes = network.getAddress();
            byte[] addressBytes = address.getAddress();
            if (networkBytes.length != addressBytes.length) {
                return false;
            }
            int completeBytes = prefixLength / Byte.SIZE;
            for (int i = 0; i < completeBytes; i++) {
                if (networkBytes[i] != addressBytes[i]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % Byte.SIZE;
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (Byte.SIZE - remainingBits);
            return (networkBytes[completeBytes] & mask) == (addressBytes[completeBytes] & mask);
        }
    }

    static String generateEventType(String serviceName, String eventName, OperationSynchronousType syncType) {
        if (serviceName == null || serviceName.isBlank() || eventName == null || eventName.isBlank()) {
            return EVENT_TYPE;
        }
        String suffix = "";
        if (syncType == OperationSynchronousType.AsyncBegin) {
            suffix = ".begin";
        } else if (syncType == OperationSynchronousType.AsyncEnd) {
            suffix = ".end";
        }
        return String.format(EVENT_TYPE + ".%s.%s%s", serviceName, eventName, suffix);
    }

    private record AuditEventSummary(String eventId) {
        String toJson() {
            JsonObject.Builder builder = JsonObject.builder();
            setJsonString(builder, "eventId", eventId);
            return builder.build().toString();
        }

        private static void setJsonString(JsonObject.Builder builder, String key, String value) {
            if (value == null) {
                builder.setNull(key);
            } else {
                builder.set(key, value);
            }
        }
    }

    private void addRequestHeaders(RoutingRequest request, Request req, String requestIdHeaderValue) {
        Map<String, String[]> headers = Map.of();
        if (whitelister.requestHeadersWhitelisted(req.getPath(), req.getAction())) {
            headers = new HashMap<>();
            for (Header header : request.headers()) {
                if (whitelister.requestHeaderAllowed(req.getPath(), req.getAction(), header.name())) {
                    String[] values = header.allValues().toArray(new String[0]);
                    // Even if white listed, mask sensitive headers
                    if (SENSITIVE_REQUEST_HEADER_NAMES.contains(header.headerName().lowerCase())) {
                        values = new String[] {"*****"};
                    } else if (REQUEST_ID_HEADER_NAME.lowerCase().equals(header.headerName().lowerCase())) {
                        values = new String[] {requestIdHeaderValue};
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

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.pic.identity.authentication.PrincipalSubType;
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.OperationSynchronousType;
import com.oracle.pic.sherlock.common.event.AuditEventV2;

/**
 * {@code AuditPayloadAppenderImpl} is a package-scoped payload appender designed to be
 * used to customize request and response payloads.
 *
 * Its upper interface is designed to be mutable, and this class does not provide
 * any thread-safety mechanism.
 *
 */
class AuditPayloadAppenderImpl implements AuditPayloadAppender {

    private static final String EVENT_TYPE_PATTERN = AuditV2Filter.EVENT_TYPE + ".%s.%s%s";
    private static final String EVENT_TYPE_SUFFIX_BEGIN = ".begin";
    private static final String EVENT_TYPE_SUFFIX_END = ".end";
    private final List<AuditRIO> auditRios = new ArrayList<>();
    private final AuditEventV2 event;
    private OperationSynchronousType operationSynchronousType = OperationSynchronousType.None;
    private boolean doNotLog = false;

    AuditPayloadAppenderImpl(AuditEventV2 event) {
        this.event = event;
    }

    @Override
    public void setEventName(String eventName, OperationSynchronousType operationSynchronousType) {
        /*
         * There is no javadoc or sources provided by sherlock-collector-core, so we cannot know
         * what is supposed to do each method:
         * https://artifacthub-phx.oci.oraclecorp.com/oscs-virtual/com/oracle/pic/sherlock/sherlock-collector-core/3.0.13/
         *
         * I was checking Micronaut integration to know what to do.
         */
        this.event.getData().setEventName(eventName);
        this.operationSynchronousType = operationSynchronousType;
        overrideEventType(generateEventType(
                event.getSource(),
                eventName,
                operationSynchronousType));
    }

    @Override
    public void overrideEventType(String eventType) {
        event.setEventType(eventType);
    }

    @Override
    public void overrideAction(String action) {
        event.getData().getRequest().setAction(action);
    }

    @Override
    public void overrideCompartmentId(String compartmentId) {
        event.getData().setCompartmentId(compartmentId);
    }

    @Override
    public void overrideAvailabilityDomain(String availabilityDomain) {
        event.getData().setAvailabilityDomain(availabilityDomain);
    }

    @Override
    public void setResourceName(String resourceName) {
        event.getData().setResourceName(resourceName);
    }

    @Override
    public void setResourceId(String resourceId) {
        event.getData().setResourceId(resourceId);
    }

    @Override
    public void setResourceVersion(String resourceVersion) {
        event.getData().setResourceVersion(resourceVersion);
    }

    @Override
    public void setTagSlug(byte[] tagSlug) {
        event.getData().setTagSlug(tagSlug);
    }

    @Override
    public void overridePrincipalId(String userId) {
        event.getData().getIdentity().setPrincipalId(userId);
    }

    @Override
    public void overridePrincipalName(String username) {
        event.getData().getIdentity().setPrincipalName(username);
    }

    @Override
    public void overridePrincipalTenantId(String tenantId) {
        event.getData().getIdentity().setTenantId(tenantId);
    }

    @Override
    public void overrideCredentials(String credentials) {
        event.getData().getIdentity().setCredentials(credentials);
    }

    @Override
    public void setAuthZPolicies(Object policies) {
        event.getData().getIdentity().setAuthZPolicies(policies);
    }

    @Override
    public void setUserGroups(Object userGroups) {
        event.getData().getIdentity().setUserGroups(userGroups);
    }

    @Override
    public void overrideCallerName(String serviceName) {
        event.getData().getIdentity().setCallerName(serviceName);
    }

    @Override
    public void setCallerId(String callerId) {
        event.getData().getIdentity().setCallerId(callerId);
    }

    @Override
    public void overrideRequestId(String requestId) {
        event.getData().getRequest().setId(requestId);
    }

    @Override
    public void setEventGroupingId(String eventGroupingId) {
        event.getData().setEventGroupingId(eventGroupingId);
    }

    @Override
    public void setResponseMessage(String responseMessage) {
        event.getData().getResponse().setMessage(responseMessage);
    }

    @Override
    public void appendToResponsePayload(String key, Object value) {
        Map<String, Object> respPayload = event.getData().getResponse().getPayload();
        if (respPayload == null) {
            respPayload = new HashMap<>();
            event.getData().getResponse().setPayload(respPayload);
        }
        respPayload.put(key, value);
    }

    @Override
    public void setPreviousState(Map<String, Object> previousState) {
        event.getData().getStateChange().setPrevious(previousState);
    }

    @Override
    public void setCurrentState(Map<String, Object> currentState) {
        event.getData().getStateChange().setCurrent(currentState);
    }

    @Override
    public void setAdditionalDetails(Map<String, Object> additionalDetails) {
        event.getData().setAdditionalDetails(additionalDetails);
    }

    @Override
    public void appendInternalAttribute(String key, Object value) {
        Map<String, Object> internalAttribs = event.getData().getInternalDetails().getAttributes();
        if (internalAttribs == null) {
            internalAttribs = new HashMap<>();
            event.getData().getInternalDetails().setAttributes(internalAttribs);
        }
        internalAttribs.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void appendInternalCorrelation(String key, String value) {
        Map<String, Object> internalAttribs = event.getData().getInternalDetails().getAttributes();
        Map<String, String> internalCorrelations = null;
        if (internalAttribs == null || internalAttribs.get("resource_correlations") == null) {
            internalCorrelations = new HashMap<>();
            appendInternalAttribute("resource_correlations", internalCorrelations);
        } else {
            internalCorrelations = (Map<String, String>) internalAttribs.get("resource_correlations");
        }
        internalCorrelations.put(key, value);
    }

    @Override
    public void setDoNotLog(boolean doNotLog) {
        this.doNotLog = doNotLog;
    }

    @Override
    public void overridePrincipalAuthType(String principalAuthType) {
        event.getData().getIdentity().setAuthType(principalAuthType);
    }

    @Override
    public void overridePrincipalAuthType(PrincipalSubType paramPrincipalSubType) {
        event.getData().getIdentity().setAuthType(paramPrincipalSubType.value());
    }

    @Override
    public void appendToAuditRios(Collection<AuditRIO> auditRios) {
        this.auditRios.addAll(auditRios);
    }

    @Override
    public String getEventId() {
        return event.getEventId();
    }

    List<AuditRIO> getAuditRios() {
        return auditRios;
    }

    OperationSynchronousType getOperationSynchronousType() {
        return operationSynchronousType;
    }

    boolean isDoNotLog() {
        return doNotLog;
    }

    AuditEventV2 getEvent() {
        return event;
    }

    private String generateEventType(String serviceName, String eventName, OperationSynchronousType syncType) {
        String suffix = "";
        if (syncType == OperationSynchronousType.AsyncBegin) {
            suffix = EVENT_TYPE_SUFFIX_BEGIN;
        } else if (syncType == OperationSynchronousType.AsyncEnd) {
            suffix = EVENT_TYPE_SUFFIX_END;
        }
        return String.format(EVENT_TYPE_PATTERN, serviceName, eventName, suffix);
    }
}

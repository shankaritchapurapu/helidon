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

    private final List<AuditRIO> auditRios = new ArrayList<>();
    private final AuditEventV2 event;
    private OperationSynchronousType operationSynchronousType = OperationSynchronousType.None;
    private List<AuditEventV2> generatedEvents;
    private boolean doNotLog = false;

    AuditPayloadAppenderImpl(AuditEventV2 event) {
        this.event = event;
    }

    @Override
    public void setEventName(String eventName, OperationSynchronousType operationSynchronousType) {
        invalidateGeneratedEvents();
        /*
         * There is no javadoc or sources provided by sherlock-collector-core, so we cannot know
         * what is supposed to do each method:
         * https://artifacthub-phx.oci.oraclecorp.com/oscs-virtual/com/oracle/pic/sherlock/sherlock-collector-core/3.0.13/
         *
         * I was checking Micronaut integration to know what to do.
         */
        this.event.getData().setEventName(eventName);
        this.operationSynchronousType = operationSynchronousType;
        overrideEventType(AuditV2Filter.generateEventType(
                event.getSource(),
                eventName,
                operationSynchronousType));
    }

    @Override
    public void overrideEventType(String eventType) {
        invalidateGeneratedEvents();
        event.setEventType(eventType);
    }

    @Override
    public void overrideAction(String action) {
        invalidateGeneratedEvents();
        event.getData().getRequest().setAction(action);
    }

    @Override
    public void overrideCompartmentId(String compartmentId) {
        invalidateGeneratedEvents();
        event.getData().setCompartmentId(compartmentId);
    }

    @Override
    public void overrideAvailabilityDomain(String availabilityDomain) {
        invalidateGeneratedEvents();
        event.getData().setAvailabilityDomain(availabilityDomain);
    }

    @Override
    public void setResourceName(String resourceName) {
        invalidateGeneratedEvents();
        event.getData().setResourceName(resourceName);
    }

    @Override
    public void setResourceId(String resourceId) {
        invalidateGeneratedEvents();
        event.getData().setResourceId(resourceId);
    }

    @Override
    public void setResourceVersion(String resourceVersion) {
        invalidateGeneratedEvents();
        event.getData().setResourceVersion(resourceVersion);
    }

    @Override
    public void setTagSlug(byte[] tagSlug) {
        invalidateGeneratedEvents();
        event.getData().setTagSlug(tagSlug);
    }

    @Override
    public void overridePrincipalId(String userId) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setPrincipalId(userId);
    }

    @Override
    public void overridePrincipalName(String username) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setPrincipalName(username);
    }

    @Override
    public void overridePrincipalTenantId(String tenantId) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setTenantId(tenantId);
    }

    @Override
    public void overrideCredentials(String credentials) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setCredentials(credentials);
    }

    @Override
    public void setAuthZPolicies(Object policies) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setAuthZPolicies(policies);
    }

    @Override
    public void setUserGroups(Object userGroups) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setUserGroups(userGroups);
    }

    @Override
    public void overrideCallerName(String serviceName) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setCallerName(serviceName);
    }

    @Override
    public void setCallerId(String callerId) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setCallerId(callerId);
    }

    @Override
    public void overrideRequestId(String requestId) {
        invalidateGeneratedEvents();
        event.getData().getRequest().setId(requestId);
    }

    @Override
    public void setEventGroupingId(String eventGroupingId) {
        invalidateGeneratedEvents();
        event.getData().setEventGroupingId(eventGroupingId);
    }

    @Override
    public void setResponseMessage(String responseMessage) {
        invalidateGeneratedEvents();
        event.getData().getResponse().setMessage(responseMessage);
    }

    @Override
    public void appendToResponsePayload(String key, Object value) {
        invalidateGeneratedEvents();
        Map<String, Object> respPayload = event.getData().getResponse().getPayload();
        if (respPayload == null) {
            respPayload = new HashMap<>();
            event.getData().getResponse().setPayload(respPayload);
        }
        respPayload.put(key, value);
    }

    @Override
    public void setPreviousState(Map<String, Object> previousState) {
        invalidateGeneratedEvents();
        event.getData().getStateChange().setPrevious(previousState);
    }

    @Override
    public void setCurrentState(Map<String, Object> currentState) {
        invalidateGeneratedEvents();
        event.getData().getStateChange().setCurrent(currentState);
    }

    @Override
    public void setAdditionalDetails(Map<String, Object> additionalDetails) {
        invalidateGeneratedEvents();
        event.getData().setAdditionalDetails(additionalDetails);
    }

    @Override
    public void appendInternalAttribute(String key, Object value) {
        invalidateGeneratedEvents();
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
        invalidateGeneratedEvents();
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
        invalidateGeneratedEvents();
        this.doNotLog = doNotLog;
    }

    @Override
    public void overridePrincipalAuthType(String principalAuthType) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setAuthType(principalAuthType);
    }

    @Override
    public void overridePrincipalAuthType(PrincipalSubType paramPrincipalSubType) {
        invalidateGeneratedEvents();
        event.getData().getIdentity().setAuthType(paramPrincipalSubType.value());
    }

    @Override
    public void appendToAuditRios(Collection<AuditRIO> auditRios) {
        invalidateGeneratedEvents();
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

    List<AuditEventV2> getGeneratedEvents() {
        return generatedEvents;
    }

    void setGeneratedEvents(List<AuditEventV2> generatedEvents) {
        this.generatedEvents = generatedEvents;
    }

    private void invalidateGeneratedEvents() {
        generatedEvents = null;
    }
}

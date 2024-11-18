/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class JavaxContainerResponseContext implements ContainerResponseContext {

    private final jakarta.ws.rs.container.ContainerResponseContext delegate;

    public JavaxContainerResponseContext(jakarta.ws.rs.container.ContainerResponseContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Override
    public void setStatus(int code) {
        delegate.setStatus(code);
    }

    @Override
    public Response.StatusType getStatusInfo() {
        return new JavaxStatusType(delegate.getStatusInfo());
    }

    @Override
    public void setStatusInfo(Response.StatusType statusInfo) {
        delegate.setStatusInfo(new JakartaStatusType(statusInfo));
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return new JavaxMultivaluedMap<>(delegate.getHeaders());
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return new JavaxMultivaluedMap<>(delegate.getStringHeaders());
    }

    @Override
    public String getHeaderString(String name) {
        return delegate.getHeaderString(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return delegate.getAllowedMethods();
    }

    @Override
    public Date getDate() {
        return delegate.getDate();
    }

    @Override
    public Locale getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public int getLength() {
        return delegate.getLength();
    }

    @Override
    public MediaType getMediaType() {
        return new JavaxMediaType(delegate.getMediaType());
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return delegate.getCookies()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> new JavaxNewCookie(v.getValue())));
    }

    @Override
    public EntityTag getEntityTag() {
        return JaxRsShim.toJavax(delegate.getEntityTag());
    }

    @Override
    public Date getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    public URI getLocation() {
        return delegate.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return delegate.getLinks()
                .stream()
                .map(JavaxLink::new)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasLink(String relation) {
        return delegate.hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return new JavaxLink(delegate.getLink(relation));
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return new JavaxLink.Builder(delegate.getLinkBuilder(relation));
    }

    @Override
    public boolean hasEntity() {
        return delegate.hasEntity();
    }

    @Override
    public Object getEntity() {
        return delegate.getEntity();
    }

    @Override
    public Class<?> getEntityClass() {
        return delegate.getEntityClass();
    }

    @Override
    public Type getEntityType() {
        return delegate.getEntityType();
    }

    @Override
    public void setEntity(Object entity) {
        delegate.setEntity(entity);
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        delegate.setEntity(entity, annotations, new JakartaMediaType(mediaType));
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return delegate.getEntityAnnotations();
    }

    @Override
    public OutputStream getEntityStream() {
        return delegate.getEntityStream();
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        delegate.setEntityStream(outputStream);
    }
}

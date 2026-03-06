/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * Simple implementation of {@link javax.ws.rs.container.ContainerResponseContext} for
 * filter processing.
 */
public class HelidonContainerResponseContext implements ContainerResponseContext {

    private int status;
    private Object entity;
    private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private final MultivaluedMap<String, String> stringHeaders = new MultivaluedHashMap<>();
    private MediaType mediaType;

    /**
     * Constructor from status and entity.
     *
     * @param status response status
     * @param entity response entity
     */
    public HelidonContainerResponseContext(int status, Object entity) {
        this.status = status;
        this.entity = entity;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int code) {
        this.status = code;
    }

    @Override
    public Response.StatusType getStatusInfo() {
        Response.Status statusEnum = Response.Status.fromStatusCode(status);
        if (statusEnum != null) {
            return statusEnum;
        }
        return new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return status;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(status);
            }

            @Override
            public String getReasonPhrase() {
                return "";
            }
        };
    }

    @Override
    public void setStatusInfo(Response.StatusType statusInfo) {
        if (statusInfo == null) {
            return;
        }
        this.status = statusInfo.getStatusCode();
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return stringHeaders;
    }

    @Override
    public String getHeaderString(String name) {
        List<String> values = stringHeaders.get(name);
        if (values == null || values.isEmpty()) {
            List<Object> rawValues = headers.get(name);
            if (rawValues == null || rawValues.isEmpty()) {
                return null;
            }
            List<String> mapped = new ArrayList<>();
            for (Object value : rawValues) {
                if (value != null) {
                    mapped.add(value.toString());
                }
            }
            return mapped.isEmpty() ? null : String.join(",", mapped);
        }
        return String.join(",", values);
    }

    @Override
    public Set<String> getAllowedMethods() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Locale getLanguage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EntityTag getEntityTag() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getLastModified() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI getLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Link> getLinks() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasLink(String relation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Link getLink(String relation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasEntity() {
        return entity != null;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public Class<?> getEntityClass() {
        return entity != null ? entity.getClass() : null;
    }

    @Override
    public Type getEntityType() {
        return getEntityClass();
    }

    @Override
    public void setEntity(Object entity) {
        this.entity = entity;
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        this.entity = entity;
        this.mediaType = mediaType;
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputStream getEntityStream() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}



/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

class JavaxClientRequestContext implements javax.ws.rs.client.ClientRequestContext {
    private final jakarta.ws.rs.client.ClientRequestContext delegate;

    public JavaxClientRequestContext(jakarta.ws.rs.client.ClientRequestContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        delegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(String name) {
        delegate.removeProperty(name);
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public void setUri(URI uri) {
        delegate.setUri(uri);
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public void setMethod(String method) {
        delegate.setMethod(method);
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
    public Date getDate() {
        return delegate.getDate();
    }

    @Override
    public Locale getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public MediaType getMediaType() {
        return new JavaxMediaType(delegate.getMediaType());
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return delegate.getAcceptableMediaTypes().stream()
                .map(JavaxMediaType::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return delegate.getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return delegate.getCookies()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new JavaxCookie(e.getValue())));
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
    public void setEntity(Object entity) {
        delegate.setEntity(entity);
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

    @Override
    public Client getClient() {
        return new JavaxClient(delegate.getClient());
    }

    @Override
    public Configuration getConfiguration() {
        return new JavaxConfiguration(delegate.getConfiguration());
    }

    @Override
    public void abortWith(Response response) {
        delegate.abortWith(new JakartaResponse(response));
    }
}

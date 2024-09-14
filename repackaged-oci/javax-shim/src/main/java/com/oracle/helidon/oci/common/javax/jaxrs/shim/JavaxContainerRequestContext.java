/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

public class JavaxContainerRequestContext implements ContainerRequestContext {
    private final jakarta.ws.rs.container.ContainerRequestContext delegate;

    public JavaxContainerRequestContext(jakarta.ws.rs.container.ContainerRequestContext delegate) {
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
    public UriInfo getUriInfo() {
        return new JavaxUriInfo(delegate.getUriInfo());
    }

    @Override
    public void setRequestUri(URI requestUri) {
        delegate.setRequestUri(requestUri);
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {
        delegate.setRequestUri(baseUri, requestUri);
    }

    @Override
    public Request getRequest() {
        return new JavaxRequest(delegate.getRequest());
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
    public MultivaluedMap<String, String> getHeaders() {
        return new JavaxMultivaluedMap<>(delegate.getHeaders());
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
    public int getLength() {
        return delegate.getLength();
    }

    @Override
    public MediaType getMediaType() {
        return new JavaxMediaType(delegate.getMediaType());
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return delegate.getAcceptableMediaTypes().stream()
                .map(JavaxMediaType::new)
                .map(MediaType.class::cast)
                .toList();
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
    public InputStream getEntityStream() {
        return delegate.getEntityStream();
    }

    @Override
    public void setEntityStream(InputStream input) {
        delegate.setEntityStream(input);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return new JavaxSecurityContext(delegate.getSecurityContext());
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        delegate.setSecurityContext(new JakartaSecurityContext(context));
    }

    @Override
    public void abortWith(Response response) {
        delegate.abortWith(new JakartaResponse(response));
    }
}

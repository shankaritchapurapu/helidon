/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class JavaxUriInfo implements UriInfo {
    private final jakarta.ws.rs.core.UriInfo delegate;

    public JavaxUriInfo(jakarta.ws.rs.core.UriInfo delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public String getPath(boolean decode) {
        return delegate.getPath(decode);
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return delegate.getPathSegments().stream()
                .map(JavaxPathSegment::new)
                .map(PathSegment.class::cast)
                .toList();
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        return delegate.getPathSegments(decode).stream()
                .map(JavaxPathSegment::new)
                .map(PathSegment.class::cast)
                .toList();
    }

    @Override
    public URI getRequestUri() {
        return delegate.getRequestUri();
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return new JavaxUriBuilder(delegate.getRequestUriBuilder());
    }

    @Override
    public URI getAbsolutePath() {
        return delegate.getAbsolutePath();
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return new JavaxUriBuilder(delegate.getAbsolutePathBuilder());
    }

    @Override
    public URI getBaseUri() {
        return delegate.getBaseUri();
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return new JavaxUriBuilder(delegate.getBaseUriBuilder());
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return new JavaxMultivaluedMap<>(delegate.getPathParameters());
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return new JavaxMultivaluedMap<>(delegate.getPathParameters(decode));
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return new JavaxMultivaluedMap<>(delegate.getQueryParameters());
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return new JavaxMultivaluedMap<>(delegate.getQueryParameters(decode));
    }

    @Override
    public List<String> getMatchedURIs() {
        return delegate.getMatchedURIs();
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        return delegate.getMatchedURIs(decode);
    }

    @Override
    public List<Object> getMatchedResources() {
        return delegate.getMatchedResources();
    }

    @Override
    public URI resolve(URI uri) {
        return delegate.resolve(uri);
    }

    @Override
    public URI relativize(URI uri) {
        return delegate.relativize(uri);
    }
}

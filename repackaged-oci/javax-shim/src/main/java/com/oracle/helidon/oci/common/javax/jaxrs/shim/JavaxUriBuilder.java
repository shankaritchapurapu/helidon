/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

public class JavaxUriBuilder extends UriBuilder implements Cloneable {

    private final jakarta.ws.rs.core.UriBuilder delegate;

    public JavaxUriBuilder(jakarta.ws.rs.core.UriBuilder delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public UriBuilder clone() {
        return new JavaxUriBuilder(delegate.clone());
    }

    @Override
    public UriBuilder uri(URI uri) {
        return new JavaxUriBuilder(delegate.uri(uri));
    }

    @Override
    public UriBuilder uri(String uriTemplate) {
        return new JavaxUriBuilder(delegate.uri(uriTemplate));
    }

    @Override
    public UriBuilder scheme(String scheme) {
        return new JavaxUriBuilder(delegate.scheme(scheme));
    }

    @Override
    public UriBuilder schemeSpecificPart(String ssp) {
        return new JavaxUriBuilder(delegate.schemeSpecificPart(ssp));
    }

    @Override
    public UriBuilder userInfo(String ui) {
        return new JavaxUriBuilder(delegate.userInfo(ui));
    }

    @Override
    public UriBuilder host(String host) {
        return new JavaxUriBuilder(delegate.host(host));
    }

    @Override
    public UriBuilder port(int port) {
        return new JavaxUriBuilder(delegate.port(port));
    }

    @Override
    public UriBuilder replacePath(String path) {
        return new JavaxUriBuilder(delegate.replacePath(path));
    }

    @Override
    public UriBuilder path(String path) {
        return new JavaxUriBuilder(delegate.path(path));
    }

    @Override
    public UriBuilder path(Class resource) {
        return new JavaxUriBuilder(delegate.path(resource));
    }

    @Override
    public UriBuilder path(Class resource, String method) {
        return new JavaxUriBuilder(delegate.path(resource, method));
    }

    @Override
    public UriBuilder path(Method method) {
        return new JavaxUriBuilder(delegate.path(method));
    }

    @Override
    public UriBuilder segment(String... segments) {
        return new JavaxUriBuilder(delegate.segment(segments));
    }

    @Override
    public UriBuilder replaceMatrix(String matrix) {
        return new JavaxUriBuilder(delegate.replaceMatrix(matrix));
    }

    @Override
    public UriBuilder matrixParam(String name, Object... values) {
        return new JavaxUriBuilder(delegate.matrixParam(name, values));
    }

    @Override
    public UriBuilder replaceMatrixParam(String name, Object... values) {
        return new JavaxUriBuilder(delegate.replaceMatrixParam(name, values));
    }

    @Override
    public UriBuilder replaceQuery(String query) {
        return new JavaxUriBuilder(delegate.replaceQuery(query));
    }

    @Override
    public UriBuilder queryParam(String name, Object... values) {
        return new JavaxUriBuilder(delegate.queryParam(name, values));
    }

    @Override
    public UriBuilder replaceQueryParam(String name, Object... values) {
        return new JavaxUriBuilder(delegate.replaceMatrixParam(name, values));
    }

    @Override
    public UriBuilder fragment(String fragment) {
        return new JavaxUriBuilder(delegate.fragment(fragment));
    }

    @Override
    public UriBuilder resolveTemplate(String name, Object value) {
        return new JavaxUriBuilder(delegate.resolveTemplate(name, value));
    }

    @Override
    public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return new JavaxUriBuilder(delegate.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public UriBuilder resolveTemplateFromEncoded(String name, Object value) {
        return new JavaxUriBuilder(delegate.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public UriBuilder resolveTemplates(Map<String, Object> templateValues) {
        return new JavaxUriBuilder(delegate.resolveTemplates(templateValues));
    }

    @Override
    public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
            throws IllegalArgumentException {
        return new JavaxUriBuilder(delegate.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return new JavaxUriBuilder(delegate.resolveTemplatesFromEncoded(templateValues));
    }

    @Override
    public URI buildFromMap(Map<String, ?> values) {
        return delegate.buildFromMap(values);
    }

    @Override
    public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath)
            throws IllegalArgumentException, UriBuilderException {
        return delegate.buildFromMap(values, encodeSlashInPath);
    }

    @Override
    public URI buildFromEncodedMap(Map<String, ?> values) throws IllegalArgumentException, UriBuilderException {
        return delegate.buildFromEncodedMap(values);
    }

    @Override
    public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
        return delegate.build(values);
    }

    @Override
    public URI build(Object[] values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
        return delegate.build(values, encodeSlashInPath);
    }

    @Override
    public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
        return delegate.buildFromEncoded(values);
    }

    @Override
    public String toTemplate() {
        return delegate.toTemplate();
    }
}

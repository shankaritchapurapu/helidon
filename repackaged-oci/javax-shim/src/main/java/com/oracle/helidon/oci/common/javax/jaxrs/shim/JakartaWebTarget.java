/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

public class JakartaWebTarget implements WebTarget {

    private final javax.ws.rs.client.WebTarget delegate;

    public JakartaWebTarget(javax.ws.rs.client.WebTarget delegate) {
        this.delegate = delegate;
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return new JakartaUriBuilder(delegate.getUriBuilder());
    }

    @Override
    public WebTarget path(String path) {
        return new JakartaWebTarget(delegate.path(path));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        return new JakartaWebTarget(delegate.resolveTemplate(name, value));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return new JakartaWebTarget(delegate.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        return new JakartaWebTarget(delegate.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        return new JakartaWebTarget(delegate.resolveTemplates(templateValues));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        return new JakartaWebTarget(delegate.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return new JakartaWebTarget(delegate.resolveTemplatesFromEncoded(templateValues));
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        return new JakartaWebTarget(delegate.matrixParam(name, values));
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        return new JakartaWebTarget(delegate.queryParam(name, values));
    }

    @Override
    public Invocation.Builder request() {
        return new JakartaInvocation.Builder(delegate.request());
    }

    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        return new JakartaInvocation.Builder(delegate.request(acceptedResponseTypes));
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        JavaxMediaType[] javaxMediaTypes =
                Arrays.stream(acceptedResponseTypes).map(JavaxMediaType::new)
                        .toArray(JavaxMediaType[]::new);
        return new JakartaInvocation.Builder(delegate.request(javaxMediaTypes));
    }

    @Override
    public Configuration getConfiguration() {
        return new JakartaConfiguration(delegate.getConfiguration());
    }

    @Override
    public WebTarget property(String name, Object value) {
        return new JakartaWebTarget(delegate.property(name, value));
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        return new JakartaWebTarget(delegate.register(componentClass));
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        return new JakartaWebTarget(delegate.register(componentClass, priority));
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        return new JakartaWebTarget(delegate.register(componentClass, contracts));
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return new JakartaWebTarget(delegate.register(componentClass, contracts));
    }

    @Override
    public WebTarget register(Object component) {
        return new JakartaWebTarget(delegate.register(component));
    }

    @Override
    public WebTarget register(Object component, int priority) {
        return new JakartaWebTarget(delegate.register(component, priority));
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        return new JakartaWebTarget(delegate.register(component, contracts));
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        return new JakartaWebTarget(delegate.register(component, contracts));
    }
}

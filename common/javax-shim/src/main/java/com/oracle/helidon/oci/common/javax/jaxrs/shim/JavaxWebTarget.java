/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class JavaxWebTarget implements WebTarget {

    private final jakarta.ws.rs.client.WebTarget delegate;

    public JavaxWebTarget(jakarta.ws.rs.client.WebTarget delegate) {
        this.delegate = delegate;
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return new JavaxUriBuilder(delegate.getUriBuilder());
    }

    @Override
    public WebTarget path(String path) {
        return new JavaxWebTarget(delegate.path(path));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        return new JavaxWebTarget(delegate.resolveTemplate(name, value));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return new JavaxWebTarget(delegate.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        return new JavaxWebTarget(delegate.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        return new JavaxWebTarget(delegate.resolveTemplates(templateValues));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        return new JavaxWebTarget(delegate.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return new JavaxWebTarget(delegate.resolveTemplatesFromEncoded(templateValues));
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        return new JavaxWebTarget(delegate.matrixParam(name, values));
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        return new JavaxWebTarget(delegate.queryParam(name, values));
    }

    @Override
    public Invocation.Builder request() {
        return new JavaxInvocation.Builder(delegate.request());
    }

    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        return new JavaxInvocation.Builder(delegate.request(acceptedResponseTypes));
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        JakartaMediaType[] jakartaMediaTypes =
                Arrays.stream(acceptedResponseTypes).map(JakartaMediaType::new)
                        .toArray(JakartaMediaType[]::new);
        return new JavaxInvocation.Builder(delegate.request(jakartaMediaTypes));
    }

    @Override
    public Configuration getConfiguration() {
        return new JavaxConfiguration(delegate.getConfiguration());
    }

    @Override
    public WebTarget property(String name, Object value) {
        return new JavaxWebTarget(delegate.property(name, value));
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        return new JavaxWebTarget(delegate.register(componentClass));
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        return new JavaxWebTarget(delegate.register(componentClass, priority));
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        return new JavaxWebTarget(delegate.register(componentClass, contracts));
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return new JavaxWebTarget(delegate.register(componentClass, contracts));
    }

    @Override
    public WebTarget register(Object component) {
        return new JavaxWebTarget(delegate.register(component));
    }

    @Override
    public WebTarget register(Object component, int priority) {
        return new JavaxWebTarget(delegate.register(component, priority));
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        return new JavaxWebTarget(delegate.register(component, contracts));
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        return new JavaxWebTarget(delegate.register(component, contracts));
    }
}

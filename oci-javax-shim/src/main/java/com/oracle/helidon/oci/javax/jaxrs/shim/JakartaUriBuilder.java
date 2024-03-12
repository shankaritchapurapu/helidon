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

package com.oracle.helidon.oci.javax.jaxrs.shim;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

@SuppressWarnings("CPD-START")
public class JakartaUriBuilder extends UriBuilder implements Cloneable {

    private final javax.ws.rs.core.UriBuilder delegate;

    public JakartaUriBuilder(javax.ws.rs.core.UriBuilder delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public UriBuilder clone() {
        return new JakartaUriBuilder(delegate.clone());
    }

    @Override
    public UriBuilder uri(URI uri) {
        return new JakartaUriBuilder(delegate.uri(uri));
    }

    @Override
    public UriBuilder uri(String uriTemplate) {
        return new JakartaUriBuilder(delegate.uri(uriTemplate));
    }

    @Override
    public UriBuilder scheme(String scheme) {
        return new JakartaUriBuilder(delegate.scheme(scheme));
    }

    @Override
    public UriBuilder schemeSpecificPart(String ssp) {
        return new JakartaUriBuilder(delegate.schemeSpecificPart(ssp));
    }

    @Override
    public UriBuilder userInfo(String ui) {
        return new JakartaUriBuilder(delegate.userInfo(ui));
    }

    @Override
    public UriBuilder host(String host) {
        return new JakartaUriBuilder(delegate.host(host));
    }

    @Override
    public UriBuilder port(int port) {
        return new JakartaUriBuilder(delegate.port(port));
    }

    @Override
    public UriBuilder replacePath(String path) {
        return new JakartaUriBuilder(delegate.replacePath(path));
    }

    @Override
    public UriBuilder path(String path) {
        return new JakartaUriBuilder(delegate.path(path));
    }

    @Override
    public UriBuilder path(Class resource) {
        return new JakartaUriBuilder(delegate.path(resource));
    }

    @Override
    public UriBuilder path(Class resource, String method) {
        return new JakartaUriBuilder(delegate.path(resource, method));
    }

    @Override
    public UriBuilder path(Method method) {
        return new JakartaUriBuilder(delegate.path(method));
    }

    @Override
    public UriBuilder segment(String... segments) {
        return new JakartaUriBuilder(delegate.segment(segments));
    }

    @Override
    public UriBuilder replaceMatrix(String matrix) {
        return new JakartaUriBuilder(delegate.replaceMatrix(matrix));
    }

    @Override
    public UriBuilder matrixParam(String name, Object... values) {
        return new JakartaUriBuilder(delegate.matrixParam(name, values));
    }

    @Override
    public UriBuilder replaceMatrixParam(String name, Object... values) {
        return new JakartaUriBuilder(delegate.replaceMatrixParam(name, values));
    }

    @Override
    public UriBuilder replaceQuery(String query) {
        return new JakartaUriBuilder(delegate.replaceQuery(query));
    }

    @Override
    public UriBuilder queryParam(String name, Object... values) {
        return new JakartaUriBuilder(delegate.queryParam(name, values));
    }

    @Override
    public UriBuilder replaceQueryParam(String name, Object... values) {
        return new JakartaUriBuilder(delegate.replaceMatrixParam(name, values));
    }

    @Override
    public UriBuilder fragment(String fragment) {
        return new JakartaUriBuilder(delegate.fragment(fragment));
    }

    @Override
    public UriBuilder resolveTemplate(String name, Object value) {
        return new JakartaUriBuilder(delegate.resolveTemplate(name, value));
    }

    @Override
    public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return new JakartaUriBuilder(delegate.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public UriBuilder resolveTemplateFromEncoded(String name, Object value) {
        return new JakartaUriBuilder(delegate.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public UriBuilder resolveTemplates(Map<String, Object> templateValues) {
        return new JakartaUriBuilder(delegate.resolveTemplates(templateValues));
    }

    @Override
    public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
            throws IllegalArgumentException {
        return new JakartaUriBuilder(delegate.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return new JakartaUriBuilder(delegate.resolveTemplatesFromEncoded(templateValues));
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

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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;

class JakartaClientRequestContext implements jakarta.ws.rs.client.ClientRequestContext {
    private final javax.ws.rs.client.ClientRequestContext delegate;

    public JakartaClientRequestContext(javax.ws.rs.client.ClientRequestContext delegate) {
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
        return new JakartaMultivaluedMap<>(delegate.getHeaders());
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return new JakartaMultivaluedMap<>(delegate.getStringHeaders());
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
        return new JakartaMediaType(delegate.getMediaType());
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return delegate.getAcceptableMediaTypes().stream()
                .map(JakartaMediaType::new)
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
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new JakartaCookie(e.getValue())));
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
        delegate.setEntity(entity, annotations, new JavaxMediaType(mediaType));
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
        return new JakartaClient(delegate.getClient());
    }

    @Override
    public Configuration getConfiguration() {
        return new JakartaConfiguration(delegate.getConfiguration());
    }

    @Override
    public void abortWith(Response response) {
        delegate.abortWith(new JavaxResponse(response));
    }
}

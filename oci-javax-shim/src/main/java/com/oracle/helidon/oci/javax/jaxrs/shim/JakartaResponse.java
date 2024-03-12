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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

public class JakartaResponse extends Response {
    private final javax.ws.rs.core.Response delegate;

    public JakartaResponse(javax.ws.rs.core.Response delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Override
    public StatusType getStatusInfo() {
        return new JakartaStatusType(delegate.getStatusInfo());
    }

    @Override
    public Object getEntity() {
        return delegate.getEntity();
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return delegate.readEntity(entityType);
    }

    @Override
    public <T> T readEntity(jakarta.ws.rs.core.GenericType<T> entityType) {
        return delegate.readEntity(new JavaxGenericType<T>(entityType));
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return delegate.readEntity(entityType, annotations);
    }

    @Override
    public <T> T readEntity(jakarta.ws.rs.core.GenericType<T> entityType, Annotation[] annotations) {
        return delegate.readEntity(new JavaxGenericType<T>(entityType), annotations);
    }

    @Override
    public boolean hasEntity() {
        return delegate.hasEntity();
    }

    @Override
    public boolean bufferEntity() {
        return delegate.bufferEntity();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public jakarta.ws.rs.core.MediaType getMediaType() {
        return new JakartaMediaType(delegate.getMediaType());
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
    public Set<String> getAllowedMethods() {
        return delegate.getAllowedMethods();
    }

    @Override
    public Map<String, jakarta.ws.rs.core.NewCookie> getCookies() {
        return delegate.getCookies().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new JakartaNewCookie(e.getValue())));
    }

    @Override
    public jakarta.ws.rs.core.EntityTag getEntityTag() {
        return JaxRsShim.toJakarta(delegate.getEntityTag());
    }

    @Override
    public Date getDate() {
        return delegate.getDate();
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
    public Set<jakarta.ws.rs.core.Link> getLinks() {
        return delegate.getLinks().stream().map(JakartaLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean hasLink(String relation) {
        return delegate.hasLink(relation);
    }

    @Override
    public jakarta.ws.rs.core.Link getLink(String relation) {
        return new JakartaLink(delegate.getLink(relation));
    }

    @Override
    public jakarta.ws.rs.core.Link.Builder getLinkBuilder(String relation) {
        return new JakartaLink.Builder(delegate.getLinkBuilder(relation));
    }

    @Override
    public jakarta.ws.rs.core.MultivaluedMap<String, Object> getMetadata() {
        return new JakartaMultivaluedMap<>(delegate.getMetadata());
    }

    @Override
    public jakarta.ws.rs.core.MultivaluedMap<String, String> getStringHeaders() {
        return new JakartaMultivaluedMap<>(delegate.getStringHeaders());
    }

    @Override
    public String getHeaderString(String name) {
        return delegate.getHeaderString(name);
    }
}

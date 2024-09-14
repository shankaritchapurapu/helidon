/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

public class JavaxResponse extends Response {

    private final jakarta.ws.rs.core.Response delegate;

    public JavaxResponse(jakarta.ws.rs.core.Response delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Override
    public StatusType getStatusInfo() {
        return new JavaxStatusType(delegate.getStatusInfo());
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
    public <T> T readEntity(GenericType<T> entityType) {
        return delegate.readEntity(new JakartaGenericType<T>(entityType));
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return delegate.readEntity(entityType, annotations);
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return delegate.readEntity(new JakartaGenericType<T>(entityType), annotations);
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
    public MediaType getMediaType() {
        return new JavaxMediaType(delegate.getMediaType());
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
    public Map<String, NewCookie> getCookies() {
        return delegate.getCookies().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new JavaxNewCookie(e.getValue())));
    }

    @Override
    public EntityTag getEntityTag() {
        return JaxRsShim.toJavax(delegate.getEntityTag());
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
    public Set<Link> getLinks() {
        return delegate.getLinks().stream().map(JavaxLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean hasLink(String relation) {
        return delegate.hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return new JavaxLink(delegate.getLink(relation));
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return new JavaxLink.Builder(delegate.getLinkBuilder(relation));
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return new JavaxMultivaluedMap<>(delegate.getMetadata());
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return new JavaxMultivaluedMap<>(delegate.getStringHeaders());
    }

    @Override
    public String getHeaderString(String name) {
        return delegate.getHeaderString(name);
    }

    public static class JavaxResponseBuilder extends ResponseBuilder {
        private final jakarta.ws.rs.core.Response.ResponseBuilder delegate;

        public JavaxResponseBuilder(jakarta.ws.rs.core.Response.ResponseBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Response build() {
            return new JavaxResponse(delegate.build());
        }

        @Override
        public ResponseBuilder clone() {
            return new JavaxResponseBuilder(delegate.clone());
        }

        @Override
        public ResponseBuilder status(int status) {
            delegate.status(status);
            return this;
        }

        @Override
        public ResponseBuilder status(int status, String reasonPhrase) {
            delegate.status(status, reasonPhrase);
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity) {
            delegate.entity(entity);
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity, Annotation[] annotations) {
            delegate.entity(entity, annotations);
            return this;
        }

        @Override
        public ResponseBuilder allow(String... methods) {
            delegate.allow(methods);
            return this;
        }

        @Override
        public ResponseBuilder allow(Set<String> methods) {
            delegate.allow(methods);
            return this;
        }

        @Override
        public ResponseBuilder cacheControl(CacheControl cacheControl) {
            delegate.cacheControl(new JakartaCacheControl(cacheControl));
            return this;
        }

        @Override
        public ResponseBuilder encoding(String encoding) {
            delegate.encoding(encoding);
            return this;
        }

        @Override
        public ResponseBuilder header(String name, Object value) {
            delegate.header(name, value);
            return this;
        }

        @Override
        public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
            delegate.replaceAll(new JakartaMultivaluedMap<>(headers));
            return this;
        }

        @Override
        public ResponseBuilder language(String language) {
            delegate.language(language);
            return this;
        }

        @Override
        public ResponseBuilder language(Locale language) {
            delegate.language(language);
            return this;
        }

        @Override
        public ResponseBuilder type(MediaType type) {
            delegate.type(new JakartaMediaType(type));
            return this;
        }

        @Override
        public ResponseBuilder type(String type) {
            delegate.type(type);
            return this;
        }

        @Override
        public ResponseBuilder variant(Variant variant) {
            delegate.variant(new JakartaVariant(variant));
            return this;
        }

        @Override
        public ResponseBuilder contentLocation(URI location) {
            delegate.contentLocation(location);
            return this;
        }

        @Override
        public ResponseBuilder cookie(NewCookie... cookies) {
            delegate.cookie(Arrays.stream(cookies)
                                    .map(JakartaNewCookie::new)
                                    .map(jakarta.ws.rs.core.NewCookie.class::cast)
                                    .toArray(jakarta.ws.rs.core.NewCookie[]::new));
            return this;
        }

        @Override
        public ResponseBuilder expires(Date expires) {
            delegate.expires(expires);
            return this;
        }

        @Override
        public ResponseBuilder lastModified(Date lastModified) {
            delegate.lastModified(lastModified);
            return this;
        }

        @Override
        public ResponseBuilder location(URI location) {
            delegate.location(location);
            return this;
        }

        @Override
        public ResponseBuilder tag(EntityTag tag) {
            delegate.tag(JaxRsShim.toJakarta(tag));
            return this;
        }

        @Override
        public ResponseBuilder tag(String tag) {
            delegate.tag(tag);
            return this;
        }

        @Override
        public ResponseBuilder variants(Variant... variants) {
            delegate.variants(Arrays.stream(variants)
                                      .map(JakartaVariant::new)
                                      .map(jakarta.ws.rs.core.Variant.class::cast)
                                      .toArray(jakarta.ws.rs.core.Variant[]::new));
            return this;
        }

        @Override
        public ResponseBuilder variants(List<Variant> variants) {
            delegate.variants(variants.stream()
                                      .map(JakartaVariant::new)
                                      .map(jakarta.ws.rs.core.Variant.class::cast)
                                      .toList());
            return this;
        }

        @Override
        public ResponseBuilder links(Link... links) {
            delegate.links(Arrays.stream(links)
                                   .map(JakartaLink::new)
                                   .map(jakarta.ws.rs.core.Link.class::cast)
                                   .toArray(jakarta.ws.rs.core.Link[]::new));
            return this;
        }

        @Override
        public ResponseBuilder link(URI uri, String rel) {
            delegate.link(uri, rel);
            return this;
        }

        @Override
        public ResponseBuilder link(String uri, String rel) {
            delegate.link(uri, rel);
            return this;
        }
    }
}

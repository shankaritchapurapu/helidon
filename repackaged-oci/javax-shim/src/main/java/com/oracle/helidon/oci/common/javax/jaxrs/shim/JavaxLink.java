/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

public class JavaxLink extends Link {

    private final jakarta.ws.rs.core.Link delegate;

    public JavaxLink(jakarta.ws.rs.core.Link delegate) {
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
    public String getRel() {
        return delegate.getRel();
    }

    @Override
    public List<String> getRels() {
        return delegate.getRels();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public Map<String, String> getParams() {
        return delegate.getParams();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public static class Builder implements Link.Builder {

        private final jakarta.ws.rs.core.Link.Builder delegate;

        public Builder(jakarta.ws.rs.core.Link.Builder delegate) {
            this.delegate = delegate;
        }

        @Override
        public Link.Builder link(Link link) {
            return new Builder(delegate.link(new JakartaLink(link)));
        }

        @Override
        public Link.Builder link(String link) {
            return new Builder(delegate.link(link));
        }

        @Override
        public Link.Builder uri(URI uri) {
            return new Builder(delegate.uri(uri));
        }

        @Override
        public Link.Builder uri(String uri) {
            return new Builder(delegate.uri(uri));
        }

        @Override
        public Link.Builder baseUri(URI uri) {
            return new Builder(delegate.baseUri(uri));
        }

        @Override
        public Link.Builder baseUri(String uri) {
            return new Builder(delegate.baseUri(uri));
        }

        @Override
        public Link.Builder uriBuilder(UriBuilder uriBuilder) {
            return new Builder(delegate.uriBuilder(new JakartaUriBuilder(uriBuilder)));
        }

        @Override
        public Link.Builder rel(String rel) {
            return new Builder(delegate.rel(rel));
        }

        @Override
        public Link.Builder title(String title) {
            return new Builder(delegate.title(title));
        }

        @Override
        public Link.Builder type(String type) {
            return new Builder(delegate.type(type));
        }

        @Override
        public Link.Builder param(String name, String value) {
            return new Builder(delegate.param(name, value));
        }

        @Override
        public Link build(Object... values) {
            return new JavaxLink(delegate.build(values));
        }

        @Override
        public Link buildRelativized(URI uri, Object... values) {
            return new JavaxLink(delegate.buildRelativized(uri, values));
        }
    }
}

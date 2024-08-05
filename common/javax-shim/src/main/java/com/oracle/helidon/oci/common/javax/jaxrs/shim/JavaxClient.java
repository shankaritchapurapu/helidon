/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import jakarta.ws.rs.core.Configurable;

public class JavaxClient implements JavaxAbstractConfigurable<Client>, Client {

    private final jakarta.ws.rs.client.Client delegate;

    public JavaxClient(jakarta.ws.rs.client.Client delegate) {
        this.delegate = delegate;
    }

    @Override
    public Configurable<?> delegate() {
        return delegate;
    }

    @Override
    public Client self() {
        return this;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public WebTarget target(String uri) {
        return new JavaxWebTarget(delegate.target(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        return new JavaxWebTarget(delegate.target(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new JavaxWebTarget(delegate.target(new JakartaUriBuilder(uriBuilder)));
    }

    @Override
    public WebTarget target(Link link) {
        return new JavaxWebTarget(delegate.target(new JakartaLink(link)));
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        return new JavaxInvocation.Builder(delegate.invocation(new JakartaLink(link)));
    }

    @Override
    public SSLContext getSslContext() {
        return delegate.getSslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return delegate.getHostnameVerifier();
    }

    @Override
    public Configuration getConfiguration() {
        return new JavaxConfiguration(delegate.getConfiguration());
    }

    @Override
    public Client property(String name, Object value) {
        return new JavaxClient(delegate.property(name, value));
    }
}

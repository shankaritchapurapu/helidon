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

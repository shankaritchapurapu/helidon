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
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;

import com.oracle.bmc.http.client.RequestInterceptor;

public class JakartaClient implements Client {

    private final javax.ws.rs.client.Client delegate;

    public JakartaClient(javax.ws.rs.client.Client delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public WebTarget target(String uri) {
        return new JakartaWebTarget(delegate.target(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        return new JakartaWebTarget(delegate.target(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new JakartaWebTarget(delegate.target(new JavaxUriBuilder(uriBuilder)));
    }

    @Override
    public WebTarget target(Link link) {
        return new JakartaWebTarget(delegate.target(new JavaxLink(link)));
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        return new JakartaInvocation.Builder(delegate.invocation(new JavaxLink(link)));
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
        return new JakartaConfiguration(delegate.getConfiguration());
    }

    @Override
    public Client property(String name, Object value) {
        return new JakartaClient(delegate.property(name, value));
    }

    @Override
    public Client register(Class<?> componentClass) {
        return new JakartaClient(delegate.register(componentClass));
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        return new JakartaClient(delegate.register(componentClass, priority));
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        return new JakartaClient(delegate.register(componentClass, contracts));
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return new JakartaClient(delegate.register(componentClass, contracts));
    }

    @Override
    public Client register(Object component) {
        if(component instanceof RequestInterceptor requestInterceptor){
            component = new JakartaRequestInterceptor(requestInterceptor);
        }
        return new JakartaClient(delegate.register(component));
    }

    @Override
    public Client register(Object component, int priority) {
        return new JakartaClient(delegate.register(component, priority));
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        return new JakartaClient(delegate.register(component, contracts));
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        return new JakartaClient(delegate.register(component, contracts));
    }
}

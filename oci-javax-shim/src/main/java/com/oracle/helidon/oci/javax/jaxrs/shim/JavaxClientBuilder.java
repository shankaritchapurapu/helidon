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

import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import jakarta.ws.rs.core.Configurable;

public class JavaxClientBuilder extends ClientBuilder implements JavaxAbstractConfigurable<ClientBuilder> {

    private final jakarta.ws.rs.client.ClientBuilder delegate;

    public JavaxClientBuilder() {
        super();
        this.delegate = jakarta.ws.rs.client.ClientBuilder.newBuilder();
    }

    JavaxClientBuilder(jakarta.ws.rs.client.ClientBuilder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Configurable<?> delegate() {
        return delegate;
    }

    @Override
    public ClientBuilder self() {
        return this;
    }

    @Override
    public ClientBuilder withConfig(Configuration config) {
        return new JavaxClientBuilder(delegate.withConfig(new JakartaConfiguration(config)));
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        return new JavaxClientBuilder(delegate.sslContext(sslContext));
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        return new JavaxClientBuilder(delegate.keyStore(keyStore, password));
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        return new JavaxClientBuilder(delegate.trustStore(trustStore));
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        return new JavaxClientBuilder(delegate.hostnameVerifier(verifier));
    }

    @Override
    public Client build() {
        return new JavaxClient(delegate.build());
    }

    @Override
    public Configuration getConfiguration() {
        return new JavaxConfiguration(delegate.getConfiguration());
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        return new JavaxClientBuilder(delegate.property(name, value));
    }
}

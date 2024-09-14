/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public ClientBuilder executorService(ExecutorService executorService) {
        delegate.executorService(executorService);
        return this;
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        delegate.scheduledExecutorService(scheduledExecutorService);
        return this;
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        delegate.connectTimeout(timeout, unit);
        return this;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        delegate.readTimeout(timeout, unit);
        return this;
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

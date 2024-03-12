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

package com.oracle.pic.vault.util;

import java.security.KeyStore;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.auth.tls.TlsConfig;
import com.oracle.bmc.internal.client.http.OracleConnectorProvider;
import com.oracle.bmc.internal.client.http.OracleHttpClientConfig;
import com.oracle.bmc.internal.client.proxy.ClientProxyConfig;
import com.oracle.helidon.oci.javax.jaxrs.shim.JakartaConfiguration;
import com.oracle.helidon.oci.javax.jaxrs.shim.JavaxClient;
import com.oracle.helidon.oci.javax.jaxrs.shim.JavaxConfiguration;
import com.oracle.pic.commons.ssl.DynamicSslContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyHttpClientBuilder extends javax.ws.rs.client.ClientBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHttpClientBuilder.class);
    public static final ObjectMapper DEFAULT_MAPPER;
    private static final JacksonJsonProvider JACKSON_JSON_PROVIDER;
    private DynamicSslContextProvider provider;
    OracleHttpClientConfig oracleHttpClientConfig;
    private final JerseyClientBuilder delegate;

    public ProxyHttpClientBuilder(DynamicSslContextProvider provider, OracleHttpClientConfig oracleHttpClientConfig) {
        this.delegate = new JerseyClientBuilder();
        this.provider = provider;
        this.sslContext(provider.getSslContext());
        this.oracleHttpClientConfig = oracleHttpClientConfig;
    }

    public static Client buildHttpClient(OracleHttpClientConfig config, ClientBuilder builder) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(JACKSON_JSON_PROVIDER);
        clientConfig.property("jersey.config.client.disableAutoDiscovery", true);
        clientConfig.property("jersey.config.client.connectTimeout", config.getConnectTimeoutInMilliseconds());
        clientConfig.property("jersey.config.client.readTimeout", config.getReadTimeoutInMilliseconds());
        clientConfig.property("jersey.config.client.async.threadPoolSize", config.getAsyncThreadPoolSize());
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        if (config.getProxyConfig() != null) {
            ClientProxyConfig proxyConfig = config.getProxyConfig();
            String proxyUri = proxyConfig.getProxyUri();
            String proxyUsername = proxyConfig.getProxyUsername();
            String proxyPassword = proxyConfig.getProxyPassword();
            if (StringUtils.isNotBlank(proxyUri)) {
                clientConfig.property("jersey.config.client.proxy.uri", proxyUri);
            } else if (StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword)) {
                clientConfig.property("jersey.config.client.proxy.username", proxyUsername);
                clientConfig.property("jersey.config.client.proxy.password", proxyPassword);
            }
        }

        if (config.getDynamicSslContextProviderConfig() == null) {
            LOGGER.info("DynamicSslContextProviderConfig is not configured. Attempting to use tlsConfig");
            if (config.getTlsConfig() != null) {
                (new ProxyTlsConfigurator(config.getTlsConfig())).customizeBuilder(builder);
            } else if (TlsConfig.isTlsTrustConfiguredThroughEnvVars()) {
                (new ProxyTlsConfigurator(TlsConfig.fromEnvironmentVariables())).customizeBuilder(builder);
            }
        }

        return getClient(config, builder, clientConfig);
    }

    static Client getClient(OracleHttpClientConfig config, ClientBuilder builder, ClientConfig clientConfig) {
        builder.hostnameVerifier(config.getHostnameVerifier());
        Client client = builder.withConfig(new JavaxConfiguration(clientConfig)).build();

        if (config.getDisableRemovingContentLengthHeaderWhenItsValueIsZero() != null && config.getDisableRemovingContentLengthHeaderWhenItsValueIsZero()) {
            ClientRequestFilter filter = new ClientRequestFilter() {
                public void filter(ClientRequestContext requestContext) {
                    if (requestContext.getHeaders().containsKey("Content-Length")) {
                        List values = (List)requestContext.getHeaders().get("Content-Length");
                        if (!values.isEmpty()) {
                            Object contentLengthValue = values.get(0);
                            if (!contentLengthValue.equals("0")) {
                                requestContext.getHeaders().remove("Content-Length");
                            }
                        }
                    }

                }
            };
            client.register(filter);

        }
        return client;
    }

    public Client buildHttpClient(OracleHttpClientConfig config) {
        if (config.getDynamicSslContextProviderConfig() != null) {
            DynamicSslContextProvider provider = new DynamicSslContextProvider();

            try {
                provider.initialize(config.getDynamicSslContextProviderConfig());
                LOGGER.info("DynamicSslContextProvider initialized");
            } catch (Exception var3) {
                throw new RuntimeException("failed to initialize the DynamicSslContextProvider", var3);
            }

            return buildHttpClient(config, new ProxyHttpClientBuilder(provider, this.oracleHttpClientConfig));
        } else {
            return buildHttpClient(config, ClientBuilder.newBuilder());
        }
    }

    public Client build() {
        JerseyClient jc = delegate.build();
        jc.getConfiguration().connectorProvider(new OracleConnectorProvider(this.provider));
        return new JavaxClient(jc);
    }

    static {
        DEFAULT_MAPPER = (new ObjectMapper()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JACKSON_JSON_PROVIDER = new JacksonJaxbJsonProvider(DEFAULT_MAPPER, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
    }

    //------------------------------------------------------------

    @Override
    public javax.ws.rs.core.Configuration getConfiguration() {
        return new JavaxConfiguration(delegate.getConfiguration());
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        delegate.property(name, value);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        delegate.register(componentClass);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        delegate.register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        delegate.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        delegate.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component) {
        delegate.register(component);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        delegate.register(component, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        delegate.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        delegate.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder withConfig(javax.ws.rs.core.Configuration config) {
        delegate.withConfig(new JakartaConfiguration(config));
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        delegate.sslContext(sslContext);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        delegate.keyStore(keyStore, password);
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        delegate.trustStore(trustStore);
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        delegate.hostnameVerifier(verifier);
        return this;
    }
}

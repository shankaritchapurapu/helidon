/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.pic.identity.authentication;

import com.oracle.bmc.http.client.ClientProperty;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.bmc.http.client.jersey3.apacheconfigurator.ApacheConfigurator;
import com.oracle.pic.commons.ssl.DynamicSslContextProvider;
import com.oracle.pic.commons.ssl.DynamicSslContextProviderConfig;

import java.util.Optional;

public class SwaggerClientConfigurator extends ApacheConfigurator {

    private final DynamicSslContextProvider dynamicSslContextProvider = new DynamicSslContextProvider();
    private DynamicSslContextProviderConfig dynamicSslContextProviderConfig;
    private Optional<String> proxyURL;

    private SwaggerClientConfigurator(DynamicSslContextProviderConfig dynamicSslContextProviderConfig,
                                      Optional<String> proxyURL) {
        super();
        this.dynamicSslContextProviderConfig = dynamicSslContextProviderConfig;
        this.proxyURL = proxyURL;
    }

    @Override
    public void customizeClient(HttpClientBuilder builder) {
        super.customizeClient(builder);
        dynamicSslContextProvider.initialize(dynamicSslContextProviderConfig);
        try {
            //            builder.sslContext(dynamicSslContextProvider.getSslContext());
            builder.property(StandardClientProperties.SSL_CONTEXT, dynamicSslContextProvider.getSslContext());
            //            ClientConfig clientConfig = new ClientConfig();
            //            clientConfig.connectorProvider(new OracleConnectorProvider(dynamicSslContextProvider));
            //            builder.withConfig(clientConfig);

            if(proxyURL.isPresent()) {
                //                 builder.property(ClientProperties.PROXY_URI, proxyURL.get());
                builder.property(ClientProperty.create("jersey.config.client.proxy.uri"), proxyURL.get());
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed generating client configurator", ex);
        }
    }

    public static SwaggerClientConfiguratorBuilder builder() {
        return new SwaggerClientConfiguratorBuilder ();
    }

    public static class SwaggerClientConfiguratorBuilder {
        private DynamicSslContextProviderConfig dynamicSslContextProviderConfig;
        private Optional<String> rootCertPath = Optional.empty();
        private Optional<String> proxyURL = Optional.empty();

        // No-arg constructor
        public SwaggerClientConfiguratorBuilder() {}

        public SwaggerClientConfiguratorBuilder dynamicSslContextProviderConfig(DynamicSslContextProviderConfig dynamicSslContextProviderConfig) {
            this.dynamicSslContextProviderConfig = dynamicSslContextProviderConfig;
            return this;
        }

        public SwaggerClientConfiguratorBuilder rootCertPath(String rootCertPath) {
            this.rootCertPath = Optional.of(rootCertPath);
            return this;
        }

        public SwaggerClientConfiguratorBuilder proxyURL(Optional<String> proxyURL) {
            this.proxyURL = proxyURL;
            return this;
        }

        public SwaggerClientConfigurator build() {

            if (this.dynamicSslContextProviderConfig == null) {

                if (!this.rootCertPath.isPresent()) {
                    this.rootCertPath = Optional.of("/etc/oci-pki/ca-bundle.pem");
                }

                this.dynamicSslContextProviderConfig = new DynamicSslContextProviderConfig(null,
                                                                                           null,
                                                                                           null,
                                                                                           this.rootCertPath.get(),
                                                                                           Constants.DEFAULT_ROOT_CERT_CHECK);
            }

            return new SwaggerClientConfigurator(dynamicSslContextProviderConfig, proxyURL);
        }
    }
}

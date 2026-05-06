/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.Service;
import com.oracle.bmc.ServiceDetails;
import com.oracle.bmc.Services;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ProvidesClientConfigurators;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.JerseyDefaultConnectorConfigurator;
import com.oracle.bmc.http.internal.ResponseConversionFunctionFactoryV2;
import com.oracle.bmc.http.internal.RestClient;
import com.oracle.bmc.http.internal.RestClientFactory;
import com.oracle.bmc.http.internal.RestClientFactoryBuilder;
import com.oracle.bmc.http.internal.RetryUtils;
import com.oracle.bmc.http.internal.WithHeaders;
import com.oracle.bmc.http.internal.WrappedInvocationBuilder;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import com.oracle.bmc.http.signing.SigningStrategy;
import com.oracle.bmc.http.signing.internal.DefaultRequestSignerFactory;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.requests.BmcRequest;
import com.oracle.bmc.retrier.BmcGenericRetrier;
import com.oracle.bmc.retrier.Retriers;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.TokenRefreshRetrier;
import com.oracle.bmc.util.CircuitBreakerUtils;
import com.oracle.bmc.util.internal.HttpUtils;
import com.oracle.bmc.util.internal.Validate;
import com.oracle.pic.vault.model.GetSecretResponse;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

final class Ssv2VaultClient implements AutoCloseable {
    private static final Service SERVICE = Services.serviceBuilder()
            .serviceName("VAULTUSER")
            .serviceEndpointPrefix("")
            .serviceEndpointTemplate("https://secret-service*.*.{region}*/v1")
            .build();
    private static final ClientConfigurator DEFAULT_CLIENT_CONFIGURATOR = new JerseyDefaultConnectorConfigurator();
    private static final ResponseConversionFunctionFactoryV2 RESPONSE_CONVERSION_FACTORY =
            new ResponseConversionFunctionFactoryV2();

    private final BasicAuthenticationDetailsProvider authenticationDetailsProvider;
    private final RetryConfiguration retryConfiguration;
    private final RestClient client;

    Ssv2VaultClient(BasicAuthenticationDetailsProvider authenticationDetailsProvider,
                    ClientConfiguration clientConfiguration,
                    ClientConfigurator clientConfigurator,
                    String endpoint) {
        this.authenticationDetailsProvider = Objects.requireNonNull(authenticationDetailsProvider,
                                                                    "authenticationDetailsProvider");
        ClientConfiguration configuration = Objects.requireNonNullElseGet(clientConfiguration,
                                                                          () -> ClientConfiguration.builder().build());
        this.retryConfiguration = configuration.getRetryConfiguration();
        this.client = restClient(authenticationDetailsProvider, configuration, clientConfigurator);
        this.client.setEndpoint(Objects.requireNonNull(endpoint, "endpoint"));
    }

    Map<String, String> getSecret(String path) {
        Validate.notBlank(path, "path must not be blank");

        GetSecretRequest request = new GetSecretRequest(path);
        WrappedInvocationBuilder requestBuilder = fromRequest(request);
        BmcGenericRetrier retrier = Retriers.createPreferredRetrier(request.getRetryConfiguration(),
                                                                    retryConfiguration,
                                                                    true);
        RetryUtils.setClientRetriesHeader(requestBuilder, retrier);

        Function<Response, Map<String, String>> responseConverter = responseConverter(requestBuilder);
        try {
            return retrier.execute(request,
                                   retryRequest -> new TokenRefreshRetrier(authenticationDetailsProvider)
                                           .execute(retryRequest, tokenRefreshRequest -> {
                                               Response response = client.get(requestBuilder, tokenRefreshRequest);
                                               return responseConverter.apply(response);
                                           }));
        } catch (BmcException e) {
            if (e.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private WrappedInvocationBuilder fromRequest(GetSecretRequest request) {
        WrappedInvocationBuilder requestBuilder = client.getBaseTarget()
                .path(HttpUtils.encodePathSegment(request.path()))
                .request();
        requestBuilder.accept("application/json");
        if (client.getClientConfigurator() != null) {
            client.getClientConfigurator().customizeRequest(request, requestBuilder);
        }
        return requestBuilder;
    }

    private static Function<Response, Map<String, String>> responseConverter(WrappedInvocationBuilder requestBuilder) {
        ServiceDetails serviceDetails = new ServiceDetails("VaultUser",
                                                           "GetSecret",
                                                           requestBuilder.getRequestUri().toString(),
                                                           "");
        return response -> {
            WithHeaders<GetSecretResponse> converted =
                    RESPONSE_CONVERSION_FACTORY.create(GetSecretResponse.class, serviceDetails).apply(response);
            GetSecretResponse body = converted.getItem();
            return body == null ? null : body.getData();
        };
    }

    private static RestClient restClient(BasicAuthenticationDetailsProvider authenticationDetailsProvider,
                                         ClientConfiguration clientConfiguration,
                                         ClientConfigurator clientConfigurator) {
        List<ClientConfigurator> configurators = new ArrayList<>(additionalClientConfigurators(authenticationDetailsProvider));
        configurators.add(new HttpUrlConnectorConfigurator());
        if (clientConfigurator != null) {
            configurators.add(clientConfigurator);
        }

        RestClientFactory restClientFactory = RestClientFactoryBuilder.builder()
                .clientConfigurator(DEFAULT_CLIENT_CONFIGURATOR)
                .additionalClientConfigurators(configurators)
                .build();
        RequestSignerFactory signerFactory = new DefaultRequestSignerFactory(SigningStrategy.STANDARD);
        RequestSigner signer = signerFactory.createRequestSigner(SERVICE, authenticationDetailsProvider);
        CircuitBreakerConfiguration circuitBreakerConfiguration =
                CircuitBreakerUtils.getUserDefinedCircuitBreakerConfiguration(clientConfiguration);
        if (circuitBreakerConfiguration == null) {
            circuitBreakerConfiguration = CircuitBreakerUtils.DEFAULT_CIRCUIT_BREAKER_CONFIGURATION;
        }
        return restClientFactory.create(signer,
                                        signingStrategyRequestSigners(authenticationDetailsProvider),
                                        clientConfiguration,
                                        false,
                                        null,
                                        circuitBreakerConfiguration);
    }

    private static List<ClientConfigurator> additionalClientConfigurators(
            BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
        if (authenticationDetailsProvider instanceof ProvidesClientConfigurators providerConfigurators) {
            return providerConfigurators.getClientConfigurators();
        }
        return List.of();
    }

    private static Map<SigningStrategy, RequestSigner> signingStrategyRequestSigners(
            BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
        Map<SigningStrategy, RequestSigner> signers = new EnumMap<>(SigningStrategy.class);
        DefaultRequestSignerFactory.createDefaultRequestSignerFactories()
                .forEach((strategy, signerFactory) -> signers.put(strategy,
                                                                  signerFactory.createRequestSigner(
                                                                          SERVICE,
                                                                          authenticationDetailsProvider)));
        return signers;
    }

    private static final class HttpUrlConnectorConfigurator implements ClientConfigurator {
        @Override
        public void customizeBuilder(ClientBuilder builder) {
            ClientConfig config = new ClientConfig().loadFrom(builder.getConfiguration());
            config.connectorProvider(new HttpUrlConnectorProvider().useSetMethodWorkaround());
            builder.withConfig(config);
        }

        @Override
        public void customizeClient(Client client) {
        }
    }

    private static final class GetSecretRequest extends BmcRequest<Void> {
        private final String path;

        private GetSecretRequest(String path) {
            this.path = path;
        }

        private String path() {
            return path;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof GetSecretRequest that)) {
                return false;
            }
            return super.equals(other)
                    && Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), path);
        }
    }
}

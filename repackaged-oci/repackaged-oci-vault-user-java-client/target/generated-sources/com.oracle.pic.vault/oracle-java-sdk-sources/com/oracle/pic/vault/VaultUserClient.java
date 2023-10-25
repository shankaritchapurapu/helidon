package com.oracle.pic.vault;

import com.oracle.bmc.util.internal.Validate;
import com.oracle.pic.vault.requests.*;
import com.oracle.pic.vault.responses.*;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.util.CircuitBreakerUtils;

import java.util.Objects;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class VaultUserClient extends com.oracle.bmc.http.internal.BaseSyncClient implements VaultUser {
    /**
     * Service instance for VaultUser.
     */
    public static final com.oracle.bmc.Service SERVICE = com.oracle.bmc.Services.serviceBuilder().serviceName("VAULTUSER").serviceEndpointPrefix("").serviceEndpointTemplate("https://secret-service*.*.{region}*/v1").build();

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VaultUserClient.class);



    VaultUserClient(
            com.oracle.bmc.common.ClientBuilderBase<?, ?> builder,
            com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider
            ) {
        super(
            builder,
            authenticationDetailsProvider,
            CircuitBreakerUtils.DEFAULT_CIRCUIT_BREAKER_CONFIGURATION
            
        );

    }


    
    /**
     * Create a builder for this client.
     * @return builder
     */
    public static Builder builder() {
        return new Builder(SERVICE);
    }

    /**
     * Builder class for this client. The "authenticationDetailsProvider" is required and must be passed to the
     * {@link #build(AbstractAuthenticationDetailsProvider)} method.
     */
    public static class Builder extends com.oracle.bmc.common.RegionalClientBuilder<Builder, VaultUserClient> {
        private Builder(com.oracle.bmc.Service service) {
            super(service);
            requestSignerFactory = new com.oracle.bmc.http.signing.internal.DefaultRequestSignerFactory(com.oracle.bmc.http.signing.SigningStrategy.STANDARD);
        }

        /**
         * Build the client.
         * @param authenticationDetailsProvider authentication details provider
         * @return the client
         */
        public VaultUserClient build(@jakarta.annotation.Nonnull com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
            return new VaultUserClient(this, authenticationDetailsProvider);
        }
    }

    @Override
    public void setRegion(com.oracle.bmc.Region region) {
        super.setRegion(region);
    }

    @Override
    public void setRegion(String regionId) {
        super.setRegion(regionId);
    }

    @Override
    
    public DeleteSecretResponse deleteSecret(DeleteSecretRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPath(), "path must not be blank");


return clientCall(request, DeleteSecretResponse::builder)
        .logger(LOG, "deleteSecret")
        .serviceDetails("VaultUser", "DeleteSecret", "")
        .method(com.oracle.bmc.http.client.Method.DELETE)
        .requestBuilder(DeleteSecretRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam(request.getPath())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public GetSecretResponse getSecret(GetSecretRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPath(), "path must not be blank");


return clientCall(request, GetSecretResponse::builder)
        .logger(LOG, "getSecret")
        .serviceDetails("VaultUser", "GetSecret", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(GetSecretRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam(request.getPath())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
                
                .appendHeader("sdk-version", request.getSdkVersion())
        
        
            .handleBody(com.oracle.pic.vault.model.GetSecretResponse.class, GetSecretResponse.Builder::getSecretResponse)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public ListSecretsResponse listSecrets(ListSecretsRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getSecretPath(), "secretPath must not be blank");
        Objects.requireNonNull(request.getList(), "list is required");
        


return clientCall(request, ListSecretsResponse::builder)
        .logger(LOG, "listSecrets")
        .serviceDetails("VaultUser", "ListSecrets", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(ListSecretsRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam(request.getSecretPath())
            
                
                    
                    .appendQueryParam("list", request.getList())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
                
                .appendHeader("sdk-version", request.getSdkVersion())
        
        
            .handleBody(com.oracle.pic.vault.model.ListSecretsResponse.class, ListSecretsResponse.Builder::listSecretsResponse)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public LoginUsingIdentityResponse loginUsingIdentity(LoginUsingIdentityRequest request) {
                Objects.requireNonNull(request.getAuthenticateClientRequest(), "authenticateClientRequest is required");
        


return clientCall(request, LoginUsingIdentityResponse::builder)
        .logger(LOG, "loginUsingIdentity")
        .serviceDetails("VaultUser", "LoginUsingIdentity", "")
        .method(com.oracle.bmc.http.client.Method.POST)
        .requestBuilder(LoginUsingIdentityRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam("internaloci").appendPathParam("login")
        .accept("application/json")
                
                .appendHeader("sdk-version", request.getSdkVersion())
        
        .hasBody()
            .handleBody(com.oracle.pic.vault.model.LoginResponse.class, LoginUsingIdentityResponse.Builder::loginResponse)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public LoginUsingPkiResponse loginUsingPki(LoginUsingPkiRequest request) {
        

return clientCall(request, LoginUsingPkiResponse::builder)
        .logger(LOG, "loginUsingPki")
        .serviceDetails("VaultUser", "LoginUsingPki", "")
        .method(com.oracle.bmc.http.client.Method.POST)
        .requestBuilder(LoginUsingPkiRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam("pki").appendPathParam("login")
        .accept("application/json")
                
                .appendHeader("sdk-version", request.getSdkVersion())
        
        
            .handleBody(com.oracle.pic.vault.model.LoginResponse.class, LoginUsingPkiResponse.Builder::loginResponse)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public PostSecretResponse postSecret(PostSecretRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPath(), "path must not be blank");
        Objects.requireNonNull(request.getBody(), "body is required");
        


return clientCall(request, PostSecretResponse::builder)
        .logger(LOG, "postSecret")
        .serviceDetails("VaultUser", "PostSecret", "")
        .method(com.oracle.bmc.http.client.Method.POST)
        .requestBuilder(PostSecretRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam(request.getPath())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        .hasBody()

                .operationUsesDefaultRetries()
                .callSync();
    }



    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(com.oracle.bmc.auth.BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
        this(
            builder(),
            authenticationDetailsProvider
        );
    }

    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @param configuration {@link Builder#configuration}
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(com.oracle.bmc.auth.BasicAuthenticationDetailsProvider authenticationDetailsProvider, com.oracle.bmc.ClientConfiguration configuration) {
        this(
            builder()
                .configuration(configuration),
            authenticationDetailsProvider
        );
    }

    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @param configuration {@link Builder#configuration}
     * @param clientConfigurator {@link Builder#clientConfigurator}
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(com.oracle.bmc.auth.BasicAuthenticationDetailsProvider authenticationDetailsProvider, com.oracle.bmc.ClientConfiguration configuration, com.oracle.bmc.http.ClientConfigurator clientConfigurator) {
        this(
            builder()
                .configuration(configuration)
                .clientConfigurator(clientConfigurator),
            authenticationDetailsProvider
        );
    }

    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @param configuration {@link Builder#configuration}
     * @param clientConfigurator {@link Builder#clientConfigurator}
     * @param defaultRequestSignerFactory {@link Builder#requestSignerFactory}
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(
            com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider,
            com.oracle.bmc.ClientConfiguration configuration,
            com.oracle.bmc.http.ClientConfigurator clientConfigurator,
            com.oracle.bmc.http.signing.RequestSignerFactory defaultRequestSignerFactory) {
        this(
            builder()
                .configuration(configuration)
                .clientConfigurator(clientConfigurator)
                .requestSignerFactory(defaultRequestSignerFactory),
            authenticationDetailsProvider
        );
    }

    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @param configuration {@link Builder#configuration}
     * @param clientConfigurator {@link Builder#clientConfigurator}
     * @param defaultRequestSignerFactory {@link Builder#requestSignerFactory}
     * @param additionalClientConfigurators {@link Builder#additionalClientConfigurators}
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(
            com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider,
            com.oracle.bmc.ClientConfiguration configuration,
            com.oracle.bmc.http.ClientConfigurator clientConfigurator,
            com.oracle.bmc.http.signing.RequestSignerFactory defaultRequestSignerFactory,
            java.util.List<com.oracle.bmc.http.ClientConfigurator> additionalClientConfigurators) {
        this(
            builder()
                .configuration(configuration)
                .clientConfigurator(clientConfigurator)
                .requestSignerFactory(defaultRequestSignerFactory)
                .additionalClientConfigurators(additionalClientConfigurators),
            authenticationDetailsProvider
        );
    }

    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @param configuration {@link Builder#configuration}
     * @param clientConfigurator {@link Builder#clientConfigurator}
     * @param defaultRequestSignerFactory {@link Builder#requestSignerFactory}
     * @param additionalClientConfigurators {@link Builder#additionalClientConfigurators}
     * @param endpoint {@link Builder#endpoint}
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(
            com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider,
            com.oracle.bmc.ClientConfiguration configuration,
            com.oracle.bmc.http.ClientConfigurator clientConfigurator,
            com.oracle.bmc.http.signing.RequestSignerFactory defaultRequestSignerFactory,
            java.util.List<com.oracle.bmc.http.ClientConfigurator> additionalClientConfigurators,
            String endpoint) {
        this(
            builder()
                .configuration(configuration)
                .clientConfigurator(clientConfigurator)
                .requestSignerFactory(defaultRequestSignerFactory)
                .additionalClientConfigurators(additionalClientConfigurators)
                .endpoint(endpoint),
            authenticationDetailsProvider
        );
    }

    /**
     * Create a new client instance.
     *
     * @param authenticationDetailsProvider The authentication details (see {@link Builder#build})
     * @param configuration {@link Builder#configuration}
     * @param clientConfigurator {@link Builder#clientConfigurator}
     * @param defaultRequestSignerFactory {@link Builder#requestSignerFactory}
     * @param additionalClientConfigurators {@link Builder#additionalClientConfigurators}
     * @param endpoint {@link Builder#endpoint}
     * @param signingStrategyRequestSignerFactories {@link Builder#signingStrategyRequestSignerFactories}
     * @deprecated Use the {@link #builder() builder} instead.
     */
    @Deprecated
    public VaultUserClient(
            com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider,
            com.oracle.bmc.ClientConfiguration configuration,
            com.oracle.bmc.http.ClientConfigurator clientConfigurator,
            com.oracle.bmc.http.signing.RequestSignerFactory defaultRequestSignerFactory,
            java.util.Map<com.oracle.bmc.http.signing.SigningStrategy, com.oracle.bmc.http.signing.RequestSignerFactory> signingStrategyRequestSignerFactories,
            java.util.List<com.oracle.bmc.http.ClientConfigurator> additionalClientConfigurators,
            String endpoint) {
        this(
            builder()
                .configuration(configuration)
                .clientConfigurator(clientConfigurator)
                .requestSignerFactory(defaultRequestSignerFactory)
                .additionalClientConfigurators(additionalClientConfigurators)
                .endpoint(endpoint)
                .signingStrategyRequestSignerFactories(signingStrategyRequestSignerFactories),
            authenticationDetailsProvider
        );
    }
}

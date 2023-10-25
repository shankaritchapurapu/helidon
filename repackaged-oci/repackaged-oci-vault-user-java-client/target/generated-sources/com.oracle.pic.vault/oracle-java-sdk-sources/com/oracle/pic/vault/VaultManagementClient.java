package com.oracle.pic.vault;

import com.oracle.bmc.util.internal.Validate;
import com.oracle.pic.vault.requests.*;
import com.oracle.pic.vault.responses.*;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.util.CircuitBreakerUtils;

import java.util.Objects;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class VaultManagementClient extends com.oracle.bmc.http.internal.BaseSyncClient implements VaultManagement {
    /**
     * Service instance for VaultManagement.
     */
    public static final com.oracle.bmc.Service SERVICE = com.oracle.bmc.Services.serviceBuilder().serviceName("VAULTMANAGEMENT").serviceEndpointPrefix("").serviceEndpointTemplate("https://secret-service*.*.{region}*/v1").build();

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VaultManagementClient.class);



    VaultManagementClient(
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
    public static class Builder extends com.oracle.bmc.common.RegionalClientBuilder<Builder, VaultManagementClient> {
        private Builder(com.oracle.bmc.Service service) {
            super(service);
            requestSignerFactory = new com.oracle.bmc.http.signing.internal.DefaultRequestSignerFactory(com.oracle.bmc.http.signing.SigningStrategy.STANDARD);
        }

        /**
         * Build the client.
         * @param authenticationDetailsProvider authentication details provider
         * @return the client
         */
        public VaultManagementClient build(@jakarta.annotation.Nonnull com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
            return new VaultManagementClient(this, authenticationDetailsProvider);
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
    
    public CreateConfigResponse createConfig(CreateConfigRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getConfigName(), "configName must not be blank");
        Objects.requireNonNull(request.getCreateConfigDetails(), "createConfigDetails is required");
        


return clientCall(request, CreateConfigResponse::builder)
        .logger(LOG, "createConfig")
        .serviceDetails("VaultManagement", "CreateConfig", "")
        .method(com.oracle.bmc.http.client.Method.POST)
        .requestBuilder(CreateConfigRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("config").appendPathParam(request.getConfigName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        .hasBody()
            .handleBody(com.oracle.pic.vault.model.ConfigDetails.class, CreateConfigResponse.Builder::configDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public DeleteConfigResponse deleteConfig(DeleteConfigRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getConfigName(), "configName must not be blank");
        Objects.requireNonNull(request.getIfMatch(), "ifMatch is required");
        


return clientCall(request, DeleteConfigResponse::builder)
        .logger(LOG, "deleteConfig")
        .serviceDetails("VaultManagement", "DeleteConfig", "")
        .method(com.oracle.bmc.http.client.Method.DELETE)
        .requestBuilder(DeleteConfigRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("config").appendPathParam(request.getConfigName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
                
                .appendHeader("if-match", request.getIfMatch())
        
        

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public DeletePolicyResponse deletePolicy(DeletePolicyRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPolicyName(), "policyName must not be blank");


return clientCall(request, DeletePolicyResponse::builder)
        .logger(LOG, "deletePolicy")
        .serviceDetails("VaultManagement", "DeletePolicy", "")
        .method(com.oracle.bmc.http.client.Method.DELETE)
        .requestBuilder(DeletePolicyRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("sys").appendPathParam("policy").appendPathParam(request.getPolicyName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public DeleteRoleResponse deleteRole(DeleteRoleRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getRoleName(), "roleName must not be blank");


return clientCall(request, DeleteRoleResponse::builder)
        .logger(LOG, "deleteRole")
        .serviceDetails("VaultManagement", "DeleteRole", "")
        .method(com.oracle.bmc.http.client.Method.DELETE)
        .requestBuilder(DeleteRoleRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("role").appendPathParam(request.getRoleName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public DeleteSecretResponse deleteSecret(DeleteSecretRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPath(), "path must not be blank");


return clientCall(request, DeleteSecretResponse::builder)
        .logger(LOG, "deleteSecret")
        .serviceDetails("VaultManagement", "DeleteSecret", "")
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
    
    public GetConfigResponse getConfig(GetConfigRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getConfigName(), "configName must not be blank");


return clientCall(request, GetConfigResponse::builder)
        .logger(LOG, "getConfig")
        .serviceDetails("VaultManagement", "GetConfig", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(GetConfigRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("config").appendPathParam(request.getConfigName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        
            .handleBody(com.oracle.pic.vault.model.ConfigDetails.class, GetConfigResponse.Builder::configDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public GetPolicyResponse getPolicy(GetPolicyRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPolicyName(), "policyName must not be blank");


return clientCall(request, GetPolicyResponse::builder)
        .logger(LOG, "getPolicy")
        .serviceDetails("VaultManagement", "GetPolicy", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(GetPolicyRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("sys").appendPathParam("policy").appendPathParam(request.getPolicyName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        
            .handleBody(com.oracle.pic.vault.model.PolicyDetails.class, GetPolicyResponse.Builder::policyDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public GetRoleResponse getRole(GetRoleRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getRoleName(), "roleName must not be blank");


return clientCall(request, GetRoleResponse::builder)
        .logger(LOG, "getRole")
        .serviceDetails("VaultManagement", "GetRole", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(GetRoleRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("role").appendPathParam(request.getRoleName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        
            .handleBody(com.oracle.pic.vault.model.RoleDetails.class, GetRoleResponse.Builder::roleDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public GetSecretResponse getSecret(GetSecretRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPath(), "path must not be blank");


return clientCall(request, GetSecretResponse::builder)
        .logger(LOG, "getSecret")
        .serviceDetails("VaultManagement", "GetSecret", "")
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
    
    public ListConfigsResponse listConfigs(ListConfigsRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        Objects.requireNonNull(request.getList(), "list is required");
        


return clientCall(request, ListConfigsResponse::builder)
        .logger(LOG, "listConfigs")
        .serviceDetails("VaultManagement", "ListConfigs", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(ListConfigsRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("config")
            
                
                    
                    .appendQueryParam("list", request.getList())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        
            .handleBody(com.oracle.pic.vault.model.ListDetails.class, ListConfigsResponse.Builder::listDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public ListPoliciesResponse listPolicies(ListPoliciesRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        Objects.requireNonNull(request.getList(), "list is required");
        


return clientCall(request, ListPoliciesResponse::builder)
        .logger(LOG, "listPolicies")
        .serviceDetails("VaultManagement", "ListPolicies", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(ListPoliciesRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("sys").appendPathParam("policy")
            
                
                    
                    .appendQueryParam("list", request.getList())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        
            .handleBody(com.oracle.pic.vault.model.PolicyListDetails.class, ListPoliciesResponse.Builder::policyListDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public ListRolesResponse listRoles(ListRolesRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        Objects.requireNonNull(request.getList(), "list is required");
        


return clientCall(request, ListRolesResponse::builder)
        .logger(LOG, "listRoles")
        .serviceDetails("VaultManagement", "ListRoles", "")
        .method(com.oracle.bmc.http.client.Method.GET)
        .requestBuilder(ListRolesRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("role")
            
                
                    
                    .appendQueryParam("list", request.getList())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        
            .handleBody(com.oracle.pic.vault.model.ListDetails.class, ListRolesResponse.Builder::listDetails)

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
        .serviceDetails("VaultManagement", "ListSecrets", "")
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
        .serviceDetails("VaultManagement", "LoginUsingIdentity", "")
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
        .serviceDetails("VaultManagement", "LoginUsingPki", "")
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
        .serviceDetails("VaultManagement", "PostSecret", "")
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

    @Override
    
    public UpdateConfigResponse updateConfig(UpdateConfigRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getConfigName(), "configName must not be blank");
        Objects.requireNonNull(request.getIfMatch(), "ifMatch is required");
        
        Objects.requireNonNull(request.getUpdateConfigDetails(), "updateConfigDetails is required");
        


return clientCall(request, UpdateConfigResponse::builder)
        .logger(LOG, "updateConfig")
        .serviceDetails("VaultManagement", "UpdateConfig", "")
        .method(com.oracle.bmc.http.client.Method.PUT)
        .requestBuilder(UpdateConfigRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("config").appendPathParam(request.getConfigName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
                
                .appendHeader("if-match", request.getIfMatch())
        
        .hasBody()
            .handleBody(com.oracle.pic.vault.model.ConfigDetails.class, UpdateConfigResponse.Builder::configDetails)

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public UpsertPolicyResponse upsertPolicy(UpsertPolicyRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getPolicyName(), "policyName must not be blank");
        Objects.requireNonNull(request.getBody(), "body is required");
        


return clientCall(request, UpsertPolicyResponse::builder)
        .logger(LOG, "upsertPolicy")
        .serviceDetails("VaultManagement", "UpsertPolicy", "")
        .method(com.oracle.bmc.http.client.Method.PUT)
        .requestBuilder(UpsertPolicyRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("sys").appendPathParam("policy").appendPathParam(request.getPolicyName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        .hasBody()

                .operationUsesDefaultRetries()
                .callSync();
    }

    @Override
    
    public UpsertRoleResponse upsertRole(UpsertRoleRequest request) {
                Objects.requireNonNull(request.getXVaultToken(), "xVaultToken is required");
        
        
        Validate.notBlank(request.getAuthPluginName().getValue(), "authPluginName must not be blank");
        
        Validate.notBlank(request.getRoleName(), "roleName must not be blank");
        Objects.requireNonNull(request.getUpsertRoleDetails(), "upsertRoleDetails is required");
        


return clientCall(request, UpsertRoleResponse::builder)
        .logger(LOG, "upsertRole")
        .serviceDetails("VaultManagement", "UpsertRole", "")
        .method(com.oracle.bmc.http.client.Method.POST)
        .requestBuilder(UpsertRoleRequest::builder)
        
        
        .basePath("/v1")
        .appendPathParam("auth").appendPathParam(request.getAuthPluginName().getValue()).appendPathParam("role").appendPathParam(request.getRoleName())
        .accept("application/json")
                
                .appendHeader("X-Vault-Token", request.getXVaultToken())
        
        .hasBody()
            .handleBody(com.oracle.pic.vault.model.RoleDetails.class, UpsertRoleResponse.Builder::roleDetails)

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
    public VaultManagementClient(com.oracle.bmc.auth.BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
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
    public VaultManagementClient(com.oracle.bmc.auth.BasicAuthenticationDetailsProvider authenticationDetailsProvider, com.oracle.bmc.ClientConfiguration configuration) {
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
    public VaultManagementClient(com.oracle.bmc.auth.BasicAuthenticationDetailsProvider authenticationDetailsProvider, com.oracle.bmc.ClientConfiguration configuration, com.oracle.bmc.http.ClientConfigurator clientConfigurator) {
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
    public VaultManagementClient(
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
    public VaultManagementClient(
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
    public VaultManagementClient(
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
    public VaultManagementClient(
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

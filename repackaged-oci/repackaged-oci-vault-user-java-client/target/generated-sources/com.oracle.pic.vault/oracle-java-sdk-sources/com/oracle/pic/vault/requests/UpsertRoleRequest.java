package com.oracle.pic.vault.requests;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class UpsertRoleRequest extends com.oracle.bmc.requests.BmcRequest<com.oracle.pic.vault.model.UpsertRoleDetails> {

        /**
     * The authorization token.
     */
    private String xVaultToken;

    

        /**
     * The authorization token.
     */
    public String getXVaultToken() {
        return xVaultToken;
    }
        /**
     * The name of the auth plugin.
     */
    private AuthPluginName authPluginName;

        /**
     * The name of the auth plugin.
     **/
    public enum AuthPluginName implements com.oracle.bmc.http.internal.BmcEnum {
        Internaloci("internaloci"),
        Pki("pki"),
        ;

        

        private final String value;
        private static java.util.Map<String, AuthPluginName> map;

        static {
            map = new java.util.HashMap<>();
            for (AuthPluginName v : AuthPluginName.values()) {
                    map.put(v.getValue(), v);
                
            }
        }

        AuthPluginName(String value) {
            this.value = value;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() {
            return value;
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static AuthPluginName create(String key) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            throw new IllegalArgumentException("Invalid AuthPluginName: " + key);
        }
    };

        /**
     * The name of the auth plugin.
     */
    public AuthPluginName getAuthPluginName() {
        return authPluginName;
    }
        /**
     * The name of the role to take. Policies available to this role will be granted. Pass * to take up all roles available to the entity that is calling this API.
     */
    private String roleName;

    

        /**
     * The name of the role to take. Policies available to this role will be granted. Pass * to take up all roles available to the entity that is calling this API.
     */
    public String getRoleName() {
        return roleName;
    }
    
    private com.oracle.pic.vault.model.UpsertRoleDetails upsertRoleDetails;

    

    
    public com.oracle.pic.vault.model.UpsertRoleDetails getUpsertRoleDetails() {
        return upsertRoleDetails;
    }
    

    /**
     * Alternative accessor for the body parameter.
     * @return body parameter
     */
    @Override
    @com.oracle.bmc.InternalSdk
    public com.oracle.pic.vault.model.UpsertRoleDetails getBody$() {
        return upsertRoleDetails;
    }

    public static class Builder implements com.oracle.bmc.requests.BmcRequest.Builder<UpsertRoleRequest, com.oracle.pic.vault.model.UpsertRoleDetails> {
        private com.oracle.bmc.http.client.RequestInterceptor invocationCallback = null;
        private com.oracle.bmc.retrier.RetryConfiguration retryConfiguration = null;

            /**
     * The authorization token.
     */
        private String xVaultToken = null;

        /**
         * The authorization token.
         * @param xVaultToken the value to set
         * @return this builder instance
         */
        public Builder xVaultToken(String xVaultToken) {
            this.xVaultToken = xVaultToken;
            return this;
        }

            /**
     * The name of the auth plugin.
     */
        private AuthPluginName authPluginName = null;

        /**
         * The name of the auth plugin.
         * @param authPluginName the value to set
         * @return this builder instance
         */
        public Builder authPluginName(AuthPluginName authPluginName) {
            this.authPluginName = authPluginName;
            return this;
        }

            /**
     * The name of the role to take. Policies available to this role will be granted. Pass * to take up all roles available to the entity that is calling this API.
     */
        private String roleName = null;

        /**
         * The name of the role to take. Policies available to this role will be granted. Pass * to take up all roles available to the entity that is calling this API.
         * @param roleName the value to set
         * @return this builder instance
         */
        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        
        private com.oracle.pic.vault.model.UpsertRoleDetails upsertRoleDetails = null;

        /**
         * 
         * @param upsertRoleDetails the value to set
         * @return this builder instance
         */
        public Builder upsertRoleDetails(com.oracle.pic.vault.model.UpsertRoleDetails upsertRoleDetails) {
            this.upsertRoleDetails = upsertRoleDetails;
            return this;
        }

        /**
         * Set the invocation callback for the request to be built.
         * @param invocationCallback the invocation callback to be set for the request
         * @return this builder instance
         */
        public Builder invocationCallback(com.oracle.bmc.http.client.RequestInterceptor invocationCallback) {
            this.invocationCallback = invocationCallback;
            return this;
        }

        /**
         * Set the retry configuration for the request to be built.
         * @param retryConfiguration the retry configuration to be used for the request
         * @return this builder instance
         */
        public Builder retryConfiguration(
        com.oracle.bmc.retrier.RetryConfiguration retryConfiguration) {
            this.retryConfiguration = retryConfiguration;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        public Builder copy(UpsertRoleRequest o) {
            xVaultToken(o.getXVaultToken());authPluginName(o.getAuthPluginName());roleName(o.getRoleName());upsertRoleDetails(o.getUpsertRoleDetails());
            invocationCallback(o.getInvocationCallback());
            retryConfiguration(o.getRetryConfiguration());
            return this;
        }

        /**
         * Build the instance of UpsertRoleRequest as configured by this builder
         *
         * Note that this method takes calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#buildWithoutInvocationCallback} does not.
         *
         * This is the preferred method to build an instance.
         *
         * @return instance of UpsertRoleRequest
         */
        public UpsertRoleRequest build() {
            UpsertRoleRequest request = buildWithoutInvocationCallback();
            request.setInvocationCallback(invocationCallback);
            request.setRetryConfiguration(retryConfiguration);
            return request;
        }

        /**
         * Alternative setter for the body parameter.
         * @param body the body parameter
         * @return this builder instance
         */
        @com.oracle.bmc.InternalSdk
        public Builder body$(com.oracle.pic.vault.model.UpsertRoleDetails body) {
            upsertRoleDetails(body);
            return this;
        }

        /**
         * Build the instance of UpsertRoleRequest as configured by this builder
         *
         * Note that this method does not take calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#build} does
         *
         * @return instance of UpsertRoleRequest
         */
        public UpsertRoleRequest buildWithoutInvocationCallback() {
            UpsertRoleRequest request = new UpsertRoleRequest();
            request.xVaultToken = xVaultToken;
            request.authPluginName = authPluginName;
            request.roleName = roleName;
            request.upsertRoleDetails = upsertRoleDetails;
            return request;
            // new UpsertRoleRequest(xVaultToken, authPluginName, roleName, upsertRoleDetails);
        }
    }

    /**
     * Return an instance of {@link Builder} that allows you to modify request properties.
     * @return instance of {@link Builder} that allows you to modify request properties.
     */
    public Builder toBuilder() {
        return new Builder()
            .xVaultToken(xVaultToken)
            .authPluginName(authPluginName)
            .roleName(roleName)
            .upsertRoleDetails(upsertRoleDetails);
    }

    /**
     * Return a new builder for this request object.
     * @return builder for the request object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append("(");
        sb.append("super=").append(super.toString());
        sb.append(",xVaultToken=").append(String.valueOf(this.xVaultToken));
        sb.append(",authPluginName=").append(String.valueOf(this.authPluginName));
        sb.append(",roleName=").append(String.valueOf(this.roleName));
        sb.append(",upsertRoleDetails=").append(String.valueOf(this.upsertRoleDetails));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpsertRoleRequest)) {
            return false;
        }

        UpsertRoleRequest other = (UpsertRoleRequest) o;
        return super.equals(o)
            && java.util.Objects.equals(this.xVaultToken, other.xVaultToken)
            && java.util.Objects.equals(this.authPluginName, other.authPluginName)
            && java.util.Objects.equals(this.roleName, other.roleName)
            && java.util.Objects.equals(this.upsertRoleDetails, other.upsertRoleDetails);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.xVaultToken == null ? 43 : this.xVaultToken.hashCode());
        result = (result * PRIME) + (this.authPluginName == null ? 43 : this.authPluginName.hashCode());
        result = (result * PRIME) + (this.roleName == null ? 43 : this.roleName.hashCode());
        result = (result * PRIME) + (this.upsertRoleDetails == null ? 43 : this.upsertRoleDetails.hashCode());
        return result;
    }
}
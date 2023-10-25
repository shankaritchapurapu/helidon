package com.oracle.pic.vault.requests;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class GetConfigRequest extends com.oracle.bmc.requests.BmcRequest<java.lang.Void> {

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
     * The name of the config to take.
     */
    private String configName;

    

        /**
     * The name of the config to take.
     */
    public String getConfigName() {
        return configName;
    }
    

    public static class Builder implements com.oracle.bmc.requests.BmcRequest.Builder<GetConfigRequest, java.lang.Void> {
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
     * The name of the config to take.
     */
        private String configName = null;

        /**
         * The name of the config to take.
         * @param configName the value to set
         * @return this builder instance
         */
        public Builder configName(String configName) {
            this.configName = configName;
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
        public Builder copy(GetConfigRequest o) {
            xVaultToken(o.getXVaultToken());authPluginName(o.getAuthPluginName());configName(o.getConfigName());
            invocationCallback(o.getInvocationCallback());
            retryConfiguration(o.getRetryConfiguration());
            return this;
        }

        /**
         * Build the instance of GetConfigRequest as configured by this builder
         *
         * Note that this method takes calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#buildWithoutInvocationCallback} does not.
         *
         * This is the preferred method to build an instance.
         *
         * @return instance of GetConfigRequest
         */
        public GetConfigRequest build() {
            GetConfigRequest request = buildWithoutInvocationCallback();
            request.setInvocationCallback(invocationCallback);
            request.setRetryConfiguration(retryConfiguration);
            return request;
        }

        /**
         * Build the instance of GetConfigRequest as configured by this builder
         *
         * Note that this method does not take calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#build} does
         *
         * @return instance of GetConfigRequest
         */
        public GetConfigRequest buildWithoutInvocationCallback() {
            GetConfigRequest request = new GetConfigRequest();
            request.xVaultToken = xVaultToken;
            request.authPluginName = authPluginName;
            request.configName = configName;
            return request;
            // new GetConfigRequest(xVaultToken, authPluginName, configName);
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
            .configName(configName);
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
        sb.append(",configName=").append(String.valueOf(this.configName));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetConfigRequest)) {
            return false;
        }

        GetConfigRequest other = (GetConfigRequest) o;
        return super.equals(o)
            && java.util.Objects.equals(this.xVaultToken, other.xVaultToken)
            && java.util.Objects.equals(this.authPluginName, other.authPluginName)
            && java.util.Objects.equals(this.configName, other.configName);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.xVaultToken == null ? 43 : this.xVaultToken.hashCode());
        result = (result * PRIME) + (this.authPluginName == null ? 43 : this.authPluginName.hashCode());
        result = (result * PRIME) + (this.configName == null ? 43 : this.configName.hashCode());
        return result;
    }
}
package com.oracle.pic.vault.requests;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class LoginUsingIdentityRequest extends com.oracle.bmc.requests.BmcRequest<com.oracle.pic.vault.model.AuthenticateClientRequest> {

        /**
     * AuthenticateClient request
     */
    private com.oracle.pic.vault.model.AuthenticateClientRequest authenticateClientRequest;

    

        /**
     * AuthenticateClient request
     */
    public com.oracle.pic.vault.model.AuthenticateClientRequest getAuthenticateClientRequest() {
        return authenticateClientRequest;
    }
        /**
     * The sdk version.
     */
    private String sdkVersion;

    

        /**
     * The sdk version.
     */
    public String getSdkVersion() {
        return sdkVersion;
    }
    

    /**
     * Alternative accessor for the body parameter.
     * @return body parameter
     */
    @Override
    @com.oracle.bmc.InternalSdk
    public com.oracle.pic.vault.model.AuthenticateClientRequest getBody$() {
        return authenticateClientRequest;
    }

    public static class Builder implements com.oracle.bmc.requests.BmcRequest.Builder<LoginUsingIdentityRequest, com.oracle.pic.vault.model.AuthenticateClientRequest> {
        private com.oracle.bmc.http.client.RequestInterceptor invocationCallback = null;
        private com.oracle.bmc.retrier.RetryConfiguration retryConfiguration = null;

            /**
     * AuthenticateClient request
     */
        private com.oracle.pic.vault.model.AuthenticateClientRequest authenticateClientRequest = null;

        /**
         * AuthenticateClient request
         * @param authenticateClientRequest the value to set
         * @return this builder instance
         */
        public Builder authenticateClientRequest(com.oracle.pic.vault.model.AuthenticateClientRequest authenticateClientRequest) {
            this.authenticateClientRequest = authenticateClientRequest;
            return this;
        }

            /**
     * The sdk version.
     */
        private String sdkVersion = null;

        /**
         * The sdk version.
         * @param sdkVersion the value to set
         * @return this builder instance
         */
        public Builder sdkVersion(String sdkVersion) {
            this.sdkVersion = sdkVersion;
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
        public Builder copy(LoginUsingIdentityRequest o) {
            authenticateClientRequest(o.getAuthenticateClientRequest());sdkVersion(o.getSdkVersion());
            invocationCallback(o.getInvocationCallback());
            retryConfiguration(o.getRetryConfiguration());
            return this;
        }

        /**
         * Build the instance of LoginUsingIdentityRequest as configured by this builder
         *
         * Note that this method takes calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#buildWithoutInvocationCallback} does not.
         *
         * This is the preferred method to build an instance.
         *
         * @return instance of LoginUsingIdentityRequest
         */
        public LoginUsingIdentityRequest build() {
            LoginUsingIdentityRequest request = buildWithoutInvocationCallback();
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
        public Builder body$(com.oracle.pic.vault.model.AuthenticateClientRequest body) {
            authenticateClientRequest(body);
            return this;
        }

        /**
         * Build the instance of LoginUsingIdentityRequest as configured by this builder
         *
         * Note that this method does not take calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#build} does
         *
         * @return instance of LoginUsingIdentityRequest
         */
        public LoginUsingIdentityRequest buildWithoutInvocationCallback() {
            LoginUsingIdentityRequest request = new LoginUsingIdentityRequest();
            request.authenticateClientRequest = authenticateClientRequest;
            request.sdkVersion = sdkVersion;
            return request;
            // new LoginUsingIdentityRequest(authenticateClientRequest, sdkVersion);
        }
    }

    /**
     * Return an instance of {@link Builder} that allows you to modify request properties.
     * @return instance of {@link Builder} that allows you to modify request properties.
     */
    public Builder toBuilder() {
        return new Builder()
            .authenticateClientRequest(authenticateClientRequest)
            .sdkVersion(sdkVersion);
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
        sb.append(",authenticateClientRequest=").append(String.valueOf(this.authenticateClientRequest));
        sb.append(",sdkVersion=").append(String.valueOf(this.sdkVersion));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginUsingIdentityRequest)) {
            return false;
        }

        LoginUsingIdentityRequest other = (LoginUsingIdentityRequest) o;
        return super.equals(o)
            && java.util.Objects.equals(this.authenticateClientRequest, other.authenticateClientRequest)
            && java.util.Objects.equals(this.sdkVersion, other.sdkVersion);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.authenticateClientRequest == null ? 43 : this.authenticateClientRequest.hashCode());
        result = (result * PRIME) + (this.sdkVersion == null ? 43 : this.sdkVersion.hashCode());
        return result;
    }
}
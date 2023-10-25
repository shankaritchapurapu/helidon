package com.oracle.pic.vault.requests;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class GetPolicyRequest extends com.oracle.bmc.requests.BmcRequest<java.lang.Void> {

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
     * The name of the policy.
     */
    private String policyName;

    

        /**
     * The name of the policy.
     */
    public String getPolicyName() {
        return policyName;
    }
    

    public static class Builder implements com.oracle.bmc.requests.BmcRequest.Builder<GetPolicyRequest, java.lang.Void> {
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
     * The name of the policy.
     */
        private String policyName = null;

        /**
         * The name of the policy.
         * @param policyName the value to set
         * @return this builder instance
         */
        public Builder policyName(String policyName) {
            this.policyName = policyName;
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
        public Builder copy(GetPolicyRequest o) {
            xVaultToken(o.getXVaultToken());policyName(o.getPolicyName());
            invocationCallback(o.getInvocationCallback());
            retryConfiguration(o.getRetryConfiguration());
            return this;
        }

        /**
         * Build the instance of GetPolicyRequest as configured by this builder
         *
         * Note that this method takes calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#buildWithoutInvocationCallback} does not.
         *
         * This is the preferred method to build an instance.
         *
         * @return instance of GetPolicyRequest
         */
        public GetPolicyRequest build() {
            GetPolicyRequest request = buildWithoutInvocationCallback();
            request.setInvocationCallback(invocationCallback);
            request.setRetryConfiguration(retryConfiguration);
            return request;
        }

        /**
         * Build the instance of GetPolicyRequest as configured by this builder
         *
         * Note that this method does not take calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#build} does
         *
         * @return instance of GetPolicyRequest
         */
        public GetPolicyRequest buildWithoutInvocationCallback() {
            GetPolicyRequest request = new GetPolicyRequest();
            request.xVaultToken = xVaultToken;
            request.policyName = policyName;
            return request;
            // new GetPolicyRequest(xVaultToken, policyName);
        }
    }

    /**
     * Return an instance of {@link Builder} that allows you to modify request properties.
     * @return instance of {@link Builder} that allows you to modify request properties.
     */
    public Builder toBuilder() {
        return new Builder()
            .xVaultToken(xVaultToken)
            .policyName(policyName);
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
        sb.append(",policyName=").append(String.valueOf(this.policyName));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetPolicyRequest)) {
            return false;
        }

        GetPolicyRequest other = (GetPolicyRequest) o;
        return super.equals(o)
            && java.util.Objects.equals(this.xVaultToken, other.xVaultToken)
            && java.util.Objects.equals(this.policyName, other.policyName);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.xVaultToken == null ? 43 : this.xVaultToken.hashCode());
        result = (result * PRIME) + (this.policyName == null ? 43 : this.policyName.hashCode());
        return result;
    }
}
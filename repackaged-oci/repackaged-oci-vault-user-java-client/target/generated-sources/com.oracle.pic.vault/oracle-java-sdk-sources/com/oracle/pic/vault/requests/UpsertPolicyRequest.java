package com.oracle.pic.vault.requests;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class UpsertPolicyRequest extends com.oracle.bmc.requests.BmcRequest<String> {

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
    
    private String body;

    

    
    public String getBody() {
        return body;
    }
    

    /**
     * Alternative accessor for the body parameter.
     * @return body parameter
     */
    @Override
    @com.oracle.bmc.InternalSdk
    public String getBody$() {
        return body;
    }

    public static class Builder implements com.oracle.bmc.requests.BmcRequest.Builder<UpsertPolicyRequest, String> {
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

        
        private String body = null;

        /**
         * 
         * @param body the value to set
         * @return this builder instance
         */
        public Builder body(String body) {
            this.body = body;
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
        public Builder copy(UpsertPolicyRequest o) {
            xVaultToken(o.getXVaultToken());policyName(o.getPolicyName());body(o.getBody());
            invocationCallback(o.getInvocationCallback());
            retryConfiguration(o.getRetryConfiguration());
            return this;
        }

        /**
         * Build the instance of UpsertPolicyRequest as configured by this builder
         *
         * Note that this method takes calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#buildWithoutInvocationCallback} does not.
         *
         * This is the preferred method to build an instance.
         *
         * @return instance of UpsertPolicyRequest
         */
        public UpsertPolicyRequest build() {
            UpsertPolicyRequest request = buildWithoutInvocationCallback();
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
        public Builder body$(String body) {
            body(body);
            return this;
        }

        /**
         * Build the instance of UpsertPolicyRequest as configured by this builder
         *
         * Note that this method does not take calls to {@link Builder#invocationCallback(com.oracle.bmc.http.client.RequestInterceptor)} into account,
         * while the method {@link Builder#build} does
         *
         * @return instance of UpsertPolicyRequest
         */
        public UpsertPolicyRequest buildWithoutInvocationCallback() {
            UpsertPolicyRequest request = new UpsertPolicyRequest();
            request.xVaultToken = xVaultToken;
            request.policyName = policyName;
            request.body = body;
            return request;
            // new UpsertPolicyRequest(xVaultToken, policyName, body);
        }
    }

    /**
     * Return an instance of {@link Builder} that allows you to modify request properties.
     * @return instance of {@link Builder} that allows you to modify request properties.
     */
    public Builder toBuilder() {
        return new Builder()
            .xVaultToken(xVaultToken)
            .policyName(policyName)
            .body(body);
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
        sb.append(",body=").append(String.valueOf(this.body));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpsertPolicyRequest)) {
            return false;
        }

        UpsertPolicyRequest other = (UpsertPolicyRequest) o;
        return super.equals(o)
            && java.util.Objects.equals(this.xVaultToken, other.xVaultToken)
            && java.util.Objects.equals(this.policyName, other.policyName)
            && java.util.Objects.equals(this.body, other.body);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.xVaultToken == null ? 43 : this.xVaultToken.hashCode());
        result = (result * PRIME) + (this.policyName == null ? 43 : this.policyName.hashCode());
        result = (result * PRIME) + (this.body == null ? 43 : this.body.hashCode());
        return result;
    }
}
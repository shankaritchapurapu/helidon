package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class GetPolicyResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code PolicyDetails} instance.
     */
    private com.oracle.pic.vault.model.PolicyDetails policyDetails;

    /**
     * The returned {@code PolicyDetails} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.PolicyDetails getPolicyDetails() {
        return policyDetails;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "policyDetails"})
    private GetPolicyResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.PolicyDetails policyDetails) {
        super(__httpStatusCode__, headers);
        this.policyDetails = policyDetails;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<GetPolicyResponse> {
        private int __httpStatusCode__;

        @Override
        public Builder __httpStatusCode__(int __httpStatusCode__) {
            this.__httpStatusCode__ = __httpStatusCode__;
            return this;
        }

        private java.util.Map<String, java.util.List<String>> headers;

        @Override
        public Builder headers(java.util.Map<String, java.util.List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * The returned {@code PolicyDetails} instance.
         */
        private com.oracle.pic.vault.model.PolicyDetails policyDetails;

        /**
         * The returned {@code PolicyDetails} instance.
         * @param policyDetails the value to set
         * @return this builder
         */
        public Builder policyDetails(com.oracle.pic.vault.model.PolicyDetails policyDetails) {
            this.policyDetails = policyDetails;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(GetPolicyResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            policyDetails(o.getPolicyDetails());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public GetPolicyResponse build() {
            return new GetPolicyResponse(__httpStatusCode__, headers, policyDetails);
        }
    }

    /**
     * Return a new builder for this response object.
     * @return builder for the response object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append("(");
        sb.append("super=").append(super.toString());
        sb.append(",policyDetails=").append(String.valueOf(policyDetails));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetPolicyResponse)) {
            return false;
        }

        GetPolicyResponse other = (GetPolicyResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.policyDetails, other.policyDetails);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.policyDetails == null ? 43 : this.policyDetails.hashCode());
        return result;
    }
}
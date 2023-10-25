package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class ListPoliciesResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code PolicyListDetails} instance.
     */
    private com.oracle.pic.vault.model.PolicyListDetails policyListDetails;

    /**
     * The returned {@code PolicyListDetails} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.PolicyListDetails getPolicyListDetails() {
        return policyListDetails;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "policyListDetails"})
    private ListPoliciesResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.PolicyListDetails policyListDetails) {
        super(__httpStatusCode__, headers);
        this.policyListDetails = policyListDetails;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<ListPoliciesResponse> {
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
         * The returned {@code PolicyListDetails} instance.
         */
        private com.oracle.pic.vault.model.PolicyListDetails policyListDetails;

        /**
         * The returned {@code PolicyListDetails} instance.
         * @param policyListDetails the value to set
         * @return this builder
         */
        public Builder policyListDetails(com.oracle.pic.vault.model.PolicyListDetails policyListDetails) {
            this.policyListDetails = policyListDetails;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(ListPoliciesResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            policyListDetails(o.getPolicyListDetails());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public ListPoliciesResponse build() {
            return new ListPoliciesResponse(__httpStatusCode__, headers, policyListDetails);
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
        sb.append(",policyListDetails=").append(String.valueOf(policyListDetails));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListPoliciesResponse)) {
            return false;
        }

        ListPoliciesResponse other = (ListPoliciesResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.policyListDetails, other.policyListDetails);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.policyListDetails == null ? 43 : this.policyListDetails.hashCode());
        return result;
    }
}
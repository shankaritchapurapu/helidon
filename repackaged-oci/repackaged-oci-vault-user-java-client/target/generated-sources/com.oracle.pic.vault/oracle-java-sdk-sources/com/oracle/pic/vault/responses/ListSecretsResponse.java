package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class ListSecretsResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code ListSecretsResponse} instance.
     */
    private com.oracle.pic.vault.model.ListSecretsResponse listSecretsResponse;

    /**
     * The returned {@code ListSecretsResponse} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.ListSecretsResponse getListSecretsResponse() {
        return listSecretsResponse;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "listSecretsResponse"})
    private ListSecretsResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.ListSecretsResponse listSecretsResponse) {
        super(__httpStatusCode__, headers);
        this.listSecretsResponse = listSecretsResponse;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<ListSecretsResponse> {
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
         * The returned {@code ListSecretsResponse} instance.
         */
        private com.oracle.pic.vault.model.ListSecretsResponse listSecretsResponse;

        /**
         * The returned {@code ListSecretsResponse} instance.
         * @param listSecretsResponse the value to set
         * @return this builder
         */
        public Builder listSecretsResponse(com.oracle.pic.vault.model.ListSecretsResponse listSecretsResponse) {
            this.listSecretsResponse = listSecretsResponse;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(ListSecretsResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            listSecretsResponse(o.getListSecretsResponse());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public ListSecretsResponse build() {
            return new ListSecretsResponse(__httpStatusCode__, headers, listSecretsResponse);
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
        sb.append(",listSecretsResponse=").append(String.valueOf(listSecretsResponse));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListSecretsResponse)) {
            return false;
        }

        ListSecretsResponse other = (ListSecretsResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.listSecretsResponse, other.listSecretsResponse);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.listSecretsResponse == null ? 43 : this.listSecretsResponse.hashCode());
        return result;
    }
}
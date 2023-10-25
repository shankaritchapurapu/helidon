package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class GetSecretResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code GetSecretResponse} instance.
     */
    private com.oracle.pic.vault.model.GetSecretResponse getSecretResponse;

    /**
     * The returned {@code GetSecretResponse} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.GetSecretResponse getGetSecretResponse() {
        return getSecretResponse;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "getSecretResponse"})
    private GetSecretResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.GetSecretResponse getSecretResponse) {
        super(__httpStatusCode__, headers);
        this.getSecretResponse = getSecretResponse;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<GetSecretResponse> {
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
         * The returned {@code GetSecretResponse} instance.
         */
        private com.oracle.pic.vault.model.GetSecretResponse getSecretResponse;

        /**
         * The returned {@code GetSecretResponse} instance.
         * @param getSecretResponse the value to set
         * @return this builder
         */
        public Builder getSecretResponse(com.oracle.pic.vault.model.GetSecretResponse getSecretResponse) {
            this.getSecretResponse = getSecretResponse;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(GetSecretResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            getSecretResponse(o.getGetSecretResponse());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public GetSecretResponse build() {
            return new GetSecretResponse(__httpStatusCode__, headers, getSecretResponse);
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
        sb.append(",getSecretResponse=").append(String.valueOf(getSecretResponse));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetSecretResponse)) {
            return false;
        }

        GetSecretResponse other = (GetSecretResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.getSecretResponse, other.getSecretResponse);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.getSecretResponse == null ? 43 : this.getSecretResponse.hashCode());
        return result;
    }
}
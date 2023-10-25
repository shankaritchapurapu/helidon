package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class CreateConfigResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code ConfigDetails} instance.
     */
    private com.oracle.pic.vault.model.ConfigDetails configDetails;

    /**
     * The returned {@code ConfigDetails} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.ConfigDetails getConfigDetails() {
        return configDetails;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "configDetails"})
    private CreateConfigResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.ConfigDetails configDetails) {
        super(__httpStatusCode__, headers);
        this.configDetails = configDetails;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<CreateConfigResponse> {
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
         * The returned {@code ConfigDetails} instance.
         */
        private com.oracle.pic.vault.model.ConfigDetails configDetails;

        /**
         * The returned {@code ConfigDetails} instance.
         * @param configDetails the value to set
         * @return this builder
         */
        public Builder configDetails(com.oracle.pic.vault.model.ConfigDetails configDetails) {
            this.configDetails = configDetails;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(CreateConfigResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            configDetails(o.getConfigDetails());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public CreateConfigResponse build() {
            return new CreateConfigResponse(__httpStatusCode__, headers, configDetails);
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
        sb.append(",configDetails=").append(String.valueOf(configDetails));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateConfigResponse)) {
            return false;
        }

        CreateConfigResponse other = (CreateConfigResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.configDetails, other.configDetails);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.configDetails == null ? 43 : this.configDetails.hashCode());
        return result;
    }
}
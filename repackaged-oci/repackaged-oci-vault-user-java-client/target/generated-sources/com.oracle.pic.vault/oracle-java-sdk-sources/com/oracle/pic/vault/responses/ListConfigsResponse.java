package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class ListConfigsResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code ListDetails} instance.
     */
    private com.oracle.pic.vault.model.ListDetails listDetails;

    /**
     * The returned {@code ListDetails} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.ListDetails getListDetails() {
        return listDetails;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "listDetails"})
    private ListConfigsResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.ListDetails listDetails) {
        super(__httpStatusCode__, headers);
        this.listDetails = listDetails;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<ListConfigsResponse> {
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
         * The returned {@code ListDetails} instance.
         */
        private com.oracle.pic.vault.model.ListDetails listDetails;

        /**
         * The returned {@code ListDetails} instance.
         * @param listDetails the value to set
         * @return this builder
         */
        public Builder listDetails(com.oracle.pic.vault.model.ListDetails listDetails) {
            this.listDetails = listDetails;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(ListConfigsResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            listDetails(o.getListDetails());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public ListConfigsResponse build() {
            return new ListConfigsResponse(__httpStatusCode__, headers, listDetails);
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
        sb.append(",listDetails=").append(String.valueOf(listDetails));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListConfigsResponse)) {
            return false;
        }

        ListConfigsResponse other = (ListConfigsResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.listDetails, other.listDetails);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.listDetails == null ? 43 : this.listDetails.hashCode());
        return result;
    }
}
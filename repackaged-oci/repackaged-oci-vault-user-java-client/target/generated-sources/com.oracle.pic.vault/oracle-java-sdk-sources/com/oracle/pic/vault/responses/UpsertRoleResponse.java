package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class UpsertRoleResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code RoleDetails} instance.
     */
    private com.oracle.pic.vault.model.RoleDetails roleDetails;

    /**
     * The returned {@code RoleDetails} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.RoleDetails getRoleDetails() {
        return roleDetails;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "roleDetails"})
    private UpsertRoleResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.RoleDetails roleDetails) {
        super(__httpStatusCode__, headers);
        this.roleDetails = roleDetails;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<UpsertRoleResponse> {
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
         * The returned {@code RoleDetails} instance.
         */
        private com.oracle.pic.vault.model.RoleDetails roleDetails;

        /**
         * The returned {@code RoleDetails} instance.
         * @param roleDetails the value to set
         * @return this builder
         */
        public Builder roleDetails(com.oracle.pic.vault.model.RoleDetails roleDetails) {
            this.roleDetails = roleDetails;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(UpsertRoleResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            roleDetails(o.getRoleDetails());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public UpsertRoleResponse build() {
            return new UpsertRoleResponse(__httpStatusCode__, headers, roleDetails);
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
        sb.append(",roleDetails=").append(String.valueOf(roleDetails));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpsertRoleResponse)) {
            return false;
        }

        UpsertRoleResponse other = (UpsertRoleResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.roleDetails, other.roleDetails);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.roleDetails == null ? 43 : this.roleDetails.hashCode());
        return result;
    }
}
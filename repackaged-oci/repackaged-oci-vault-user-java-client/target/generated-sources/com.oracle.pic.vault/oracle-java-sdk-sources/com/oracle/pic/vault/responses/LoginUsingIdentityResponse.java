package com.oracle.pic.vault.responses;

import com.oracle.pic.vault.model.*;

@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
public class LoginUsingIdentityResponse extends com.oracle.bmc.responses.BmcResponse {
    /**
     * The returned {@code LoginResponse} instance.
     */
    private com.oracle.pic.vault.model.LoginResponse loginResponse;

    /**
     * The returned {@code LoginResponse} instance.
     * @return the value
     */
    public com.oracle.pic.vault.model.LoginResponse getLoginResponse() {
        return loginResponse;
    }


    @java.beans.ConstructorProperties({"__httpStatusCode__", "headers", "loginResponse"})
    private LoginUsingIdentityResponse(int __httpStatusCode__,  java.util.Map<String, java.util.List<String>> headers, com.oracle.pic.vault.model.LoginResponse loginResponse) {
        super(__httpStatusCode__, headers);
        this.loginResponse = loginResponse;

    }

    public static class Builder implements com.oracle.bmc.responses.BmcResponse.Builder<LoginUsingIdentityResponse> {
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
         * The returned {@code LoginResponse} instance.
         */
        private com.oracle.pic.vault.model.LoginResponse loginResponse;

        /**
         * The returned {@code LoginResponse} instance.
         * @param loginResponse the value to set
         * @return this builder
         */
        public Builder loginResponse(com.oracle.pic.vault.model.LoginResponse loginResponse) {
            this.loginResponse = loginResponse;
            return this;
        }

        /**
         * Copy method to populate the builder with values from the given instance.
         * @return this builder instance
         */
        @Override
        public Builder copy(LoginUsingIdentityResponse o) {
            __httpStatusCode__(o.get__httpStatusCode__());
            headers(o.getHeaders());
            
            loginResponse(o.getLoginResponse());
            
            return this;
        }

        /**
         * Build the response object.
         * @return the response object
         */
        @Override
        public LoginUsingIdentityResponse build() {
            return new LoginUsingIdentityResponse(__httpStatusCode__, headers, loginResponse);
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
        sb.append(",loginResponse=").append(String.valueOf(loginResponse));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginUsingIdentityResponse)) {
            return false;
        }

        LoginUsingIdentityResponse other = (LoginUsingIdentityResponse) o;
        return super.equals(o)
            && java.util.Objects.equals(this.loginResponse, other.loginResponse);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = (result * PRIME) + (this.loginResponse == null ? 43 : this.loginResponse.hashCode());
        return result;
    }
}
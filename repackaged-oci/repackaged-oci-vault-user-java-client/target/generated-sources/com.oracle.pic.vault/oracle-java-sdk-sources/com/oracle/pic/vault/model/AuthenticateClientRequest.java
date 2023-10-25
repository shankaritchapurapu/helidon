package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=AuthenticateClientRequest.Builder.class)

public final class AuthenticateClientRequest  {
    @Deprecated
    @java.beans.ConstructorProperties({"requestHeaders"})
    public AuthenticateClientRequest(java.util.Map<String, java.util.List<String>> requestHeaders) {
        super();
        this.requestHeaders = requestHeaders;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
            /**
     * The signed headers of the client
     **/
    
        @com.fasterxml.jackson.annotation.JsonProperty("requestHeaders")
        private java.util.Map<String, java.util.List<String>> requestHeaders;

                /**
         * The signed headers of the client
         * @param requestHeaders the value to set
         * @return this builder
         **/
        

        public Builder requestHeaders(java.util.Map<String, java.util.List<String>> requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }


        public AuthenticateClientRequest build() {
            AuthenticateClientRequest model = new AuthenticateClientRequest(this.requestHeaders);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(AuthenticateClientRequest model) {
            this.requestHeaders(model.getRequestHeaders());
            return this;
        }
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }


    public Builder toBuilder() {
        return new Builder().copy(this);
    }

    


        /**
     * The signed headers of the client
     **/
    
    @com.fasterxml.jackson.annotation.JsonProperty("requestHeaders")
    private final java.util.Map<String, java.util.List<String>> requestHeaders;

        /**
     * The signed headers of the client
     * @return the value
     **/
    
    public java.util.Map<String, java.util.List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public String toString() {
        return this.toString(true);
    }

    /**
     * Return a string representation of the object.
     * @param includeByteArrayContents true to include the full contents of byte arrays
     * @return string representation
     */
    public String toString(boolean includeByteArrayContents) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append("AuthenticateClientRequest(");
        sb.append("requestHeaders=").append(String.valueOf(this.requestHeaders));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthenticateClientRequest)) {
            return false;
        }

        AuthenticateClientRequest other = (AuthenticateClientRequest) o;
        return java.util.Objects.equals(this.requestHeaders, other.requestHeaders);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.requestHeaders == null ? 43 : this.requestHeaders.hashCode());
        return result;
    }


}

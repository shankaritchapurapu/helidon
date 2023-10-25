package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=LoginResponse.Builder.class)

public final class LoginResponse  {
    @Deprecated
    @java.beans.ConstructorProperties({"requestId", "auth"})
    public LoginResponse(String requestId, LoginAuthDetails auth) {
        super();
        this.requestId = requestId;
        this.auth = auth;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("request_id")
        private String requestId;

        

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("auth")
        private LoginAuthDetails auth;

        

        public Builder auth(LoginAuthDetails auth) {
            this.auth = auth;
            return this;
        }


        public LoginResponse build() {
            LoginResponse model = new LoginResponse(this.requestId
                , this.auth);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(LoginResponse model) {
            this.requestId(model.getRequestId());
            this.auth(model.getAuth());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("request_id")
    private final String requestId;

    
    public String getRequestId() {
        return requestId;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("auth")
    private final LoginAuthDetails auth;

    
    public LoginAuthDetails getAuth() {
        return auth;
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
        sb.append("LoginResponse(");
        sb.append("requestId=").append(String.valueOf(this.requestId));
        sb.append(", auth=").append(String.valueOf(this.auth));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginResponse)) {
            return false;
        }

        LoginResponse other = (LoginResponse) o;
        return java.util.Objects.equals(this.requestId, other.requestId) &&
            java.util.Objects.equals(this.auth, other.auth);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.requestId == null ? 43 : this.requestId.hashCode());
        result = (result * PRIME) + (this.auth == null ? 43 : this.auth.hashCode());
        return result;
    }


}

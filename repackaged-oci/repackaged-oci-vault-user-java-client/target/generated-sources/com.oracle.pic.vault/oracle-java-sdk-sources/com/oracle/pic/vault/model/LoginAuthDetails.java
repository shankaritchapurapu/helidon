package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=LoginAuthDetails.Builder.class)

public final class LoginAuthDetails  {
    @Deprecated
    @java.beans.ConstructorProperties({"clientToken", "policies", "metadata", "leaseDuration", "renewable"})
    public LoginAuthDetails(String clientToken, java.util.List<String> policies, java.util.Map<String, String> metadata, Integer leaseDuration, Boolean renewable) {
        super();
        this.clientToken = clientToken;
        this.policies = policies;
        this.metadata = metadata;
        this.leaseDuration = leaseDuration;
        this.renewable = renewable;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("client_token")
        private String clientToken;

        

        public Builder clientToken(String clientToken) {
            this.clientToken = clientToken;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("policies")
        private java.util.List<String> policies;

        

        public Builder policies(java.util.List<String> policies) {
            this.policies = policies;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("metadata")
        private java.util.Map<String, String> metadata;

        

        public Builder metadata(java.util.Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("lease_duration")
        private Integer leaseDuration;

        

        public Builder leaseDuration(Integer leaseDuration) {
            this.leaseDuration = leaseDuration;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("renewable")
        private Boolean renewable;

        

        public Builder renewable(Boolean renewable) {
            this.renewable = renewable;
            return this;
        }


        public LoginAuthDetails build() {
            LoginAuthDetails model = new LoginAuthDetails(this.clientToken
                , this.policies
                , this.metadata
                , this.leaseDuration
                , this.renewable);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(LoginAuthDetails model) {
            this.clientToken(model.getClientToken());
            this.policies(model.getPolicies());
            this.metadata(model.getMetadata());
            this.leaseDuration(model.getLeaseDuration());
            this.renewable(model.getRenewable());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("client_token")
    private final String clientToken;

    
    public String getClientToken() {
        return clientToken;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("policies")
    private final java.util.List<String> policies;

    
    public java.util.List<String> getPolicies() {
        return policies;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("metadata")
    private final java.util.Map<String, String> metadata;

    
    public java.util.Map<String, String> getMetadata() {
        return metadata;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("lease_duration")
    private final Integer leaseDuration;

    
    public Integer getLeaseDuration() {
        return leaseDuration;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("renewable")
    private final Boolean renewable;

    
    public Boolean getRenewable() {
        return renewable;
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
        sb.append("LoginAuthDetails(");
        sb.append("clientToken=").append(String.valueOf(this.clientToken));
        sb.append(", policies=").append(String.valueOf(this.policies));
        sb.append(", metadata=").append(String.valueOf(this.metadata));
        sb.append(", leaseDuration=").append(String.valueOf(this.leaseDuration));
        sb.append(", renewable=").append(String.valueOf(this.renewable));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginAuthDetails)) {
            return false;
        }

        LoginAuthDetails other = (LoginAuthDetails) o;
        return java.util.Objects.equals(this.clientToken, other.clientToken) &&
            java.util.Objects.equals(this.policies, other.policies) &&
            java.util.Objects.equals(this.metadata, other.metadata) &&
            java.util.Objects.equals(this.leaseDuration, other.leaseDuration) &&
            java.util.Objects.equals(this.renewable, other.renewable);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.clientToken == null ? 43 : this.clientToken.hashCode());
        result = (result * PRIME) + (this.policies == null ? 43 : this.policies.hashCode());
        result = (result * PRIME) + (this.metadata == null ? 43 : this.metadata.hashCode());
        result = (result * PRIME) + (this.leaseDuration == null ? 43 : this.leaseDuration.hashCode());
        result = (result * PRIME) + (this.renewable == null ? 43 : this.renewable.hashCode());
        return result;
    }


}

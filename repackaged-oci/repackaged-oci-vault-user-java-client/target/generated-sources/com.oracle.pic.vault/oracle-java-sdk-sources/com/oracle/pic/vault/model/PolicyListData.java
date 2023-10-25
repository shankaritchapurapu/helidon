package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=PolicyListData.Builder.class)

public final class PolicyListData  {
    @Deprecated
    @java.beans.ConstructorProperties({"keys", "policies"})
    public PolicyListData(java.util.List<String> keys, java.util.List<String> policies) {
        super();
        this.keys = keys;
        this.policies = policies;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("keys")
        private java.util.List<String> keys;

        

        public Builder keys(java.util.List<String> keys) {
            this.keys = keys;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("policies")
        private java.util.List<String> policies;

        

        public Builder policies(java.util.List<String> policies) {
            this.policies = policies;
            return this;
        }


        public PolicyListData build() {
            PolicyListData model = new PolicyListData(this.keys
                , this.policies);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(PolicyListData model) {
            this.keys(model.getKeys());
            this.policies(model.getPolicies());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("keys")
    private final java.util.List<String> keys;

    
    public java.util.List<String> getKeys() {
        return keys;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("policies")
    private final java.util.List<String> policies;

    
    public java.util.List<String> getPolicies() {
        return policies;
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
        sb.append("PolicyListData(");
        sb.append("keys=").append(String.valueOf(this.keys));
        sb.append(", policies=").append(String.valueOf(this.policies));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyListData)) {
            return false;
        }

        PolicyListData other = (PolicyListData) o;
        return java.util.Objects.equals(this.keys, other.keys) &&
            java.util.Objects.equals(this.policies, other.policies);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.keys == null ? 43 : this.keys.hashCode());
        result = (result * PRIME) + (this.policies == null ? 43 : this.policies.hashCode());
        return result;
    }


}

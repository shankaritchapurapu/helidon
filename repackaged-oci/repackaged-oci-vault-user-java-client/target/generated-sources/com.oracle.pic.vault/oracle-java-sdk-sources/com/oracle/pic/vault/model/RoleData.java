package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=RoleData.Builder.class)

public final class RoleData  {
    @Deprecated
    @java.beans.ConstructorProperties({"description", "policyList", "role", "ttl", "version"})
    public RoleData(String description, java.util.List<String> policyList, String role, Integer ttl, Integer version) {
        super();
        this.description = description;
        this.policyList = policyList;
        this.role = role;
        this.ttl = ttl;
        this.version = version;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("description")
        private String description;

        

        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("policy_list")
        private java.util.List<String> policyList;

        

        public Builder policyList(java.util.List<String> policyList) {
            this.policyList = policyList;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("role")
        private String role;

        

        public Builder role(String role) {
            this.role = role;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("ttl")
        private Integer ttl;

        

        public Builder ttl(Integer ttl) {
            this.ttl = ttl;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("version")
        private Integer version;

        

        public Builder version(Integer version) {
            this.version = version;
            return this;
        }


        public RoleData build() {
            RoleData model = new RoleData(this.description
                , this.policyList
                , this.role
                , this.ttl
                , this.version);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(RoleData model) {
            this.description(model.getDescription());
            this.policyList(model.getPolicyList());
            this.role(model.getRole());
            this.ttl(model.getTtl());
            this.version(model.getVersion());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("description")
    private final String description;

    
    public String getDescription() {
        return description;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("policy_list")
    private final java.util.List<String> policyList;

    
    public java.util.List<String> getPolicyList() {
        return policyList;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("role")
    private final String role;

    
    public String getRole() {
        return role;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("ttl")
    private final Integer ttl;

    
    public Integer getTtl() {
        return ttl;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("version")
    private final Integer version;

    
    public Integer getVersion() {
        return version;
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
        sb.append("RoleData(");
        sb.append("description=").append(String.valueOf(this.description));
        sb.append(", policyList=").append(String.valueOf(this.policyList));
        sb.append(", role=").append(String.valueOf(this.role));
        sb.append(", ttl=").append(String.valueOf(this.ttl));
        sb.append(", version=").append(String.valueOf(this.version));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleData)) {
            return false;
        }

        RoleData other = (RoleData) o;
        return java.util.Objects.equals(this.description, other.description) &&
            java.util.Objects.equals(this.policyList, other.policyList) &&
            java.util.Objects.equals(this.role, other.role) &&
            java.util.Objects.equals(this.ttl, other.ttl) &&
            java.util.Objects.equals(this.version, other.version);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.description == null ? 43 : this.description.hashCode());
        result = (result * PRIME) + (this.policyList == null ? 43 : this.policyList.hashCode());
        result = (result * PRIME) + (this.role == null ? 43 : this.role.hashCode());
        result = (result * PRIME) + (this.ttl == null ? 43 : this.ttl.hashCode());
        result = (result * PRIME) + (this.version == null ? 43 : this.version.hashCode());
        return result;
    }


}

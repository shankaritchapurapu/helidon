package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=UpsertRoleDetails.Builder.class)

public final class UpsertRoleDetails  {
    @Deprecated
    @java.beans.ConstructorProperties({"description", "ttl", "addPolicyList", "removePolicyList"})
    public UpsertRoleDetails(String description, Integer ttl, String addPolicyList, String removePolicyList) {
        super();
        this.description = description;
        this.ttl = ttl;
        this.addPolicyList = addPolicyList;
        this.removePolicyList = removePolicyList;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("description")
        private String description;

        

        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("ttl")
        private Integer ttl;

        

        public Builder ttl(Integer ttl) {
            this.ttl = ttl;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("add_policy_list")
        private String addPolicyList;

        

        public Builder addPolicyList(String addPolicyList) {
            this.addPolicyList = addPolicyList;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("remove_policy_list")
        private String removePolicyList;

        

        public Builder removePolicyList(String removePolicyList) {
            this.removePolicyList = removePolicyList;
            return this;
        }


        public UpsertRoleDetails build() {
            UpsertRoleDetails model = new UpsertRoleDetails(this.description
                , this.ttl
                , this.addPolicyList
                , this.removePolicyList);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(UpsertRoleDetails model) {
            this.description(model.getDescription());
            this.ttl(model.getTtl());
            this.addPolicyList(model.getAddPolicyList());
            this.removePolicyList(model.getRemovePolicyList());
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


    
    @com.fasterxml.jackson.annotation.JsonProperty("ttl")
    private final Integer ttl;

    
    public Integer getTtl() {
        return ttl;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("add_policy_list")
    private final String addPolicyList;

    
    public String getAddPolicyList() {
        return addPolicyList;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("remove_policy_list")
    private final String removePolicyList;

    
    public String getRemovePolicyList() {
        return removePolicyList;
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
        sb.append("UpsertRoleDetails(");
        sb.append("description=").append(String.valueOf(this.description));
        sb.append(", ttl=").append(String.valueOf(this.ttl));
        sb.append(", addPolicyList=").append(String.valueOf(this.addPolicyList));
        sb.append(", removePolicyList=").append(String.valueOf(this.removePolicyList));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpsertRoleDetails)) {
            return false;
        }

        UpsertRoleDetails other = (UpsertRoleDetails) o;
        return java.util.Objects.equals(this.description, other.description) &&
            java.util.Objects.equals(this.ttl, other.ttl) &&
            java.util.Objects.equals(this.addPolicyList, other.addPolicyList) &&
            java.util.Objects.equals(this.removePolicyList, other.removePolicyList);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.description == null ? 43 : this.description.hashCode());
        result = (result * PRIME) + (this.ttl == null ? 43 : this.ttl.hashCode());
        result = (result * PRIME) + (this.addPolicyList == null ? 43 : this.addPolicyList.hashCode());
        result = (result * PRIME) + (this.removePolicyList == null ? 43 : this.removePolicyList.hashCode());
        return result;
    }


}

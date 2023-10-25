package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=PolicyData.Builder.class)

public final class PolicyData  {
    @Deprecated
    @java.beans.ConstructorProperties({"name", "rules"})
    public PolicyData(String name, String rules) {
        super();
        this.name = name;
        this.rules = rules;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private String name;

        

        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("rules")
        private String rules;

        

        public Builder rules(String rules) {
            this.rules = rules;
            return this;
        }


        public PolicyData build() {
            PolicyData model = new PolicyData(this.name
                , this.rules);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(PolicyData model) {
            this.name(model.getName());
            this.rules(model.getRules());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("name")
    private final String name;

    
    public String getName() {
        return name;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("rules")
    private final String rules;

    
    public String getRules() {
        return rules;
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
        sb.append("PolicyData(");
        sb.append("name=").append(String.valueOf(this.name));
        sb.append(", rules=").append(String.valueOf(this.rules));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyData)) {
            return false;
        }

        PolicyData other = (PolicyData) o;
        return java.util.Objects.equals(this.name, other.name) &&
            java.util.Objects.equals(this.rules, other.rules);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.name == null ? 43 : this.name.hashCode());
        result = (result * PRIME) + (this.rules == null ? 43 : this.rules.hashCode());
        return result;
    }


}

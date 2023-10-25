package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=CreateConfigDetails.Builder.class)

public final class CreateConfigDetails  {
    @Deprecated
    @java.beans.ConstructorProperties({"configValue"})
    public CreateConfigDetails(String configValue) {
        super();
        this.configValue = configValue;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("configValue")
        private String configValue;

        

        public Builder configValue(String configValue) {
            this.configValue = configValue;
            return this;
        }


        public CreateConfigDetails build() {
            CreateConfigDetails model = new CreateConfigDetails(this.configValue);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(CreateConfigDetails model) {
            this.configValue(model.getConfigValue());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("configValue")
    private final String configValue;

    
    public String getConfigValue() {
        return configValue;
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
        sb.append("CreateConfigDetails(");
        sb.append("configValue=").append(String.valueOf(this.configValue));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateConfigDetails)) {
            return false;
        }

        CreateConfigDetails other = (CreateConfigDetails) o;
        return java.util.Objects.equals(this.configValue, other.configValue);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.configValue == null ? 43 : this.configValue.hashCode());
        return result;
    }


}

package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=ConfigData.Builder.class)

public final class ConfigData  {
    @Deprecated
    @java.beans.ConstructorProperties({"configName", "configValue", "etag"})
    public ConfigData(String configName, String configValue, String etag) {
        super();
        this.configName = configName;
        this.configValue = configValue;
        this.etag = etag;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("configName")
        private String configName;

        

        public Builder configName(String configName) {
            this.configName = configName;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("configValue")
        private String configValue;

        

        public Builder configValue(String configValue) {
            this.configValue = configValue;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("etag")
        private String etag;

        

        public Builder etag(String etag) {
            this.etag = etag;
            return this;
        }


        public ConfigData build() {
            ConfigData model = new ConfigData(this.configName
                , this.configValue
                , this.etag);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(ConfigData model) {
            this.configName(model.getConfigName());
            this.configValue(model.getConfigValue());
            this.etag(model.getEtag());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("configName")
    private final String configName;

    
    public String getConfigName() {
        return configName;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("configValue")
    private final String configValue;

    
    public String getConfigValue() {
        return configValue;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("etag")
    private final String etag;

    
    public String getEtag() {
        return etag;
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
        sb.append("ConfigData(");
        sb.append("configName=").append(String.valueOf(this.configName));
        sb.append(", configValue=").append(String.valueOf(this.configValue));
        sb.append(", etag=").append(String.valueOf(this.etag));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigData)) {
            return false;
        }

        ConfigData other = (ConfigData) o;
        return java.util.Objects.equals(this.configName, other.configName) &&
            java.util.Objects.equals(this.configValue, other.configValue) &&
            java.util.Objects.equals(this.etag, other.etag);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.configName == null ? 43 : this.configName.hashCode());
        result = (result * PRIME) + (this.configValue == null ? 43 : this.configValue.hashCode());
        result = (result * PRIME) + (this.etag == null ? 43 : this.etag.hashCode());
        return result;
    }


}

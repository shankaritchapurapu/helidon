package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=ListData.Builder.class)

public final class ListData  {
    @Deprecated
    @java.beans.ConstructorProperties({"keys"})
    public ListData(java.util.List<String> keys) {
        super();
        this.keys = keys;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("keys")
        private java.util.List<String> keys;

        

        public Builder keys(java.util.List<String> keys) {
            this.keys = keys;
            return this;
        }


        public ListData build() {
            ListData model = new ListData(this.keys);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(ListData model) {
            this.keys(model.getKeys());
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
        sb.append("ListData(");
        sb.append("keys=").append(String.valueOf(this.keys));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListData)) {
            return false;
        }

        ListData other = (ListData) o;
        return java.util.Objects.equals(this.keys, other.keys);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.keys == null ? 43 : this.keys.hashCode());
        return result;
    }


}

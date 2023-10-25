package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=ListSecretsResponse.Builder.class)

public final class ListSecretsResponse  {
    @Deprecated
    @java.beans.ConstructorProperties({"data"})
    public ListSecretsResponse(SecretList data) {
        super();
        this.data = data;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("data")
        private SecretList data;

        

        public Builder data(SecretList data) {
            this.data = data;
            return this;
        }


        public ListSecretsResponse build() {
            ListSecretsResponse model = new ListSecretsResponse(this.data);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(ListSecretsResponse model) {
            this.data(model.getData());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("data")
    private final SecretList data;

    
    public SecretList getData() {
        return data;
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
        sb.append("ListSecretsResponse(");
        sb.append("data=").append(String.valueOf(this.data));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListSecretsResponse)) {
            return false;
        }

        ListSecretsResponse other = (ListSecretsResponse) o;
        return java.util.Objects.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.data == null ? 43 : this.data.hashCode());
        return result;
    }


}

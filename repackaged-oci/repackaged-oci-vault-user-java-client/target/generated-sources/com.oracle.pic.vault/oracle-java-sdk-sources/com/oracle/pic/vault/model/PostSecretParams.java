package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=PostSecretParams.Builder.class)

public final class PostSecretParams  {
    @Deprecated
    @java.beans.ConstructorProperties({"secretParams"})
    public PostSecretParams(java.util.Map<String, String> secretParams) {
        super();
        this.secretParams = secretParams;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("secretParams")
        private java.util.Map<String, String> secretParams;

        

        public Builder secretParams(java.util.Map<String, String> secretParams) {
            this.secretParams = secretParams;
            return this;
        }


        public PostSecretParams build() {
            PostSecretParams model = new PostSecretParams(this.secretParams);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(PostSecretParams model) {
            this.secretParams(model.getSecretParams());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("secretParams")
    private final java.util.Map<String, String> secretParams;

    
    public java.util.Map<String, String> getSecretParams() {
        return secretParams;
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
        sb.append("PostSecretParams(");
        sb.append("secretParams=").append(String.valueOf(this.secretParams));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostSecretParams)) {
            return false;
        }

        PostSecretParams other = (PostSecretParams) o;
        return java.util.Objects.equals(this.secretParams, other.secretParams);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.secretParams == null ? 43 : this.secretParams.hashCode());
        return result;
    }


}

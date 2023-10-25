package com.oracle.pic.vault.model;



/**
 * 
**/
@jakarta.annotation.Generated(value = "OracleSDKGenerator", comments = "API Version: 1.0.0")
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder=PolicyListDetails.Builder.class)

public final class PolicyListDetails  {
    @Deprecated
    @java.beans.ConstructorProperties({"requestId", "leaseId", "renewable", "leaseDuration", "data"})
    public PolicyListDetails(String requestId, String leaseId, Boolean renewable, Integer leaseDuration, PolicyListData data) {
        super();
        this.requestId = requestId;
        this.leaseId = leaseId;
        this.renewable = renewable;
        this.leaseDuration = leaseDuration;
        this.data = data;
    }

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        @com.fasterxml.jackson.annotation.JsonProperty("request_id")
        private String requestId;

        

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("lease_id")
        private String leaseId;

        

        public Builder leaseId(String leaseId) {
            this.leaseId = leaseId;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("renewable")
        private Boolean renewable;

        

        public Builder renewable(Boolean renewable) {
            this.renewable = renewable;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("lease_duration")
        private Integer leaseDuration;

        

        public Builder leaseDuration(Integer leaseDuration) {
            this.leaseDuration = leaseDuration;
            return this;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("data")
        private PolicyListData data;

        

        public Builder data(PolicyListData data) {
            this.data = data;
            return this;
        }


        public PolicyListDetails build() {
            PolicyListDetails model = new PolicyListDetails(this.requestId
                , this.leaseId
                , this.renewable
                , this.leaseDuration
                , this.data);            return model;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Builder copy(PolicyListDetails model) {
            this.requestId(model.getRequestId());
            this.leaseId(model.getLeaseId());
            this.renewable(model.getRenewable());
            this.leaseDuration(model.getLeaseDuration());
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

    


    
    @com.fasterxml.jackson.annotation.JsonProperty("request_id")
    private final String requestId;

    
    public String getRequestId() {
        return requestId;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("lease_id")
    private final String leaseId;

    
    public String getLeaseId() {
        return leaseId;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("renewable")
    private final Boolean renewable;

    
    public Boolean getRenewable() {
        return renewable;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("lease_duration")
    private final Integer leaseDuration;

    
    public Integer getLeaseDuration() {
        return leaseDuration;
    }


    
    @com.fasterxml.jackson.annotation.JsonProperty("data")
    private final PolicyListData data;

    
    public PolicyListData getData() {
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
        sb.append("PolicyListDetails(");
        sb.append("requestId=").append(String.valueOf(this.requestId));
        sb.append(", leaseId=").append(String.valueOf(this.leaseId));
        sb.append(", renewable=").append(String.valueOf(this.renewable));
        sb.append(", leaseDuration=").append(String.valueOf(this.leaseDuration));
        sb.append(", data=").append(String.valueOf(this.data));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolicyListDetails)) {
            return false;
        }

        PolicyListDetails other = (PolicyListDetails) o;
        return java.util.Objects.equals(this.requestId, other.requestId) &&
            java.util.Objects.equals(this.leaseId, other.leaseId) &&
            java.util.Objects.equals(this.renewable, other.renewable) &&
            java.util.Objects.equals(this.leaseDuration, other.leaseDuration) &&
            java.util.Objects.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = (result * PRIME) + (this.requestId == null ? 43 : this.requestId.hashCode());
        result = (result * PRIME) + (this.leaseId == null ? 43 : this.leaseId.hashCode());
        result = (result * PRIME) + (this.renewable == null ? 43 : this.renewable.hashCode());
        result = (result * PRIME) + (this.leaseDuration == null ? 43 : this.leaseDuration.hashCode());
        result = (result * PRIME) + (this.data == null ? 43 : this.data.hashCode());
        return result;
    }


}

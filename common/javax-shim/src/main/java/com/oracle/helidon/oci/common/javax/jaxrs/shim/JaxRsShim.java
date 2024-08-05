/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

/**
 *
 */
public class JaxRsShim {

    private JaxRsShim() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     *
     * @param <T>       entity Java type.
     * @param javaxEntity entity data
     * @return entity instance
     */
    public static <T> jakarta.ws.rs.client.Entity<T> toJakarta(javax.ws.rs.client.Entity<T> javaxEntity) {
        T payload = javaxEntity.getEntity();
        javax.ws.rs.core.Variant variant = javaxEntity.getVariant();
        javax.ws.rs.core.MediaType mediaType = variant.getMediaType();
        jakarta.ws.rs.core.MediaType jakartaMediaType = jakarta.ws.rs.core.MediaType.valueOf(mediaType.toString());
        jakarta.ws.rs.core.Variant jakartaVariant = new jakarta.ws.rs.core.Variant(jakartaMediaType,
                                                                                   variant.getLanguage(),
                                                                                   variant.getEncoding());
        return jakarta.ws.rs.client.Entity.entity(payload, jakartaVariant);
    }

    /**
     *
     * @param <T>       entity Java type.
     * @param jakartaEntity entity data
     * @return entity instance
     */
    public static <T> javax.ws.rs.client.Entity<T> toJavax(jakarta.ws.rs.client.Entity<T> jakartaEntity) {
        T payload = jakartaEntity.getEntity();
        jakarta.ws.rs.core.Variant variant = jakartaEntity.getVariant();
        jakarta.ws.rs.core.MediaType mediaType = variant.getMediaType();
        javax.ws.rs.core.MediaType jakartaMediaType = javax.ws.rs.core.MediaType.valueOf(mediaType.toString());
        javax.ws.rs.core.Variant jakartaVariant = new javax.ws.rs.core.Variant(jakartaMediaType,
                                                                               variant.getLanguage(),
                                                                               variant.getEncoding());
        return javax.ws.rs.client.Entity.entity(payload, jakartaVariant);
    }

    /**
     *
     * @param in entity tag
     * @return entity tag
     */
    public static jakarta.ws.rs.core.EntityTag toJakarta(javax.ws.rs.core.EntityTag in) {
        return new jakarta.ws.rs.core.EntityTag(in.getValue(), in.isWeak());
    }

    /**
     *
     * @param in entity tag
     * @return entity tag
     */
    public static javax.ws.rs.core.EntityTag toJavax(jakarta.ws.rs.core.EntityTag in) {
        return new javax.ws.rs.core.EntityTag(in.getValue(), in.isWeak());
    }
}

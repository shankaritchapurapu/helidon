/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.javax.jaxrs.shim;

public class JaxRsShim {
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

    public static jakarta.ws.rs.core.EntityTag toJakarta(javax.ws.rs.core.EntityTag in) {
        return new jakarta.ws.rs.core.EntityTag(in.getValue(), in.isWeak());
    }

    public static javax.ws.rs.core.EntityTag toJavax(jakarta.ws.rs.core.EntityTag in) {
        return new javax.ws.rs.core.EntityTag(in.getValue(), in.isWeak());
    }
}

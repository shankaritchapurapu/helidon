/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.IOException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.Variant;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityTag;

/**
 * Helper class for translating Javax vs. Jakarta JAX-RS classes.
 */
public final class JaxRsShim {

    private JaxRsShim() {
        //noop
    }

    /**
     * Shim Javax JAX-RS Entity to Jakarta namespace.
     *
     * @param <T>         entity Java type.
     * @param javaxEntity entity data
     * @return entity instance
     */
    public static <T> Entity<T> toJakarta(javax.ws.rs.client.Entity<T> javaxEntity) {
        T payload = javaxEntity.getEntity();
        Variant variant = javaxEntity.getVariant();
        MediaType mediaType = variant.getMediaType();
        jakarta.ws.rs.core.MediaType jakartaMediaType = jakarta.ws.rs.core.MediaType.valueOf(mediaType.toString());
        jakarta.ws.rs.core.Variant jakartaVariant = new jakarta.ws.rs.core.Variant(jakartaMediaType,
                                                                                   variant.getLanguage(),
                                                                                   variant.getEncoding());
        return Entity.entity(payload, jakartaVariant);
    }

    /**
     * Shim Jakarta JAX-RS Entity to Javax namespace.
     *
     * @param <T>           entity Java type.
     * @param jakartaEntity entity data
     * @return entity instance
     */
    public static <T> javax.ws.rs.client.Entity<T> toJavax(Entity<T> jakartaEntity) {
        T payload = jakartaEntity.getEntity();
        jakarta.ws.rs.core.Variant variant = jakartaEntity.getVariant();
        jakarta.ws.rs.core.MediaType mediaType = variant.getMediaType();
        MediaType jakartaMediaType = MediaType.valueOf(mediaType.toString());
        Variant jakartaVariant = new Variant(jakartaMediaType,
                                             variant.getLanguage(),
                                             variant.getEncoding());
        return javax.ws.rs.client.Entity.entity(payload, jakartaVariant);
    }

    /**
     * Shim Javax JAX-RS EntityTag to Jakarta namespace.
     *
     * @param in entity tag
     * @return entity tag
     */
    public static EntityTag toJakarta(javax.ws.rs.core.EntityTag in) {
        return new EntityTag(in.getValue(), in.isWeak());
    }

    /**
     * Shim Jakarta JAX-RS EntityTag to Javax namespace.
     *
     * @param in entity tag
     * @return entity tag
     */
    public static javax.ws.rs.core.EntityTag toJavax(EntityTag in) {
        return new javax.ws.rs.core.EntityTag(in.getValue(), in.isWeak());
    }

    /**
     * Translate usual JAX-RS exceptions from Javax to Jakarta namespace.
     *
     * @param t Javax JAX-RS exception
     * @return Jakarta JAX-RS exception
     */
    @SuppressWarnings("unchecked")
    public static Throwable toJakarta(Throwable t) {
        if (t == null) {
            return null;
        }
        return switch (t) {
            case RuntimeException e -> toJakarta(e);
            case IOException e -> toJakarta(e);
            default -> t;
        };
    }

    /**
     * Translate usual JAX-RS exceptions from Javax to Jakarta namespace.
     *
     * @param t   Javax JAX-RS exception
     * @param <T> Expected exception type
     * @return Jakarta JAX-RS exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends RuntimeException> T toJakarta(RuntimeException t) {
        if (t == null) {
            return null;
        }
        // Spotbugs older than 4.8.6 reports pattern matching as false positive BC_UNCONFIRMED_CAST
        // https://github.com/spotbugs/spotbugs/issues/2782
        return (T) switch (t) {
            case BadRequestException e -> new JakartaBadRequestException(e);
            case ForbiddenException e -> new JakartaForbiddenException(e);
            case InternalServerErrorException e -> new JakartaInternalServerErrorException(e);
            case NotAcceptableException e -> new JakartaNotAcceptableException(e);
            case NotAllowedException e -> new JakartaNotAllowedException(e);
            case NotAuthorizedException e -> new JakartaNotAuthorizedException(e);
            case NotFoundException e -> new JakartaNotFoundException(e);
            case NotSupportedException e -> new JakartaNotSupportedException(e);
            case ResponseProcessingException e -> new JakartaResponseProcessingException(e);
            case ProcessingException e -> new JakartaProcessingException(e);
            case RedirectionException e -> new JakartaRedirectionException(e);
            case ServiceUnavailableException e -> new JakartaServiceUnavailableException(e);
            case UriBuilderException e -> new JakartaUriBuilderException(e);
            case ServerErrorException e -> new JakartaServerErrorException(e);
            case ClientErrorException e -> new JakartaClientErrorException(e);
            case WebApplicationException e -> new JakartaWebApplicationException(e);
            default -> t;
        };
    }

    /**
     * Translate usual JAX-RS exceptions from Javax to Jakarta namespace.
     *
     * @param t   Javax JAX-RS exception
     * @param <T> Expected exception type
     * @return Jakarta JAX-RS exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends IOException> T toJakarta(IOException t) {
        if (t == null) {
            return null;
        }
        if (t instanceof NoContentException nce) {
            return (T) new JakartaNoContentException(nce);
        }
        return (T) t;
    }
}

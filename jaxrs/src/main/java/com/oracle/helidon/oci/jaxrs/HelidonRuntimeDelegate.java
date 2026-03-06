/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.message.internal.CacheControlProvider;
import org.glassfish.jersey.message.internal.CookieProvider;
import org.glassfish.jersey.message.internal.DateProvider;
import org.glassfish.jersey.message.internal.EntityTagProvider;
import org.glassfish.jersey.message.internal.LinkProvider;
import org.glassfish.jersey.message.internal.LocaleProvider;
import org.glassfish.jersey.message.internal.MediaTypeProvider;
import org.glassfish.jersey.message.internal.NewCookieProvider;
import org.glassfish.jersey.message.internal.StringHeaderProvider;
import org.glassfish.jersey.message.internal.UriProvider;
import org.glassfish.jersey.spi.HeaderDelegateProvider;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

/**
 * An implementation of {@link javax.ws.rs.ext.RuntimeDelegate} for Helidon.
 */
public class HelidonRuntimeDelegate extends RuntimeDelegate {

    private final Set<HeaderDelegateProvider<?>> providers;
    private final Map<Class<?>, HeaderDelegate<?>> headerDelegates;

    /**
     * Default constructor.
     */
    public HelidonRuntimeDelegate() {
        this.providers = new HashSet<>();
        this.providers.add(new CacheControlProvider());
        this.providers.add(new CookieProvider());
        this.providers.add(new DateProvider());
        this.providers.add(new EntityTagProvider());
        this.providers.add(new LinkProvider());
        this.providers.add(new LocaleProvider());
        this.providers.add(new MediaTypeProvider());
        this.providers.add(new NewCookieProvider());
        this.providers.add(new StringHeaderProvider());
        this.providers.add(new UriProvider());

        this.headerDelegates = new HashMap<>();
        this.headerDelegates.put(EntityTag.class, findHeaderDelegate(EntityTag.class));
        this.headerDelegates.put(MediaType.class, findHeaderDelegate(MediaType.class));
        this.headerDelegates.put(CacheControl.class, findHeaderDelegate(CacheControl.class));
        this.headerDelegates.put(NewCookie.class, findHeaderDelegate(NewCookie.class));
        this.headerDelegates.put(Cookie.class, findHeaderDelegate(Cookie.class));
        this.headerDelegates.put(URI.class, findHeaderDelegate(URI.class));
        this.headerDelegates.put(Date.class, findHeaderDelegate(Date.class));
        this.headerDelegates.put(String.class, findHeaderDelegate(String.class));
    }

    @Override
    public UriBuilder createUriBuilder() {
        return new JerseyUriBuilder();      // use Jersey's
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return new HelidonResponse.ResponseBuilder();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException {
        Objects.requireNonNull(type, "Type must not be null");
        HeaderDelegate<T> delegate = (HeaderDelegate<T>) headerDelegates.get(type);
        return delegate != null ? delegate : findHeaderDelegate(type);
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Link.Builder createLinkBuilder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("unchecked")
    private <T> HeaderDelegate<T> findHeaderDelegate(Class<T> type) {
        for (HeaderDelegateProvider<?> provider : providers) {
            if (provider.supports(type)) {
                return (HeaderDelegate<T>) provider;
            }
        }
        return null;
    }
}

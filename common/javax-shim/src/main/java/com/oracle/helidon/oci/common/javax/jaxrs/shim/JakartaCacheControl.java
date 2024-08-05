/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.CacheControl;

public class JakartaCacheControl extends CacheControl {
    private final javax.ws.rs.core.CacheControl delegate;

    public JakartaCacheControl(javax.ws.rs.core.CacheControl delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isMustRevalidate() {
        return delegate.isMustRevalidate();
    }

    @Override
    public void setMustRevalidate(boolean mustRevalidate) {
        delegate.setMustRevalidate(mustRevalidate);
    }

    @Override
    public boolean isProxyRevalidate() {
        return delegate.isProxyRevalidate();
    }

    @Override
    public void setProxyRevalidate(boolean proxyRevalidate) {
        delegate.setProxyRevalidate(proxyRevalidate);
    }

    @Override
    public int getMaxAge() {
        return delegate.getMaxAge();
    }

    @Override
    public void setMaxAge(int maxAge) {
        delegate.setMaxAge(maxAge);
    }

    @Override
    public int getSMaxAge() {
        return delegate.getSMaxAge();
    }

    @Override
    public void setSMaxAge(int smaxAge) {
        delegate.setSMaxAge(smaxAge);
    }

    @Override
    public List<String> getNoCacheFields() {
        return delegate.getNoCacheFields();
    }

    @Override
    public boolean isNoCache() {
        return delegate.isNoCache();
    }

    @Override
    public void setNoCache(boolean noCache) {
        delegate.setNoCache(noCache);
    }

    @Override
    public boolean isPrivate() {
        return delegate.isPrivate();
    }

    @Override
    public void setPrivate(boolean flag) {
        delegate.setPrivate(flag);
    }

    @Override
    public List<String> getPrivateFields() {
        return delegate.getPrivateFields();
    }

    @Override
    public boolean isNoTransform() {
        return delegate.isNoTransform();
    }

    @Override
    public void setNoTransform(boolean noTransform) {
        delegate.setNoTransform(noTransform);
    }

    @Override
    public boolean isNoStore() {
        return delegate.isNoStore();
    }

    @Override
    public void setNoStore(boolean noStore) {
        delegate.setNoStore(noStore);
    }

    @Override
    public Map<String, String> getCacheExtension() {
        return delegate.getCacheExtension();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}

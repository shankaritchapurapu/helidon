/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A read-only {@link Map}-like context object used to store authentication-related attributes
 * produced by the {@link com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter}.
 * <p>
 * For example, it can return a {@link com.oracle.pic.identity.authentication.Principal} as
 * the value for the property
 * {@link com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter#PIC_PRINCIPAL}.
 *
 * @see com.oracle.pic.identity.authorization.sdk.AuthContextRequestFilter
 */
public class IdentityContext implements Map<String, Object> {

    private final Map<String, Object> delegate;

    /**
     * Create a new identity context backed by the provided properties map.
     *
     * @param delegate request properties produced by the Auth SDK filter
     */
    public IdentityContext(Map<String, Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return delegate.get(key);
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<Object> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        delegate.forEach(action);
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

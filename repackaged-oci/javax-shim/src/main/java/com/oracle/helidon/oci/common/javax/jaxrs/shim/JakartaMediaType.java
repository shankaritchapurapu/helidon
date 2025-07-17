/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;

@SuppressWarnings({"CPD-START", "PMD.UselessParentheses"})
public class JakartaMediaType extends MediaType {
    private final javax.ws.rs.core.MediaType delegate;

    public JakartaMediaType(javax.ws.rs.core.MediaType delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public boolean isWildcardType() {
        return delegate.isWildcardType();
    }

    @Override
    public String getSubtype() {
        return delegate.getSubtype();
    }

    @Override
    public boolean isWildcardSubtype() {
        return delegate.isWildcardSubtype();
    }

    @Override
    public Map<String, String> getParameters() {
        return delegate.getParameters();
    }

    @Override
    public MediaType withCharset(String charset) {
        return new JakartaMediaType(delegate.withCharset(charset));
    }

    @Override
    public boolean isCompatible(MediaType other) {
        return delegate.isCompatible(new JavaxMediaType(other));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof javax.ws.rs.core.MediaType other)) {
            return false;
        }

        return (
                this.getType().equalsIgnoreCase(other.getType())
                        && this.getSubtype().equalsIgnoreCase(other.getSubtype())
                        && this.getParameters().equals(other.getParameters()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getType().toLowerCase(), this.getSubtype().toLowerCase(), this.getParameters());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}

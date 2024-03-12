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

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.CacheControl;

public class JavaxCacheControl extends CacheControl {
    private final jakarta.ws.rs.core.CacheControl delegate;

    public JavaxCacheControl(jakarta.ws.rs.core.CacheControl delegate) {
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
    public void setNoCache(boolean noCache) {
        delegate.setNoCache(noCache);
    }

    @Override
    public boolean isNoCache() {
        return delegate.isNoCache();
    }

    @Override
    public boolean isPrivate() {
        return delegate.isPrivate();
    }

    @Override
    public List<String> getPrivateFields() {
        return delegate.getPrivateFields();
    }

    @Override
    public void setPrivate(boolean flag) {
        delegate.setPrivate(flag);
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

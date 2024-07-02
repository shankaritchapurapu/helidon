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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

public class JavaxMultivaluedMap<K, V> implements MultivaluedMap<K, V> {
    private final jakarta.ws.rs.core.MultivaluedMap<K, V> delegate;

    public JavaxMultivaluedMap(jakarta.ws.rs.core.MultivaluedMap<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void putSingle(K key, V value) {
        delegate.putSingle(key, value);
    }

    @Override
    public void add(K key, V value) {
        delegate.add(key, value);
    }

    @Override
    public V getFirst(K key) {
        return delegate.getFirst(key);
    }

    @Override
    public void addAll(K key, V... newValues) {
        delegate.addAll(key, newValues);
    }

    @Override
    public void addAll(K key, List<V> valueList) {
        delegate.addAll(key, valueList);
    }

    @Override
    public void addFirst(K key, V value) {
        delegate.addFirst(key, value);
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<K, V> otherMap) {
        return delegate.equalsIgnoreValueOrder(new JakartaMultivaluedMap<K, V>(otherMap));
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
    public List<V> get(Object key) {
        return delegate.get(key);
    }

    @Override
    public List<V> put(K key, List<V> value) {
        return delegate.put(key, value);
    }

    @Override
    public List<V> remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> map) {
        delegate.putAll(map);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<List<V>> values() {
        return delegate.values();
    }

    @Override
    public Set<Map.Entry<K, List<V>>> entrySet() {
        return delegate.entrySet();
    }
}

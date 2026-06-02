/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import io.helidon.http.Header;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.webserver.http.ServerRequest;

/**
 * Implementation of {@link javax.ws.rs.core.MultivaluedMap} for Helidon.
 */
public class HelidonMultivaluedHashMap implements MultivaluedMap<String, String> {

    private final ServerRequest request;

    /**
     * Constructor from request.
     *
     * @param request the request
     */
    public HelidonMultivaluedHashMap(ServerRequest request) {
        this.request = request;
    }

    @Override
    public void putSingle(String key, String value) {
        request.header(HeaderValues.create(key, value));
    }

    @Override
    public void add(String key, String value) {
        HeaderName headerName = HeaderNames.create(key);
        if (request.headers().contains(headerName)) {
            Header header = request.headers().get(headerName);
            List<String> values = new ArrayList<>(header.allValues());
            values.add(value);
            request.header(HeaderValues.create(key, values));
        } else {
            putSingle(key, value);
        }
    }

    @Override
    public String getFirst(String key) {
        return request.headers().first(HeaderNames.create(key)).orElse(null);
    }

    @Override
    public void addAll(String key, String... newValues) {
        HeaderName headerName = HeaderNames.create(key);
        Header header = request.headers().get(headerName);
        List<String> values = new ArrayList<>(header.allValues());
        values.addAll(List.of(newValues));
        request.header(HeaderValues.create(key, values));
    }

    @Override
    public void addAll(String key, List<String> valueList) {
        addAll(key, valueList.toArray(new String[0]));
    }

    @Override
    public void addFirst(String key, String value) {
        HeaderName headerName = HeaderNames.create(key);
        if (request.headers().contains(headerName)) {
            Header header = request.headers().get(headerName);
            List<String> values = new ArrayList<>();
            values.add(value);
            values.addAll(header.allValues());
            request.header(HeaderValues.create(key, values));
        } else {
            putSingle(key, value);
        }
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<String, String> otherMap) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public int size() {
        return request.headers().size();
    }

    @Override
    public boolean isEmpty() {
        return request.headers().size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String s) {
            return request.headers().contains(HeaderNames.create(s));
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof String s) {
            return request.headers()
                    .stream()
                    .map(Header::allValues)
                    .flatMap(List::stream)
                    .anyMatch(val -> Objects.equals(val, s));
        }
        return false;
    }

    @Override
    public List<String> get(Object key) {
        if (key instanceof String s) {
            HeaderName headerName = HeaderNames.create(s);
            if (request.headers().contains(headerName)) {
                return request.headers().get(headerName).allValues();
            }
        }
        return List.of();
    }

    @Override
    public List<String> put(String key, List<String> values) {
        HeaderName headerName = HeaderNames.create(key);
        if (request.headers().contains(headerName)) {
            Header header = request.headers().get(headerName);
            List<String> oldValues = header.allValues();
            request.header(HeaderValues.create(key, values));
            return oldValues;
        } else {
            request.header(HeaderValues.create(key, values));
            return List.of();
        }
    }

    @Override
    public List<String> remove(Object key) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Set<String> keySet() {
        return request.headers()
                .stream()
                .map(Header::name)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<List<String>> values() {
        return request.headers()
                .stream()
                .map(Header::allValues)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return request.headers()
                .stream()
                .map(header -> new Entry<String, List<String>>() {

                    @Override
                    public String getKey() {
                        return header.name();
                    }

                    @Override
                    public List<String> getValue() {
                        return header.allValues();
                    }

                    @Override
                    public List<String> setValue(List<String> value) {
                        throw new UnsupportedOperationException("not yet implemented");
                    }
                })
                .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HelidonMultivaluedHashMap that)) {
            return false;
        }
        return Objects.equals(request, that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(request);
    }
}

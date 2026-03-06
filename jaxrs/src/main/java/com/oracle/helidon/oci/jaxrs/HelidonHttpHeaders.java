/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import io.helidon.webserver.http.ServerRequest;

/**
 * Wrapper that adapts Helidon's ServerRequest headers to JAX-RS HttpHeaders.
 */
public class HelidonHttpHeaders implements HttpHeaders {

    private final HelidonMultivaluedHashMap headers;

    /**
     * Create headers from server request.
     *
     * @param request the request
     */
    public HelidonHttpHeaders(ServerRequest request) {
        this.headers = new HelidonMultivaluedHashMap(request);
    }

    @Override
    public List<String> getRequestHeader(String name) {
        return headers.get(name);
    }

    @Override
    public String getHeaderString(String name) {
        return headers.getFirst(name);
    }

    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        return headers;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        String accept = getHeaderString("Accept");
        if (accept == null || accept.isEmpty()) {
            return List.of(MediaType.WILDCARD_TYPE);
        }
        List<MediaType> types = new ArrayList<>();
        for (String part : accept.split(",")) {
            try {
                types.add(MediaType.valueOf(part.trim()));
            } catch (IllegalArgumentException e) {
                // Skip invalid media types
            }
        }
        return types.isEmpty() ? List.of(MediaType.WILDCARD_TYPE) : types;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        String acceptLang = getHeaderString("Accept-Language");
        if (acceptLang == null || acceptLang.isEmpty()) {
            return List.of(Locale.getDefault());
        }
        List<Locale> locales = new ArrayList<>();
        for (String part : acceptLang.split(",")) {
            String lang = part.split(";")[0].trim();
            if (!lang.isEmpty()) {
                locales.add(Locale.forLanguageTag(lang));
            }
        }
        return locales.isEmpty() ? List.of(Locale.getDefault()) : locales;
    }

    @Override
    public MediaType getMediaType() {
        String contentType = getHeaderString("Content-Type");
        return contentType != null ? MediaType.valueOf(contentType) : null;
    }

    @Override
    public Locale getLanguage() {
        String lang = getHeaderString("Content-Language");
        return lang != null ? Locale.forLanguageTag(lang) : null;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public int getLength() {
        String length = getHeaderString("Content-Length");
        if (length != null) {
            try {
                return Integer.parseInt(length);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}



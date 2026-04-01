/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.store;

import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

/**
 * HTTP endpoint that demonstrates using the Kiev integration through an injected application service.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/store")
@Service.Singleton
class StoreEndpoint {
    private final StoreService storeService;

    @Service.Inject
    StoreEndpoint(StoreService storeService) {
        this.storeService = storeService;
    }

    @Http.GET
    @Http.Path("/items")
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String list(@Http.QueryParam("pageSize") Optional<String> pageSize,
                @Http.QueryParam("pageToken") Optional<String> pageToken) {
        StorePage result = storeService.list(pageToken.filter(token -> !token.isBlank()).orElse(null),
                                             parsePageSize(pageSize.orElse(null)));
        return formatPage(result);
    }

    @Http.POST
    @Http.Path("/items")
    @Http.Consumes(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String post(@Http.Entity String body) {
        String[] entry = parseEntry(body);
        String storedValue = storeService.post(entry[0], entry[1]);
        return entry[0] + "=" + storedValue;
    }

    @Http.PUT
    @Http.Path("/items/{key}")
    @Http.Consumes(MediaTypes.TEXT_PLAIN_VALUE)
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String put(@Http.PathParam("key") String key, @Http.Entity String body) {
        String itemKey = parseKey(key);
        String value = parseValue(body);
        String storedValue = storeService.put(itemKey, value);
        return itemKey + "=" + storedValue;
    }

    @Http.GET
    @Http.Path("/items/{key}")
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String get(@Http.PathParam("key") String key) {
        String itemKey = parseKey(key);
        return storeService.get(itemKey)
                .orElseThrow(() -> new HttpException("No value found for key: " + itemKey, Status.NOT_FOUND_404));
    }

    @Http.DELETE
    @Http.Path("/items/{key}")
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String delete(@Http.PathParam("key") String key) {
        String itemKey = parseKey(key);
        return storeService.delete(itemKey)
                .map(ignored -> itemKey)
                .orElseThrow(() -> new HttpException("No value found for key: " + itemKey, Status.NOT_FOUND_404));
    }

    private static String[] parseEntry(String body) {
        int separator = body.indexOf('=');
        if (separator <= 0 || separator == body.length() - 1) {
            throw new HttpException("Request body must use key=value format", Status.BAD_REQUEST_400);
        }

        String key = parseKey(body.substring(0, separator));
        String value = parseValue(body.substring(separator + 1));
        return new String[] {key, value};
    }

    private static String parseKey(String body) {
        String key = body == null ? "" : body.trim();
        if (key.isEmpty()) {
            throw new HttpException("Request must contain a key", Status.BAD_REQUEST_400);
        }
        return key;
    }

    private static String parseValue(String body) {
        String value = body == null ? "" : body.trim();
        if (value.isEmpty()) {
            throw new HttpException("Request must contain a value", Status.BAD_REQUEST_400);
        }
        return value;
    }

    private static int parsePageSize(String value) {
        if (value == null || value.isBlank()) {
            return 10;
        }
        try {
            int pageSize = Integer.parseInt(value);
            if (pageSize <= 0 || pageSize > 100) {
                throw new HttpException("pageSize must be between 1 and 100", Status.BAD_REQUEST_400);
            }
            return pageSize;
        } catch (NumberFormatException e) {
            throw new HttpException("pageSize must be an integer", Status.BAD_REQUEST_400, e);
        }
    }

    private static String formatPage(StorePage page) {
        StringBuilder result = new StringBuilder();
        for (StoreItem item : page.items()) {
            result.append(item.id)
                    .append('=')
                    .append(item.value)
                    .append('\n');
        }
        page.nextPageToken().ifPresent(token -> result.append("next-page-token=").append(token).append('\n'));
        return result.toString();
    }
}

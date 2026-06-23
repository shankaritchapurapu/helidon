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
 * JSON endpoint that demonstrates reading Kiev stream records for the store example.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/store/stream")
@Service.Singleton
class StreamingStoreEndpoint {
    private final StoreStreamService streamService;

    @Service.Inject
    StreamingStoreEndpoint(StoreStreamService streamService) {
        this.streamService = streamService;
    }

    @Http.GET
    @Http.Path("/cursors")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    StoreStreamCursors streamCursors() {
        return streamService.cursors();
    }

    @Http.GET
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    StoreStreamPage stream(@Http.QueryParam("cursor") Optional<String> cursor,
                           @Http.QueryParam("limit") Optional<String> limit) {
        int recordLimit = parseLimit(limit.orElse(null));
        String streamCursor = cursor.filter(value -> !value.isBlank())
                .orElseThrow(() -> new HttpException("cursor query parameter is required", Status.BAD_REQUEST_400));
        return streamService.get(streamCursor, recordLimit);
    }

    private static int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return 10;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 100) {
                throw new HttpException("limit must be between 1 and 100", Status.BAD_REQUEST_400);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new HttpException("limit must be an integer", Status.BAD_REQUEST_400, e);
        }
    }
}

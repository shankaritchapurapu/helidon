/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.Function;

class FutureUtils {

    private FutureUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static Future<javax.ws.rs.core.Response> toJavaxResponse(Future<jakarta.ws.rs.core.Response> jakartaResponse) {
        return mapFuture(jakartaResponse, jakarta.ws.rs.core.Response.class, JavaxResponse::new);
    }

    static Future<jakarta.ws.rs.core.Response> toJakartaResponse(Future<javax.ws.rs.core.Response> javaxResponse) {
        return mapFuture(javaxResponse, javax.ws.rs.core.Response.class, JakartaResponse::new);
    }

    private static <I, O> CompletableFuture<O> mapFuture(Future<I> future, Class<I> inType, Function<I, O> mapper) {
        if (future instanceof CompletionStage<?> completionStage) {
            return completionStage
                    .thenApply(inType::cast)
                    .thenApply(mapper)
                    .toCompletableFuture();
        }

        throw new UnsupportedOperationException("Only completable future is supported!");
    }
}

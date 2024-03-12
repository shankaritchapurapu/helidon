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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.Function;

class FutureUtils {
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

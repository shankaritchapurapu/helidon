/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.identity;

import java.util.function.Supplier;

import javax.ws.rs.core.Context;

import io.helidon.webserver.ServerRequest;

/**
 * Bridges from Jersey's {@link javax.ws.rs.core.Context} over to a CDI request-scoped producer.
 * @see com.oracle.helidon.oci.identity.ContainerRequestContextSupplier
 */
class ServerRequestSupplier implements Supplier<ServerRequest> {
    @Context
    private ServerRequest serverRequest;

    @Override
    public ServerRequest get() {
        return serverRequest;
    }
}

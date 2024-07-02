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

import java.util.function.Function;

import jakarta.ws.rs.client.InvocationCallback;

public class JakartaInvocationCallback<I, O> implements InvocationCallback<O> {

    private final javax.ws.rs.client.InvocationCallback<I> delegate;
    private final Function<O, I> mapper;

    public JakartaInvocationCallback(javax.ws.rs.client.InvocationCallback<I> delegate, Function<O, I> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    public JakartaInvocationCallback(javax.ws.rs.client.InvocationCallback<I> delegate) {
        this.delegate = delegate;
        this.mapper = out -> (I) out;
    }

    @Override
    public void completed(O payload) {
        delegate.completed(mapper.apply(payload));
    }

    @Override
    public void failed(Throwable throwable) {
        delegate.failed(throwable);
    }
}

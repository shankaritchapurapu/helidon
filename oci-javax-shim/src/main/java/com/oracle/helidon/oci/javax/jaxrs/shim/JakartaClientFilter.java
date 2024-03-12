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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

public class JakartaClientFilter implements ClientRequestFilter {

    private final javax.ws.rs.client.ClientRequestFilter delegate;

    public JakartaClientFilter(Class<?> delegateclass) {
        try {
            delegate = (javax.ws.rs.client.ClientRequestFilter) delegateclass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public JakartaClientFilter(javax.ws.rs.client.ClientRequestFilter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        delegate.filter(new JavaxClientRequestContext(requestContext));
    }
}

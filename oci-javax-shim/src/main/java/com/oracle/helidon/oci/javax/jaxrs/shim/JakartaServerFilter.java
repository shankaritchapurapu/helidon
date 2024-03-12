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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

public class JakartaServerFilter implements ContainerRequestFilter {

    private final javax.ws.rs.container.ContainerRequestFilter delegate;

    public JakartaServerFilter(javax.ws.rs.container.ContainerRequestFilter delegate) {
        this.delegate = delegate;
    }

    public JakartaServerFilter(Class<? extends javax.ws.rs.container.ContainerRequestFilter> delegateClass) {
        try {
            this.delegate = delegateClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        delegate.filter(new JavaxContainerRequestContext(requestContext));
    }
}

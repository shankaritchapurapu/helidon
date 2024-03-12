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

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

public class JavaxRequest implements Request {
    private final jakarta.ws.rs.core.Request delegate;

    public JavaxRequest(jakarta.ws.rs.core.Request delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public Variant selectVariant(List<Variant> variants) {
        return new JavaxVariant(delegate.selectVariant(variants.stream()
                                                               .map(JakartaVariant::new)
                                                               .map(jakarta.ws.rs.core.Variant.class::cast)
                                                               .toList()));
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        return new JavaxResponse.JavaxResponseBuilder(delegate.evaluatePreconditions(JaxRsShim.toJakarta(eTag)));
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(Date lastModified) {
        return null;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        return null;
    }

    @Override
    public Response.ResponseBuilder evaluatePreconditions() {
        return null;
    }
}

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

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

public class JakartaRequestInterceptor implements ClientRequestFilter {

    private final RequestInterceptor delegate;

    public JakartaRequestInterceptor(Class<?> delegateclass) {
        try {
            delegate = (RequestInterceptor) delegateclass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public JakartaRequestInterceptor(RequestInterceptor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void filter(ClientRequestContext jakartaReq) throws IOException {
        delegate.intercept(new HttpRequest() {

            @Override
            public Method method() {
                return Method.valueOf(jakartaReq.getMethod());
            }

            @Override
            public HttpRequest body(Object o) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public HttpRequest body(InputStream inputStream, long l) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public Object body() {
                return jakartaReq.getEntity();
            }

            @Override
            public HttpRequest appendPathPart(String s) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public HttpRequest query(String s, String s1) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public URI uri() {
                return jakartaReq.getUri();
            }

            @Override
            public HttpRequest header(String name, String value) {
                MultivaluedMap<String, Object> headers = jakartaReq.getHeaders();
                headers.add(name, value);
                return this;
            }

            @Override
            public Map<String, List<String>> headers() {
                return jakartaReq.getStringHeaders();
            }

            @Override
            public Object attribute(String s) {
                return jakartaReq.getProperty(s);
            }

            @Override
            public HttpRequest removeAttribute(String s) {
                jakartaReq.removeProperty(s);
                return this;
            }

            @Override
            public HttpRequest attribute(String s, Object o) {
                jakartaReq.setProperty(s, o);
                return this;
            }

            @Override
            public HttpRequest offloadExecutor(Executor executor) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public HttpRequest copy() {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public void discard() {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public CompletionStage<HttpResponse> execute() {
                throw new UnsupportedOperationException("Not implemented");
            }
        });
    }
}

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

package com.oracle.helidon.oci.integrations.requestid;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.logging.jul.JulMdc;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

class RequestIdServerFilterTest {

    @Test
    void testRequestFilter() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        ContainerRequestContext requestContext = newRequestContext(map);

        RequestIdServerFilter filter = new RequestIdServerFilter();
        filter.filter(requestContext);
        assertThat(map.containsKey(OciHeaderNames.OPC_REQUEST_ID), is(true));
    }

    @Test
    void testResponseFilter() {
        MultivaluedMap<String, String> requestMap = new MultivaluedHashMap<>();
        ContainerRequestContext requestContext = newRequestContext(requestMap);
        MultivaluedMap<String, Object> responseMap = new MultivaluedHashMap<>();
        ContainerResponseContext responseContext = newResponseContext(responseMap);

        RequestIdServerFilter filter = new RequestIdServerFilter();
        filter.filter(requestContext, responseContext);
        assertThat(requestMap.getFirst(OciHeaderNames.OPC_REQUEST_ID), is(responseMap.getFirst(OciHeaderNames.OPC_REQUEST_ID)));
    }

    @Test
    void testLoggingMdc() {
        ContainerRequestContext requestContext = newRequestContext(null);
        RequestIdServerFilter filter = new RequestIdServerFilter();
        filter.filter(requestContext);
        assertThat(JulMdc.get(OciHeaderNames.OPC_REQUEST_ID), notNullValue());
    }

    @Test
    void testContext() {
        Context context = Context.create();
        Contexts.runInContext(context, () -> {
            ContainerRequestContext requestContext = newRequestContext(null);
            RequestIdServerFilter filter = new RequestIdServerFilter();
            filter.filter(requestContext);
        });
        assertThat(context.get(Request.class, String.class).isPresent(), is(true));
    }

    private ContainerRequestContext newRequestContext(MultivaluedMap<String, String> map) {
        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(map == null ? new MultivaluedHashMap<>() : map);
        return requestContext;
    }

    private ContainerResponseContext newResponseContext(MultivaluedMap<String, Object> map) {
        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        when(responseContext.getHeaders()).thenReturn(map == null ? new MultivaluedHashMap<>() : map);
        return responseContext;
    }
}

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

package com.oracle.helidon.oci.requestid;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.testing.http.junit5.HttpHeaderMatcher;
import io.helidon.http.Header;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.ServerResponseHeaders;
import io.helidon.http.WritableHeaders;
import io.helidon.logging.jul.JulMdc;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.requestid.RequestIdServerFilter.OCI_REQUEST_ID_HEADER;
import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestIdServerFilterTest {

    @Test
    void testFilter() {
        WritableHeaders<?> reqHeaders = WritableHeaders.create();
        ServerResponseHeaders resHeaders = ServerResponseHeaders.create();

        RequestIdServerFilter filter = new RequestIdServerFilter();
        filter.filter(mockFilterChain(),
                      mockRoutingRequest(reqHeaders),
                      mockRoutingResponse(resHeaders));

        assertThat("Request header must be added", reqHeaders, HttpHeaderMatcher.hasHeader(OCI_REQUEST_ID_HEADER));
        assertThat("Response header must be added", resHeaders, HttpHeaderMatcher.hasHeader(OCI_REQUEST_ID_HEADER));
        assertThat("Response header must match request header",
                   reqHeaders.get(OCI_REQUEST_ID_HEADER),
                   is(resHeaders.get(OCI_REQUEST_ID_HEADER)));
    }

    @Test
    void testLoggingMdc() {
        WritableHeaders<?> reqHeaders = WritableHeaders.create();
        ServerResponseHeaders resHeaders = ServerResponseHeaders.create();

        RequestIdServerFilter filter = new RequestIdServerFilter();
        filter.filter(mockFilterChain(),
                      mockRoutingRequest(reqHeaders),
                      mockRoutingResponse(resHeaders));

        assertThat(JulMdc.get(OciRequestId.OCI_REQUEST_ID), notNullValue());
        assertThat(JulMdc.get(OciRequestId.OCI_REQUEST_ID), is(reqHeaders.get(OCI_REQUEST_ID_HEADER).getString()));
    }

    @Test
    void testContext() {
        WritableHeaders<?> reqHeaders = WritableHeaders.create();
        ServerResponseHeaders resHeaders = ServerResponseHeaders.create();

        Context context = Context.create();
        Contexts.runInContext(context, () -> {
            RequestIdServerFilter filter = new RequestIdServerFilter();
            filter.filter(mockFilterChain(),
                          mockRoutingRequest(reqHeaders),
                          mockRoutingResponse(resHeaders));
        });
        assertThat(context.get(OciRequestId.class)
                           .map(OciRequestId::upstreamHeaderValue),
                   optionalValue(is(reqHeaders.get(OCI_REQUEST_ID_HEADER).getString())));
    }

    private static FilterChain mockFilterChain() {
        return mock(FilterChain.class);
    }

    private static RoutingRequest mockRoutingRequest(WritableHeaders<?> headers) {
        ServerRequestHeaders requestHeaders = ServerRequestHeaders.create(headers);
        RoutingRequest mock = mock(RoutingRequest.class);
        when(mock.headers()).thenReturn(requestHeaders);

        doAnswer(invocation -> {
            headers.set(invocation.getArgument(0));
            return null;
        }).when(mock)
                .header(any(Header.class));

        return mock;
    }

    private static RoutingResponse mockRoutingResponse(ServerResponseHeaders headers) {
        RoutingResponse mock = mock(RoutingResponse.class);

        when(mock.headers()).thenReturn(headers);
        return mock;
    }
}

/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.requestid.webserver;

import io.helidon.common.Weights;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.testing.http.junit5.HttpHeaderMatcher;
import io.helidon.http.Header;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.ServerResponseHeaders;
import io.helidon.http.WritableHeaders;
import io.helidon.logging.common.LogConfig;
import io.helidon.logging.jul.JulMdc;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import com.oracle.helidon.oci.requestid.OciRequestId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.requestid.webserver.RequestIdServerFilter.OCI_REQUEST_ID_HEADER;
import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestIdServerFilterTest {
    @BeforeAll
    static void setup() {
        LogConfig.configureRuntime();
    }

    @Test
    void testFeatureControlsRegistrationOrder() {
        assertThat(Weights.find(RequestIdServerFeature.create(), 100), is(1050.0));
        assertThat(Weights.find(new RequestIdServerFilter(), 100), is(100.0));
    }

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

    @Test
    void testMalformedRequestIdHeader() {
        WritableHeaders<?> reqHeaders = WritableHeaders.create();
        reqHeaders.set(OCI_REQUEST_ID_HEADER, "/");
        ServerResponseHeaders resHeaders = ServerResponseHeaders.create();

        RequestIdServerFilter filter = new RequestIdServerFilter();
        filter.filter(mockFilterChain(),
                      mockRoutingRequest(reqHeaders),
                      mockRoutingResponse(resHeaders));

        assertThat(reqHeaders.get(OCI_REQUEST_ID_HEADER).getString(), notNullValue());
        assertThat(reqHeaders.get(OCI_REQUEST_ID_HEADER).getString(), not("/"));
        assertThat(resHeaders.get(OCI_REQUEST_ID_HEADER), is(reqHeaders.get(OCI_REQUEST_ID_HEADER)));
    }

    @Test
    void testInjectableFactory() {
        WritableHeaders<?> reqHeaders = WritableHeaders.create();
        ServerResponseHeaders resHeaders = ServerResponseHeaders.create();

        Context context = Context.create();
        Contexts.runInContext(context, () -> {
            RequestIdServerFilter filter = new RequestIdServerFilter();
            filter.filter(mockFilterChain(),
                          mockRoutingRequest(reqHeaders),
                          mockRoutingResponse(resHeaders));
        });

        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.context()).thenReturn(context);

        OciRequestId requestId = new OciRequestIdFactory(serverRequest).get();
        assertThat(requestId.upstreamHeaderValue(), is(reqHeaders.get(OCI_REQUEST_ID_HEADER).getString()));
    }

    private static FilterChain mockFilterChain() {
        return mock(FilterChain.class);
    }

    private static RoutingRequest mockRoutingRequest(WritableHeaders<?> headers) {
        ServerRequestHeaders requestHeaders = ServerRequestHeaders.create(headers);
        RoutingRequest mock = mock(RoutingRequest.class);
        when(mock.headers()).thenReturn(requestHeaders);
        when(mock.context()).thenReturn(Contexts.context().orElseGet(Context::create));

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

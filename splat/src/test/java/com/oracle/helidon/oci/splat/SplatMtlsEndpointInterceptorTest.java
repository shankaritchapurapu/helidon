/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import javax.ws.rs.container.ResourceInfo;

import io.helidon.common.types.ElementSignature;
import io.helidon.common.types.TypeName;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.ServiceInfo;
import io.helidon.webserver.http.HttpEntryPoint;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SplatMtlsEndpointInterceptorTest {

    @Test
    void shouldProceedWhenRequestHandlerAllowsRequest() throws Exception {
        SplatMtlsRequestHandler requestHandler = mock(SplatMtlsRequestHandler.class);
        when(requestHandler.shouldAllow(any(), any(), any())).thenReturn(true);
        SplatMtlsEndpointInterceptor interceptor = SplatMtlsEndpointInterceptor.createForTesting(requestHandler);
        HttpEntryPoint.Interceptor.Chain chain = mock(HttpEntryPoint.Interceptor.Chain.class);
        ServerRequest request = mock(ServerRequest.class);
        ServerResponse response = mock(ServerResponse.class);

        interceptor.proceed(mockInterceptionContext(), chain, request, response);

        verify(chain).proceed(request, response);
    }

    @Test
    void shouldStopChainWhenRequestHandlerRejectsRequest() throws Exception {
        SplatMtlsRequestHandler requestHandler = mock(SplatMtlsRequestHandler.class);
        when(requestHandler.shouldAllow(any(), any(), any())).thenReturn(false);
        SplatMtlsEndpointInterceptor interceptor = SplatMtlsEndpointInterceptor.createForTesting(requestHandler);
        HttpEntryPoint.Interceptor.Chain chain = mock(HttpEntryPoint.Interceptor.Chain.class);

        interceptor.proceed(mockInterceptionContext(), chain, mock(ServerRequest.class), mock(ServerResponse.class));

        verify(chain, never()).proceed(any(), any());
    }

    @Test
    void shouldPassEndpointResourceInfoToRequestHandler() throws Exception {
        SplatMtlsRequestHandler requestHandler = mock(SplatMtlsRequestHandler.class);
        when(requestHandler.shouldAllow(any(), any(), any())).thenReturn(true);
        SplatMtlsEndpointInterceptor interceptor = SplatMtlsEndpointInterceptor.createForTesting(requestHandler);
        ArgumentCaptor<ResourceInfo> resourceInfoCaptor = ArgumentCaptor.forClass(ResourceInfo.class);

        interceptor.proceed(mockInterceptionContext(),
                            mock(HttpEntryPoint.Interceptor.Chain.class),
                            mock(ServerRequest.class),
                            mock(ServerResponse.class));

        verify(requestHandler).shouldAllow(any(), any(), resourceInfoCaptor.capture());
        ResourceInfo resourceInfo = resourceInfoCaptor.getValue();
        assertEquals(EndpointSampleService.class, resourceInfo.getResourceClass());
        assertEquals("doNothing", resourceInfo.getResourceMethod().getName());
    }

    private static InterceptionContext mockInterceptionContext() {
        InterceptionContext interceptionContext = mock(InterceptionContext.class);
        ServiceInfo serviceInfo = mock(ServiceInfo.class);
        io.helidon.common.types.TypedElementInfo elementInfo = mock(io.helidon.common.types.TypedElementInfo.class);
        ElementSignature signature = ElementSignature.createMethod(TypeName.create(void.class), "doNothing", java.util.List.of());

        when(interceptionContext.serviceInfo()).thenReturn(serviceInfo);
        when(interceptionContext.elementInfo()).thenReturn(elementInfo);
        when(serviceInfo.serviceType()).thenReturn(TypeName.create(EndpointSampleService.class));
        when(elementInfo.signature()).thenReturn(signature);
        return interceptionContext;
    }

}

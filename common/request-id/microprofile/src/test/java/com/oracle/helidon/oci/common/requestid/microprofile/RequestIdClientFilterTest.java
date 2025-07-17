/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.requestid.microprofile;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;

import com.oracle.helidon.oci.common.requestid.OciRequestId;
import com.oracle.pic.commons.rid.RequestIdUtils;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

class RequestIdClientFilterTest {

    @Test
    void testRequestIdDownstream() {
        String requestId = "00000/11111/22222";
        Context context = Context.create();
        context.register(OciRequestId.parseUpstreamRequest(requestId));

        Contexts.runInContext(context, () -> {
            MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
            ClientRequestContext requestContext = newRequestContext(map);
            RequestIdClientFilter filter = new RequestIdClientFilter();
            filter.filter(requestContext);
            assertThat(RequestIdUtils.getDownStreamRequestId(requestId), startsWith(map.getFirst(OciRequestId.OCI_REQUEST_ID).toString()));
        });
    }

    private ClientRequestContext newRequestContext(MultivaluedMap<String, Object> map) {
        ClientRequestContext requestContext = Mockito.mock(ClientRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(map == null ? new MultivaluedHashMap<>() : map);
        return requestContext;
    }
}

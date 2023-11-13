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

import com.oracle.pic.commons.rid.RequestIdUtils;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import javax.ws.rs.client.ClientRequestContext;;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

class RequestIdClientFilterTest {

    @Test
    void testRequestIdDownstream() {
        String requestId = "00000/11111/22222";
        Context context = Context.create();
        context.register(Request.class, requestId);

        Contexts.runInContext(context, () -> {
            MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
            ClientRequestContext requestContext = newRequestContext(map);
            RequestIdClientFilter filter = new RequestIdClientFilter();
            filter.filter(requestContext);
            assertThat(map.getFirst(OciHeaderNames.OPC_REQUEST_ID), is(RequestIdUtils.getDownStreamRequestId(requestId)));
        });
    }

    private ClientRequestContext newRequestContext(MultivaluedMap<String, Object> map) {
        ClientRequestContext requestContext = Mockito.mock(ClientRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(map == null ? new MultivaluedHashMap<>() : map);
        return requestContext;
    }
}

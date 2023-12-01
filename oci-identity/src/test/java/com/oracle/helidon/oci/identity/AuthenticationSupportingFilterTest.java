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

package com.oracle.helidon.oci.identity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.oracle.helidon.oci.identity.AuthenticationSupportingFilter.Configuration;
import static com.oracle.helidon.oci.identity.AuthenticationSupportingFilter.TAG_DEFAULT_HEADER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthenticationSupportingFilterTest {

    static final String TEST_INPUT = "Hello Joe";

    @Test
    void testRequestFilter_GET_withBody() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        ContainerRequestContext rc = newRequestContext("/", HttpMethod.GET, TEST_INPUT, map);

        AuthenticationSupportingFilter filter = new AuthenticationSupportingFilter();
        filter.filter(rc);
        assertThat(map.containsKey(TAG_DEFAULT_HEADER),
                   is(true));
        assertThat(map.getFirst(TAG_DEFAULT_HEADER),
                   equalTo("KFl7HCWzS/TNRlIt6rej3jzJBQ0pttRghDf7ZP/wpBM="));
    }

    @Test
    void testRequestFilter_GET_withNoBody() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        ContainerRequestContext rc = newRequestContext("/", HttpMethod.GET, null, map);

        AuthenticationSupportingFilter filter = new AuthenticationSupportingFilter();
        filter.filter(rc);
        assertThat(map.containsKey(TAG_DEFAULT_HEADER),
                   is(false));
    }

    @Test
    void testRequestFilter_POST_withBody() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        ContainerRequestContext rc = newRequestContext("/", HttpMethod.POST, TEST_INPUT, map);

        AuthenticationSupportingFilter filter = new AuthenticationSupportingFilter();
        filter.filter(rc);
        assertThat(map.containsKey(TAG_DEFAULT_HEADER),
                   is(true));
        assertThat(map.getFirst(TAG_DEFAULT_HEADER),
                   equalTo("KFl7HCWzS/TNRlIt6rej3jzJBQ0pttRghDf7ZP/wpBM="));
    }

    @Test
    void matchTest() {
        Configuration config = Mockito.mock(Configuration.class);
        when(config.uriPrefix()).thenReturn(List.of("/path/0", "/path/1"));
        AuthenticationSupportingFilter filter = new AuthenticationSupportingFilter(config);

        assertThat(filter.matches("/whatever"), is(false));
        assertThat(filter.matches("/path/1"), is(true));
        assertThat(filter.matches("/path/1/more"), is(true));
        assertThat(filter.matches("/"), is(false));
    }

    @Test
    void configTest() {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("memoryThreshold", "1",
                                                     "useEncryption", "true",
                                                     "headerTag", "tag",
                                                     "uriPrefix.0", "/v1/")))
                .build();
        AuthenticationSupportingFilter filter = new AuthenticationSupportingFilter(config);
        assertThat(filter.configuration().uriPrefix(), equalTo(List.of("/v1/")));
        assertThat(filter.configuration().headerTag(), equalTo("tag"));

        RepeatableInputStreamer.Stream stream = filter.createRepeatableStream(new ByteArrayInputStream("".getBytes()));
        RepeatableInputStreamer.Configuration rsConfig = stream.configuration();
        assertThat(rsConfig.useEncryption(), is(true));
        assertThat(rsConfig.memoryThreshold(), is(1));
        assertThat(rsConfig.streamThreshold(), is(RepeatableInputStreamer.DEFAULT_BYTES_MAX));
    }

    private ContainerRequestContext newRequestContext(String uri,
                                                      String method,
                                                      String body,
                                                      MultivaluedMap<String, String> headerMap) {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create(uri));

        ContainerRequestContext rc = Mockito.mock(ContainerRequestContext.class);
        when(rc.getUriInfo()).thenReturn(uriInfo);
        when(rc.getMethod()).thenReturn(method);
        when(rc.hasEntity()).thenReturn(body != null);
        when(rc.getEntityStream()).thenReturn(body != null ? new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)) : null);
        when(rc.getHeaders()).thenReturn(headerMap == null ? new MultivaluedHashMap<>() : headerMap);
        Mockito.doAnswer((i) -> {
            assertThat(new String(((InputStream) i.getArgument(0)).readAllBytes(), StandardCharsets.UTF_8),
                       equalTo(TEST_INPUT));
            return null;
        }).when(rc).setEntityStream(any(InputStream.class));
        return rc;
    }

}

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

import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JakartaServerFilter;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonTest
@AddBean(JavaxServerFilterTest.JavaxTestFilter.class)
@Path("/test")
public class JavaxServerFilterTest {

    public static final String TEST_VALUE_FROM_FILTER = "test-value-from-filter";
    public static final String TEST_HEADER = "test-header";
    public static final String PATH = "/test";

    @Inject
    private WebTarget target;

    // called by Jersey's auto-discoverable
    // register server filter here
    static void configure(jakarta.ws.rs.core.FeatureContext ctx) {
        ctx.register(new JakartaServerFilter(JavaxServerFilterTest.JavaxTestFilter.class));
    }

    @GET
    public String testGet(@HeaderParam(TEST_HEADER) String testHeader) {
        return testHeader;
    }

    @Test
    void extraHeaderTest() {
        try (var res = target.path(PATH).request().get()) {
            assertThat(res.readEntity(String.class), is(TEST_VALUE_FROM_FILTER));
        }
    }

    @Test
    void filterForbiddenErrorTest() {
        try (var res = target.path(PATH)
                .request()
                .header("throw", "javax.ws.rs.ForbiddenException")
                .get()) {
            assertThat(res.getStatus(), is(403));
        }
    }

    @Test
    void filterNotFoundErrorTest() {
        try (var res = target.path(PATH)
                .request()
                .header("throw", "javax.ws.rs.NotFoundException")
                .get()) {
            assertThat(res.getStatus(), is(404));
        }
    }

    @Test
    void filterBadRequestErrorTest() {
        try (var res = target.path(PATH)
                .request()
                .header("throw", "javax.ws.rs.BadRequestException")
                .get()) {
            assertThat(res.getStatus(), is(400));
        }
    }

    @Test
    void filterNotSupportedErrorTest() {
        try (var res = target.path(PATH)
                .request()
                .header("throw", "javax.ws.rs.NotSupportedException")
                .get()) {
            assertThat(res.getStatus(), is(415));
        }
    }

    public static class JavaxTestFilter implements javax.ws.rs.container.ContainerRequestFilter {

        @Override
        public void filter(javax.ws.rs.container.ContainerRequestContext ctx) throws IOException {
            ctx.getHeaders().add(TEST_HEADER, TEST_VALUE_FROM_FILTER);

            String exceptionFqdn = ctx.getHeaders().getFirst("throw");
            if (exceptionFqdn != null) {
                try {
                    Class<?> exception = Thread.currentThread().getContextClassLoader().loadClass(exceptionFqdn);
                    throw (RuntimeException) exception.getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

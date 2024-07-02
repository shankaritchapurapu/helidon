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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaxWebApplicationExceptionTest {

    public static final String TEST_MESSAGE = "Test Message!";
    public static final int I_AM_A_TEAPOT = 418;

    @Test
    void forbiddenExceptionTest() throws IOException {
        ForbiddenException e = Assertions.assertThrows(ForbiddenException.class,
                                                       () -> new JakartaServerFilter(new JavaxForbiddenTestFilter())
                                                               .filter(null));
        assertThat(e, notNullValue());
        assertThat(e.getMessage(), is(TEST_MESSAGE));
        assertThat(e, instanceOf(jakarta.ws.rs.ForbiddenException.class));
        assertThat(e.getSuppressed().length, is(1));
        assertThat(e.getSuppressed()[0], instanceOf(javax.ws.rs.ForbiddenException.class));
        assertThat(e.getResponse().getStatus(), is(403));
    }

    @Test
    void customExceptionTest() throws IOException {
        WebApplicationException e = Assertions.assertThrows(WebApplicationException.class,
                                                            () -> new JakartaServerFilter(new JavaxCustomExceptionTestFilter())
                                                                    .filter(null));
        assertThat(e, notNullValue());
        assertThat(e.getMessage(), is(TEST_MESSAGE));
        assertThat(e, instanceOf(jakarta.ws.rs.WebApplicationException.class));
        assertThat(e.getSuppressed().length, is(1));
        assertThat(e.getSuppressed()[0], instanceOf(CustomWebApplicationException.class));
        assertThat(e.getResponse().getStatus(), is(I_AM_A_TEAPOT));
    }

    public static class JavaxForbiddenTestFilter implements javax.ws.rs.container.ContainerRequestFilter {
        @Override
        public void filter(javax.ws.rs.container.ContainerRequestContext ctx) throws IOException {
            throw new javax.ws.rs.ForbiddenException(TEST_MESSAGE);
        }
    }

    public static class JavaxCustomExceptionTestFilter implements javax.ws.rs.container.ContainerRequestFilter {
        @Override
        public void filter(javax.ws.rs.container.ContainerRequestContext ctx) throws IOException {
            throw new CustomWebApplicationException(TEST_MESSAGE);
        }
    }

    public static class CustomWebApplicationException extends javax.ws.rs.WebApplicationException {
        public CustomWebApplicationException(String message) {
            super(message, I_AM_A_TEAPOT);
        }
    }
}

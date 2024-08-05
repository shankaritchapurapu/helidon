/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim.shim;

import java.io.IOException;

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JakartaServerFilter;
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

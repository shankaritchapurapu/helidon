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

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

public class JakartaServerFilter implements ContainerRequestFilter {

    private final javax.ws.rs.container.ContainerRequestFilter delegate;

    public JakartaServerFilter(javax.ws.rs.container.ContainerRequestFilter delegate) {
        this.delegate = delegate;
    }

    public JakartaServerFilter(Class<? extends javax.ws.rs.container.ContainerRequestFilter> delegateClass) {
        try {
            this.delegate = delegateClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            delegate.filter(new JavaxContainerRequestContext(requestContext));
        } catch (javax.ws.rs.BadRequestException e) {
            throw jakartify(e, (m, r) -> new BadRequestException(m, r, e.getCause()));
        } catch (javax.ws.rs.ForbiddenException e) {
            throw jakartify(e, (m, r) -> new ForbiddenException(m, r, e.getCause()));
        } catch (javax.ws.rs.InternalServerErrorException e) {
            throw jakartify(e, (m, r) -> new InternalServerErrorException(m, r, e.getCause()));
        } catch (javax.ws.rs.NotAcceptableException e) {
            throw jakartify(e, (m, r) -> new NotAcceptableException(m, r, e.getCause()));
        } catch (javax.ws.rs.NotAllowedException e) {
            throw jakartify(e, (m, r) -> new NotAllowedException(m, r, e.getCause()));
        } catch (javax.ws.rs.NotAuthorizedException e) {
            throw jakartify(e, (m, r) -> new NotAuthorizedException(m, r, e.getCause()));
        } catch (javax.ws.rs.NotFoundException e) {
            throw jakartify(e, (m, r) -> new NotFoundException(m, r, e.getCause()));
        } catch (javax.ws.rs.NotSupportedException e) {
            throw jakartify(e, (m, r) -> new NotSupportedException(m, r, e.getCause()));
        } catch (javax.ws.rs.RedirectionException e) {
            throw jakartify(e, RedirectionException::new);
        } catch (javax.ws.rs.ServiceUnavailableException e) {
            throw jakartify(e, (m, r) -> new ServiceUnavailableException(m, r, e.getCause()));
        } catch (javax.ws.rs.ServerErrorException e) {
            throw jakartify(e, (m, r) -> new ServerErrorException(m, r, e.getCause()));
        } catch (javax.ws.rs.ClientErrorException e) {
            throw jakartify(e, (m, r) -> new ClientErrorException(m, r, e.getCause()));
        } catch (javax.ws.rs.WebApplicationException javaxException) {
            var jakartaShim = new JakartaWebApplicationException(javaxException);
            jakartaShim.addSuppressed(javaxException);
            throw jakartaShim;
        }
    }

    private WebApplicationException jakartify(javax.ws.rs.WebApplicationException javaxException,
                                              BiFunction<String, Response, WebApplicationException> fnc) {
        var jakartaException = fnc.apply(javaxException.getMessage(), new JakartaResponse(javaxException.getResponse()));
        jakartaException.addSuppressed(javaxException);
        return jakartaException;
    }
}

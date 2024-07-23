/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.common.errorcode.microprofile;

import java.util.Map;

import io.helidon.http.Status;
import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.oracle.helidon.oci.common.errorcode.ErrorCodes;
import com.oracle.helidon.oci.common.errorcode.ErrorDetail;
import com.oracle.helidon.oci.common.errorcode.RenderableException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalEmpty;
import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@HelidonTest
@AddBean(MpErrorCodeTest.TestResource.class)
class MpErrorCodeTest {

    @Inject
    private WebTarget webTarget;

    @Test
    void testError() {
        Response response = webTarget.path("error").request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        ErrorDetail errorDetail = response.readEntity(ErrorDetail.class);
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.InvalidParameter.errorCode()));
        assertThat(errorDetail.getMessage(), is("Invalid parameter sent"));
    }

    @Test
    void testError2() {
        Response response = webTarget.path("error2").request().get();
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        ErrorDetail errorDetail = response.readEntity(ErrorDetail.class);
        assertThat(errorDetail.getErrorCode(), is(ErrorCodes.NotAuthenticated.errorCode()));
        assertThat(errorDetail.getMessage(), is("User 'helidon' is not authenticated"));
        assertThat(errorDetail.getOriginalMessage(), is("Authentication failure for 'helidon'"));
        assertThat(errorDetail.getOriginalMessageTemplate(), is("Authentication failure for '{user}'"));
        assertThat(errorDetail.getMessageArguments(), is(Map.of("user", "helidon")));
    }

    @Test
    void testErrorMp() {
        TestResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(webTarget.getUri())
                .build(TestResourceClient.class);
        try (Response response = client.error()) {
            fail("Response was not mapped to exception " + response);
        } catch (RenderableException e) {
            assertThat(e.errorCode().status().code(), is(Status.BAD_REQUEST_400.code()));
            assertThat(e.errorCode(), is(ErrorCodes.InvalidParameter));
            assertThat(e.getMessage(), is("Invalid parameter sent"));
            assertThat(e.originalMessage(), optionalEmpty());
            assertThat(e.originalMessageTemplate(), optionalEmpty());
            assertThat(e.messageArguments(), is(Map.of()));
        }
    }

    @Test
    void testErrorMp2() {
        TestResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(webTarget.getUri())
                .build(TestResourceClient.class);
        try (Response response = client.error2()) {
            fail("Response was not mapped to exception " + response);
        } catch (RenderableException e) {
            assertThat(e.errorCode().status().code(), is(Status.UNAUTHORIZED_401.code()));
            assertThat(e.errorCode(), is(ErrorCodes.NotAuthenticated));
            assertThat(e.getMessage(), is("User 'helidon' is not authenticated"));
            assertThat(e.originalMessage(), optionalValue(is("Authentication failure for 'helidon'")));
            assertThat(e.originalMessageTemplate(), optionalValue(is("Authentication failure for '{user}'")));
            assertThat(e.messageArguments(), is(Map.of("user", "helidon")));
        }
    }

    @Path("/")
    @RegisterProvider(ErrorCodeResponseMapper.class)        // maps to RenderableException
    public interface TestResourceClient {

        @GET
        @Path("error")
        Response error() throws RenderableException;

        @GET
        @Path("error2")
        Response error2() throws RenderableException;
    }

    @Path("/")
    public static class TestResource {

        @GET
        @Path("error")
        public Response error() {
            throw new RenderableException(ErrorCodes.InvalidParameter, "Invalid parameter sent");
        }

        @GET
        @Path("error2")
        public Response error2() {
            throw new RenderableException(ErrorCodes.NotAuthenticated,
                                          "User 'helidon' is not authenticated",
                                          "Authentication failure for 'helidon'",
                                          "Authentication failure for '{user}'",
                                          Map.of("user", "helidon"));
        }
    }
}

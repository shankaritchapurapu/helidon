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
package com.oracle.helidon.oci.tests.integration.requestid;

import java.net.URI;
import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import static com.oracle.helidon.oci.common.requestid.OciRequestId.OCI_REQUEST_ID;

@Path("/oci")
public class OciResource {
    private static final Logger LOGGER = Logger.getLogger(OciResource.class.getName());

    @Context
    private UriInfo uriInfo;

    @GET
    public Response test(@HeaderParam(OCI_REQUEST_ID) String upstreamRequestId) {
        // this request id is the full id with a new generated span id
        LOGGER.info("Resource /oci");
        return Response.ok(upstreamRequestId).build();
    }

    @GET
    @Path("client")
    public Response testClient(@HeaderParam(OCI_REQUEST_ID) String upstreamRequestId) {
        LOGGER.info("Resource /oci/client");
        Client client = ClientBuilder.newBuilder().build();
        URI uri = uriInfo.getBaseUriBuilder().path("oci").build();       // self get
        Response downstreamResponse = client.target(uri).request().get();

        return Response.ok()
                .header("x-oci-upstream", upstreamRequestId) // received from remote call (our span id)
                // returned from remote call (their span id)
                .header("x-oci-downstream-request", downstreamResponse.readEntity(String.class))
                // returned from remote call (their span id)
                .header("x-oci-downstream-response", downstreamResponse.getHeaderString(OCI_REQUEST_ID))
                .build();
    }

    @GET
    @Path("mpclient")
    public Response testMpClient(@HeaderParam(OCI_REQUEST_ID) String upstreamRequestId) {
        LOGGER.info("Resource /oci/mpclient");
        OciResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uriInfo.getBaseUri())
                .build(OciResourceClient.class);
        Response downstreamResponse = client.test();       // self get
        return Response.ok()
                .header("x-oci-upstream", upstreamRequestId) // received from remote call (our span id)
                // returned from remote call (their span id)
                .header("x-oci-downstream-request", downstreamResponse.readEntity(String.class))
                // returned from remote call (their span id)
                .header("x-oci-downstream-response", downstreamResponse.getHeaderString(OCI_REQUEST_ID))
                .build();
    }

    @Path("/oci")
    public interface OciResourceClient {
        @GET
        Response test();
    }
}

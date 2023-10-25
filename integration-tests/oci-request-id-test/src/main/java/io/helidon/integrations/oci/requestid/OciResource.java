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
package io.helidon.integrations.oci.requestid;

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

import static io.helidon.integrations.oci.requestid.OciHeaderNames.OPC_REQUEST_ID;

@Path("/oci")
public class OciResource {
    private static final Logger LOGGER = Logger.getLogger(OciResource.class.getName());

    @Context
    private UriInfo uriInfo;

    @GET
    public Response test(@HeaderParam(OPC_REQUEST_ID) String requestId) {
        LOGGER.info("Resource /oci");
        return Response.ok(requestId).build();
    }

    @GET
    @Path("client")
    public Response testClient() {
        LOGGER.info("Resource /oci/client");
        Client client = ClientBuilder.newBuilder().build();
        URI uri = uriInfo.getBaseUriBuilder().path("oci").build();       // self get
        Response response = client.target(uri).request().get();
        String requestId = response.readEntity(String.class);
        return Response.ok(requestId).build();
    }

    @GET
    @Path("mpclient")
    public Response testMpClient() {
        LOGGER.info("Resource /oci/mpclient");
        OciResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uriInfo.getBaseUri())
                .build(OciResourceClient.class);
        return client.test();       // self get
    }

    @Path("/oci")
    public interface OciResourceClient {
        @GET
        Response test();
    }
}

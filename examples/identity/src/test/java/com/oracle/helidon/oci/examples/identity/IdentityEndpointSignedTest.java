/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.identity;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.http.signing.SigningStrategy;
import com.oracle.bmc.http.signing.internal.DefaultRequestSignerFactory;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * There are the pre-requisites to run this test:
 *
 * <li>You need an entry in your ~/.oci/config file (profile) name API_KEY. Something
 * like this:
 * <pre>
 * [API_KEY]
 * fingerprint = 92:f8:66:...
 * key_file = /Users/spericas/.oci/private.pem
 * tenancy = ocid1.tenancy.oc1..aaaaaaaa...
 * region = us-ashburn-1
 * user = ocid1.user.oc1..aaaaaaaa...
 * </pre>
 * </li>
 *
 * In addition, if not running in an OCI instance, you need to set up an SSH tunnel
 * to get the certificates for instance principal authentication. For example:
 *
 * <pre>
 *  ssh -L 8000:169.254.169.254:80 oci-reference-infra-ad1 -t watch -n 90 date
 * </pre>
 *
 * Finally, using the tunnel shown above, you need to make sure to set {@link
 * com.oracle.helidon.oci.identity.AuthenticationConfig#instancePrincipalUri()}
 * to {@code http://localhost:8080}. See config in application.yaml.
 */
@ServerTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IdentityEndpointSignedTest extends IdentityEndpointBase {

    public IdentityEndpointSignedTest(WebServer webServer) {
        super(webServer);
    }

    @Test
    @Order(1)
    void testPing() throws Exception {
        testPing(Status.OK_200.code(), "pong");
    }

    @Test
    @Order(2)
    void testOnceSuccess() throws Exception {
        testOnceSuccess(Status.OK_200.code(), "Hello World");
    }

    @Test
    @Order(3)
    void testTwiceSuccess() throws Exception {
        testTwiceSuccess(Status.OK_200.code(), "Hello WorldHello World");
    }

    Map<String, String> signRequest(URI uri,
                                    String httpMethod,
                                    Map<String, List<String>> headers,
                                    Object body) throws IOException {
        ConfigFileReader.ConfigFile config = ConfigFileReader.parse(ConfigFileReader.DEFAULT_FILE_PATH, "API_KEY");
        var provider = new ConfigFileAuthenticationDetailsProvider(config);
        var signerFactory = new DefaultRequestSignerFactory(SigningStrategy.STANDARD);
        RequestSigner signer = signerFactory.createRequestSigner(null, provider);
        return signer.signRequest(uri, httpMethod, headers, body);
    }
}

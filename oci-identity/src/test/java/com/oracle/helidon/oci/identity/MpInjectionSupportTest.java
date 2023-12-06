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

import java.util.Collection;
import java.util.Map;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.control.RequestContextController;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.client.WebTarget;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.microprofile.tests.junit5.HelidonTest;
import io.helidon.security.Principal;
import io.helidon.security.SecurityContext;
import io.helidon.security.Subject;

import com.oracle.pic.identity.authentication.AuthenticatorClient;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.supplier.InstancePrincipalCertificateSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

//@Disabled
@HelidonTest
public class MpInjectionSupportTest {

    @Inject
    private RequestContextController rcc;

    @Inject
    private WebTarget target;

    @Inject
    private SecurityContext sc;

    @Inject
    private Provider<Subject> subjectProvider;

    @Inject
    private Provider<com.oracle.pic.identity.authentication.Principal> principalProvider;

    @Inject
    private Provider<com.oracle.pic.identity.authorization.sdk.AuthorizationRequest> authorizationRequestProvider;

    @Inject
    private AuthenticatorClient authenticatorClient;

    @Inject
    private ServiceAuthenticationClient serviceAuthenticationClient;

    @Inject
    private InstancePrincipalCertificateSupplier instancePrincipalCertificateSupplier;

    @BeforeAll
    static void overrideConfig() {
        Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("authEnabled", "false",
                                                     "rootCertPath", "target/test-classes/secrets/ca.cert.pem")))
                .build();
        OciIdentityConfiguration.AuthConfig authConfig = new OciIdentityConfiguration.AuthConfig(config);
        if (!authConfig.rootCertFilePath().isPresent()) {
            throw new IllegalStateException("Expected cert to be present: " + authConfig.rootCertPath());
        }
        OciIdentityConfiguration.authConfigOverride(authConfig);
    }

    @AfterAll
    static void reset() {
        OciIdentityConfiguration.authConfigOverride(null);
    }

    @Test
    void sanity() {
        assertThat(rcc, notNullValue());
        assertThat(target, notNullValue());
        assertThat(sc, notNullValue());
        assertThat(subjectProvider, notNullValue());
        assertThat(authorizationRequestProvider, notNullValue());
        assertThat(principalProvider, notNullValue());
    }

    @Test
    void testEmptySubject() {
        Subject s = Subject.builder().build();
        assertThat(s.abacAttributeNames().size(), is(2));
        assertThat(s.abacAttributeNames().contains("principal"), is(true));
        assertThat(s.abacAttributeRaw("principal"), is(nullValue()));
        assertThat(s.abacAttributeNames().contains("grant"), is(true));
        assertThat(s.abacAttributeRaw("grant"), is(instanceOf(Collection.class)));
        assertThat(((Collection<?>) s.abacAttributeRaw("grant")).isEmpty(), is(true));
        assertThat(s.principal(), is(nullValue()));
        assertThat(s.principals().isEmpty(), is(true));
    }

    @Test
    void testEmptyPrincipal() {
        Principal p = Principal.builder().build();
        assertThat(p.id(), nullValue());
        assertThat(p.getName(), nullValue());
        assertThat(p.abacAttributeNames().size(), is(2));
        assertThat(p.abacAttributeNames().contains("id"), is(true));
        assertThat(p.abacAttributeRaw("id"), nullValue());
        assertThat(p.abacAttributeNames().contains("name"), is(true));
        assertThat(p.abacAttributeRaw("name"), nullValue());
    }

    @Test
    void testInjectedWhenNoRequestScopeIsActive() {
        assertThrows(ContextNotActiveException.class, this.subjectProvider::get);

        // TODO: How is this not throwing - since we are not in a request scoped context?
//        assertThrows(IllegalStateException.class, this.authorizationRequestProvider::get);
//        assertThrows(ContextNotActiveException.class, this.principalProvider::get);
    }

    @Test
    void testInjectedSubjectWhenRequestIsActive() {
        try {
            rcc.activate();
            Subject s = this.subjectProvider.get();
            assertThat(s, not(nullValue()));
            Object r = this.authorizationRequestProvider.get();
            assertThat(r, not(nullValue()));
            Object p = this.principalProvider.get();
            assertThat(p, not(nullValue()));
        } finally {
            rcc.deactivate();
        }
    }

}

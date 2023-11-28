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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import io.helidon.security.SecurityContext;
import io.helidon.security.Subject;

import com.oracle.pic.identity.authentication.PrincipalType;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequestFactory;

/**
 * Provides the bridge from OCI injectable identity types into CDI.
 */
@ApplicationScoped
public class MpInjectionSupport {
    static final Subject EMPTY_SUBJECT = Subject.builder().build();

    private MpInjectionSupport() {
    }

    // Subject.class is final so can't be proxied so can't be @RequestScoped.
    @Produces
    @Default
    @Dependent
    private static Subject produceDefaultSubject(SecurityContext sc) {
        // TODO:
        return sc.service().orElse(EMPTY_SUBJECT);
    }

    // Subject.class is final so can't be proxied so can't be @RequestScoped.
    @Produces
    @Service
    @Dependent
    private static Subject produceContextualSubject(SecurityContext sc) {
        // TODO:
        return sc.service().orElse(EMPTY_SUBJECT);
    }

    @Produces
    @Default
    @Dependent
    public static com.oracle.pic.identity.authentication.Principal produceDefaultPrincipal(SecurityContext sc) {
        // TODO:
        return com.oracle.pic.authproxy.AuthProxyAnonymousPrincipal.builder().build();
    }

    @Produces
    @Dependent
    @Service
    public static com.oracle.pic.identity.authentication.Principal produceContextualPrincipal(SecurityContext sc/*,
            InjectionPoint ip*/) {
        // TODO:
        return com.oracle.pic.authproxy.AuthProxyAnonymousPrincipal.builder().build();
    }


    @Produces
    @Default
    @Dependent
    public static com.oracle.pic.identity.authorization.sdk.AuthorizationRequest produceDefaultAuthorizationRequest(SecurityContext sc) {
        // TODO:
        return AuthorizationRequestFactory.serviceRequest(PrincipalType.SERVICE.name(), produceDefaultPrincipal(sc));
    }

    @Produces
    @Dependent
    @Service
    public static com.oracle.pic.identity.authorization.sdk.AuthorizationRequest produceContextualAuthorizationRequest(SecurityContext sc/*,
                                                                                                             InjectionPoint ip*/) {
        // TODO:
        return AuthorizationRequestFactory.serviceRequest(PrincipalType.SERVICE.name(), produceContextualPrincipal(sc));
    }

}

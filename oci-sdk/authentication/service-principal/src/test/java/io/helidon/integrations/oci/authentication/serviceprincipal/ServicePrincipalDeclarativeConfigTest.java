/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.serviceprincipal;

import java.net.URI;

import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ServicePrincipalDeclarativeConfigTest {
    private ServiceRegistryManager registryManager;

    @AfterEach
    void shutdownServices() {
        if (registryManager != null) {
            registryManager.shutdown();
        }
    }

    @Test
    void loadsServicePrincipalSettingsFromOciConfig() {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());

        OciConfig ociConfig = Services.get(OciConfig.class);
        ServicePrincipalMethodConfig servicePrincipalConfig = Services.get(ServicePrincipalMethodConfig.class);

        assertThat(ociConfig.authenticationMethod(), is("service-principal"));
        assertThat(ociConfig.imdsBaseUri().orElseThrow(),
                   is(URI.create("http://instance-metadata.svc.ad1.r1/opc/v2/")));
        assertThat(ociConfig.federationEndpoint().orElseThrow(),
                   is(URI.create("https://authservice-alpha-preprod.svc.ad2.r1")));
        assertThat(ociConfig.tenantId().orElseThrow(), is("ocid1.tenancy.oc1..testserviceprincipal"));

        assertThat(servicePrincipalConfig.useInstancePrincipal(), is(false));
        assertThat(servicePrincipalConfig.certificates().size(), is(2));

        ServicePrincipalCertificateConfig leafCertificate = servicePrincipalConfig.certificates().getFirst();
        assertThat(leafCertificate.certificate(), is("cert.pem"));
        assertThat(leafCertificate.privateKey().orElseThrow(), is("key.pem"));
        assertThat(leafCertificate.passphrase(), is(""));

        ServicePrincipalCertificateConfig intermediateCertificate = servicePrincipalConfig.certificates().get(1);
        assertThat(intermediateCertificate.certificate(), is("intermediates.pem"));
        assertThat(intermediateCertificate.privateKey().isEmpty(), is(true));
        assertThat(intermediateCertificate.passphrase(), is(""));
    }
}

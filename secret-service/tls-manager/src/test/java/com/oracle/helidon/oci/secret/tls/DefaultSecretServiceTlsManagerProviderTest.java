/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.util.Map;

import io.helidon.common.Errors;
import io.helidon.common.tls.Tls;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSecretServiceTlsManagerProviderTest {
    @Test
    void createsManagerFromTlsConfig() {
        Tls tls = Tls.create(config(Map.of(
                "endpoint-identification-algorithm", "NONE",
                "manager.oci-ssv2.reload.enabled", "false",
                "manager.oci-ssv2.pki.resource.path", "src/test/resources/mtls/client-pki.json",
                "manager.oci-ssv2.trust.path", "src/test/resources/mtls/ca.pem"
        )));

        assertThat(tls.prototype().manager(), instanceOf(SecretServiceTlsManager.class));
        assertThat(tls.sslContext(), is(tls.prototype().manager().sslContext()));
    }

    @Test
    void createsIndependentManagersForEquivalentConfig() {
        Map<String, String> values = Map.of(
                "endpoint-identification-algorithm", "NONE",
                "manager.oci-ssv2.reload.enabled", "false",
                "manager.oci-ssv2.pki.resource.path", "src/test/resources/mtls/client-pki.json",
                "manager.oci-ssv2.trust.path", "src/test/resources/mtls/ca.pem"
        );

        Tls first = Tls.create(config(values));
        Tls second = Tls.create(config(values));

        assertThat(first.prototype().manager(), not(sameInstance(second.prototype().manager())));
    }

    @Test
    void disabledTlsDoesNotRequireManagerMaterial() {
        Tls tls = Tls.create(config(Map.of(
                "enabled", "false"
        )));

        assertThat(tls.enabled(), is(false));
    }

    @Test
    void explicitManagerConfigRequiresPki() {
        Errors.ErrorMessagesException thrown = assertThrows(Errors.ErrorMessagesException.class,
                                                            () -> Tls.create(config(Map.of(
                                                                    "enabled", "false",
                                                                    "manager.oci-ssv2.reload.enabled", "false"
                                                            ))));

        assertThat(thrown.getMessage().contains("Property \"pki\" must not be null, but not set"), is(true));
    }

    private static Config config(Map<String, String> values) {
        return Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(values))
                .build();
    }

}

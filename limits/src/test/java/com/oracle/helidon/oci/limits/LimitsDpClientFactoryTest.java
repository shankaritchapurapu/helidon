/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.limits;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.integrations.oci.OciConfig;
import io.helidon.service.registry.Services;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader.ConfigFile;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.S2SAuthenticationDetailsProvider;
import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalAuthConfig;
import com.oracle.helidon.oci.sdk.common.core.ServicePrincipalCertificateConfig;
import com.oracle.oci.limits.LimitsDPClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitsDpClientFactoryTest {

    @Test
    void limitsDPClient() {
        LimitsDPClient client = Services.get(LimitsDPClient.class);
        assertNotNull(client, "LimitsDPClient instance should not be null");
    }

    @Test
    void config() {
        LimitsConfig config = Services.get(LimitsConfig.class);
        assertNotNull(config, "LimitsConfig instance should not be null");
        ClientConfiguration client = config.client().orElseThrow();
        assertEquals(5000, client.getConnectionTimeoutMillis(), "Connection timeout should match");
        assertEquals(30000, client.getReadTimeoutMillis(), "Read timeout should match");
        assertEquals(20, client.getMaxAsyncThreads(), "Max async threads should match");
        assertEquals("service-principal", config.auth().orElseThrow().authenticationMethod(),
                     "Auth method should match");
    }

    @Test
    void scopedAuthConfig() {
        LimitsAuthConfig auth = scopedLimitsConfig().auth().orElseThrow();
        ServicePrincipalAuthConfig servicePrincipal = auth.servicePrincipal().orElseThrow();
        assertEquals("service-principal", auth.authenticationMethod(), "Auth method should match");
        assertEquals("ocid1.tenancy.oc1..testserviceprincipal", servicePrincipal.tenantId().orElseThrow(),
                     "Tenant should match");
        assertEquals(2, servicePrincipal.certificates().size(), "Certificates should match");
    }

    @Test
    void limitsScopedAuthProvider() {
        BasicAuthenticationDetailsProvider provider = LimitsAuthProviderFactory.authProvider(
                scopedLimitsConfig(),
                Services.get(OciConfig.class),
                Optional::empty);

        assertInstanceOf(S2SAuthenticationDetailsProvider.class, provider);
    }

    @Test
    void limitsAuthProviderIsNotGlobalOciAuthProvider() {
        BasicAuthenticationDetailsProvider provider = Services.get(BasicAuthenticationDetailsProvider.class);

        assertFalse(provider instanceof S2SAuthenticationDetailsProvider,
                    "Global BasicAuthenticationDetailsProvider must not come from Limits module-local auth");
    }

    @Test
    void usesGlobalProviderWhenLimitsAuthIsAbsent() {
        BasicAuthenticationDetailsProvider globalProvider = new TestBasicAuthenticationDetailsProvider();
        BasicAuthenticationDetailsProvider provider = LimitsAuthProviderFactory.authProvider(
                LimitsConfig.builder().build(),
                OciConfig.builder().build(),
                () -> Optional.of(globalProvider));

        assertSame(globalProvider, provider);
    }

    @Test
    void reportsMissingGlobalProviderWhenLimitsAuthIsAbsent() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          LimitsConfig.builder().build(),
                                                          OciConfig.builder().build(),
                                                          Optional::empty));

        assertEquals("BasicAuthenticationDetailsProvider must be available when oci.limits.auth is not configured",
                     error.getMessage());
    }

    @Test
    void limitsAuthRejectsUnsupportedAuthenticationMethod() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          LimitsConfig.builder()
                                                                  .auth(auth -> auth.authenticationMethod("config"))
                                                                  .build(),
                                                          OciConfig.builder().build(),
                                                          Optional::empty));

        assertEquals("oci.limits.auth.authentication-method supports only service-principal",
                     error.getMessage());
    }

    @Test
    void limitsAuthFailsWhenPlatformProvidedS2sCannotReachImds() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          LimitsConfig.builder()
                                                                  .auth(LimitsAuthConfig.create())
                                                                  .build(),
                                                          OciConfig.builder()
                                                                  .imdsBaseUri(URI.create("http://127.0.0.1:1/opc/v2/"))
                                                                  .imdsTimeout(Duration.ofMillis(1))
                                                                  .build(),
                                                          Optional::empty));

        assertEquals("oci.limits.auth service-principal requires platform-provided S2S configuration to be available",
                     error.getMessage());
    }

    @Test
    void limitsAuthOverridesCommonOciConfig() {
        ServicePrincipalAuthConfig servicePrincipalConfig = ServicePrincipalAuthConfig.builder()
                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                .tenantId("ocid1.tenancy.oc1..limits")
                .imdsBaseUri(URI.create("http://127.0.0.1:8000/opc/v2/"))
                .build();

        OciConfig effective = LimitsAuthProviderFactory.effectiveOciConfig(
                OciConfig.builder()
                        .region(Region.US_ASHBURN_1)
                        .tenantId("ocid1.tenancy.oc1..common")
                        .build(),
                servicePrincipalConfig);

        assertEquals(Region.US_ASHBURN_1, effective.region().orElseThrow());
        assertEquals("ocid1.tenancy.oc1..limits", effective.tenantId().orElseThrow());
        assertEquals(URI.create("https://auth.test.oraclecloud.com/v1/x509"),
                     effective.federationEndpoint().orElseThrow());
        assertEquals(URI.create("http://127.0.0.1:8000/opc/v2/"),
                     effective.imdsBaseUri().orElseThrow());
    }

    @Test
    void limitsAuthPreservesCommonImdsConfig() {
        OciConfig effective = LimitsAuthProviderFactory.effectiveOciConfig(
                OciConfig.builder()
                        .imdsBaseUri(URI.create("http://127.0.0.1:8000/opc/v2/"))
                        .imdsTimeout(Duration.ofSeconds(3))
                        .imdsDetectRetries(2)
                        .build(),
                ServicePrincipalAuthConfig.create());

        assertEquals(URI.create("http://127.0.0.1:8000/opc/v2/"),
                     effective.imdsBaseUri().orElseThrow());
        assertEquals(Duration.ofSeconds(3), effective.imdsTimeout());
        assertEquals(2, effective.imdsDetectRetries().orElseThrow());
    }

    @Test
    void limitsExplicitAuthReportsLimitsFederationEndpointKey() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          LimitsConfig.builder()
                                                                  .auth(auth -> auth.servicePrincipal(
                                                                          sp -> sp
                                                                                  .tenantId("ocid1.tenancy.oc1..limits")
                                                                                  .usePlatformProvided(false)
                                                                                  .certificates(explicitCertificates()
                                                                                                            .certificates())))
                                                                  .build(),
                                                          OciConfig.builder().build(),
                                                          Optional::empty));

        assertEquals("oci.limits.auth.service-principal.federation-endpoint or helidon.oci.federation-endpoint "
                             + "must be configured when oci.limits.auth.service-principal.use-platform-provided "
                             + "is false",
                     error.getMessage());
    }

    @Test
    void limitsExplicitAuthReportsLimitsTenantKey() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          LimitsConfig.builder()
                                                                  .auth(auth -> auth.servicePrincipal(
                                                                          sp -> sp
                                                                                  .federationEndpoint(URI.create(
                                                                                          "https://auth.test."
                                                                                                  + "oraclecloud.com"
                                                                                                  + "/v1/x509"))
                                                                                  .usePlatformProvided(false)
                                                                                  .certificates(explicitCertificates()
                                                                                                            .certificates())))
                                                                  .build(),
                                                          OciConfig.builder().build(),
                                                          Optional::empty));

        assertEquals("oci.limits.auth.service-principal.tenant-id or helidon.oci.tenant-id must be configured "
                             + "when oci.limits.auth.service-principal.use-platform-provided is false",
                     error.getMessage());
    }

    @Test
    void limitsExplicitAuthReportsMissingCertificates() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          explicitAuthConfig(ServicePrincipalAuthConfig.builder()
                                                                                     .usePlatformProvided(false)
                                                                                     .build()),
                                                          OciConfig.builder().build(),
                                                          Optional::empty));

        assertEquals("oci.limits.auth.service-principal.certificates must contain at least the leaf certificate "
                             + "when use-platform-provided is false",
                     error.getMessage());
    }

    @Test
    void limitsExplicitAuthReportsMissingLeafPrivateKey() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                                                  () -> LimitsAuthProviderFactory.authProvider(
                                                          explicitAuthConfig(
                                                                  ServicePrincipalAuthConfig.builder()
                                                                          .usePlatformProvided(false)
                                                                          .certificates(List.of(
                                                                                  certificateWithoutPrivateKey()))
                                                                          .build()),
                                                          OciConfig.builder().build(),
                                                          Optional::empty));

        assertEquals("The first oci.limits.auth.service-principal certificate entry must configure private-key",
                     error.getMessage());
    }

    @Test
    void defaultClientConfigurationMatchesPreviousLimitsDefaults() {
        LimitsConfig config = LimitsConfig.builder().build();
        ClientConfiguration client = config.client()
                .orElseGet(() -> ClientConfiguration.builder().build());

        assertEquals(10000, client.getConnectionTimeoutMillis(), "Connection timeout should match");
        assertEquals(60000, client.getReadTimeoutMillis(), "Read timeout should match");
        assertEquals(50, client.getMaxAsyncThreads(), "Max async threads should match");
    }

    @Test
    void configFile() {
        ConfigFile configFile = Services.get(ConfigFile.class);
        assertNotNull(configFile, "ConfigFile instance should not be null");
        assertEquals("test", configFile.get("user"));
    }

    private static LimitsConfig scopedLimitsConfig() {
        return LimitsConfig.create(Config.just(ConfigSources.create(Map.ofEntries(
                Map.entry("oci.limits.auth.authentication-method", "service-principal"),
                Map.entry("oci.limits.auth.service-principal.federation-endpoint",
                          "https://auth.test.oraclecloud.com/v1/x509"),
                Map.entry("oci.limits.auth.service-principal.tenant-id",
                          "ocid1.tenancy.oc1..testserviceprincipal"),
                Map.entry("oci.limits.auth.service-principal.use-platform-provided", "false"),
                Map.entry("oci.limits.auth.service-principal.certificates.0.certificate",
                          "servicePrincipalCert.pem"),
                Map.entry("oci.limits.auth.service-principal.certificates.0.private-key",
                          "servicePrincipalKey.pem"),
                Map.entry("oci.limits.auth.service-principal.certificates.0.passphrase", ""),
                Map.entry("oci.limits.auth.service-principal.certificates.1.certificate",
                          "servicePrincipalCert.pem")
        ))).get("oci.limits"));
    }

    private static ServicePrincipalAuthConfig explicitCertificates() {
        return ServicePrincipalAuthConfig.builder()
                .usePlatformProvided(false)
                .certificates(List.of(ServicePrincipalCertificateConfig.builder()
                                      .certificate("servicePrincipalCert.pem")
                                      .privateKey("servicePrincipalKey.pem")
                                      .build()))
                .build();
    }

    private static ServicePrincipalCertificateConfig certificateWithoutPrivateKey() {
        return ServicePrincipalCertificateConfig.builder()
                .certificate("servicePrincipalCert.pem")
                .build();
    }

    private static LimitsConfig explicitAuthConfig(ServicePrincipalAuthConfig servicePrincipalConfig) {
        return LimitsConfig.builder()
                .auth(auth -> auth.servicePrincipal(
                        ServicePrincipalAuthConfig.builder(servicePrincipalConfig)
                                .federationEndpoint(URI.create("https://auth.test.oraclecloud.com/v1/x509"))
                                .tenantId("ocid1.tenancy.oc1..limits")
                                .build()))
                .build();
    }

    private static final class TestBasicAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {
        @Override
        public String getKeyId() {
            return "ocid1.tenancy.oc1..test/ocid1.user.oc1..test/fingerprint";
        }

        @Override
        public InputStream getPrivateKey() {
            return InputStream.nullInputStream();
        }

        @Override
        public String getPassPhrase() {
            return null;
        }

        @Override
        public char[] getPassphraseCharacters() {
            return null;
        }
    }
}

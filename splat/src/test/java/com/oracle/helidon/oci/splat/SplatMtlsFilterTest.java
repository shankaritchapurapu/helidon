/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.splat;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.mp.MpConfig;
import io.helidon.config.mp.MpConfigSources;

import com.oracle.pic.platform.splat.sdk.config.SplatMtlsFilterConfig;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class SplatMtlsFilterTest {
    private static final String VALID_SPLAT_CERTIFICATE_CN = "splat-api-client.us-ashburn-1.oci.oracleiaas.com";
    private static final String INALID_SPLAT_CERTIFICATE_CN = "invalid.anywhere-1.oci.oracleiaas.com";
    private static final String SPLAT_MTLS_FILTER_CONFIG_KEY_ENABLED =
            SplatMtlsFilter.SPLAT_MTLS_FILTER_CONFIG_KEY + "." + SplatMtlsFilter.ENABLED;
    private static final String SPLAT_SKIP_AUTHZ_VALIDATION_CHECK =
            SplatMtlsFilter.SPLAT_MTLS_FILTER_CONFIG_KEY + "." + SplatMtlsFilter.SKIP_AUTHZ_VALIDATION_CHECK;
    private static final String SPLAT_REJECT_X_REGION_CALLS =
            SplatMtlsFilter.SPLAT_MTLS_FILTER_CONFIG_KEY + "." + SplatMtlsFilter.REJECT_X_REGION_CALLS;
    private static final String US_ASHBURN_1_REGION = "us-ashburn-1";
    private static final String US_PHOENIX_1_REGION = "us-phoenix-1";
    private static final String US_SANJOSE_1_REGION = "us-sanjose-1";
    Map<String, Object> containerRequestProperty = new HashMap<>();

    @ParameterizedTest
    @MethodSource("certificateCNs")
    void testCertificateValidation(String certificateCN, boolean valid) throws Exception {
        DummySplatMtlsFilter splatMtlsFilter = getSplatMtlsFilter(
                Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "true"),
                certificateCN);
        ContainerRequestContext requestContext = newRequestContext("https://localhost:8080");
        if (valid) {
            splatMtlsFilter.filter(requestContext);
            assertThat(splatMtlsFilter.isFilterNotSkipped(), is(true));
        } else {
            ForbiddenException exception =
                    assertThrows(ForbiddenException.class, () -> splatMtlsFilter.filter(requestContext));
            assertThat(exception.getMessage(), containsString("Client cert is not whitelisted"));
            assertThat(splatMtlsFilter.isFilterNotSkipped(), is(true));
        }
    }

    @ParameterizedTest
    @MethodSource("uriSchemes")
    void testIfHTTPIsSkipped(String uriScheme, boolean notSkipped) throws Exception {
        DummySplatMtlsFilter splatMtlsFilter = getSplatMtlsFilter(
                Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "true"),
                VALID_SPLAT_CERTIFICATE_CN);
        // Passing an http base uri
        ContainerRequestContext requestContext = newRequestContext(String.format("%s://localhost:8080", uriScheme));
        splatMtlsFilter.filter(requestContext);
        assertThat(splatMtlsFilter.isFilterNotSkipped(), is(notSkipped));
    }

    @ParameterizedTest
    @MethodSource("enabledConfigParam")
    void testEnableSplatMtlsFilter(Map<String, String> configMap, boolean valid) {
        DummySplatMtlsFilter splatMtlsFilter = getSplatMtlsFilter(
                configMap,
                VALID_SPLAT_CERTIFICATE_CN);
        assertThat(splatMtlsFilter.isCreateSplatMtlsFilterInvoked(), is(valid));
    }

    @ParameterizedTest
    @MethodSource("regionConfigParam")
    void testRegionOverride(Map configMap, String expectedRegion) {
        DummySplatMtlsFilter splatMtlsFilter = getSplatMtlsFilter(
                configMap,
                VALID_SPLAT_CERTIFICATE_CN);
        assertThat(splatMtlsFilter.getRegion(), is(expectedRegion));
    }

    @ParameterizedTest
    @MethodSource("splatMtlsFilterConfigParam")
    void testSplatMtlsFilterConfig(Map configMap, List expectedResult) {
        DummySplatMtlsFilter splatMtlsFilter = getSplatMtlsFilter(
                configMap,
                VALID_SPLAT_CERTIFICATE_CN);
        SplatMtlsFilterConfig splatMtlsFilterConfig = splatMtlsFilter.getSplatMtlsFilterConfig();
        if (splatMtlsFilterConfig != null) {
            assertThat(splatMtlsFilterConfig.isSkipAuthzValidationCheck(), is(expectedResult.get(0)));
            assertThat(splatMtlsFilterConfig.isRejectXRegionCalls(), is(expectedResult.get(1)));
        } else {
            assertThat(expectedResult, is(nullValue()));
        }
    }

    @Test
    void testRegionFromServiceRegistry() {
        DummySplatMtlsFilter splatMtlsFilter = getSplatMtlsFilter(
                Map.of(),
                VALID_SPLAT_CERTIFICATE_CN,
                false);
        assertThat(splatMtlsFilter.regionFromServiceRegistry(), is(US_PHOENIX_1_REGION));
    }

    @Test
    void testRegionFromConfig() {
        // This can also be set in oci-config.yaml or as an environment variable
        System.setProperty("helidon.oci.region", US_SANJOSE_1_REGION);
        var splatMtlsFilter = new SplatMtlsFilter();
        assertThat(splatMtlsFilter.getRegion(Config.just(ConfigSources.create(Map.of()))), is(US_SANJOSE_1_REGION));
    }

    DummySplatMtlsFilter getSplatMtlsFilter(Map<String, String> configMap, String certCommonName) {
        return getSplatMtlsFilter(configMap, certCommonName, true);
    }

    DummySplatMtlsFilter getSplatMtlsFilter(Map<String, String> configMap, String certCommonName, boolean withRegionConfig) {
        Map<String, String> updatedConfigMap = new HashMap<>(configMap);
        if (withRegionConfig && !configMap.containsKey(SplatMtlsFilter.OCI_REGION_CONFIG_KEY)) {
            // Add this if it doesn't exist to avoid retrieving region information from Service Registry
            updatedConfigMap.put(SplatMtlsFilter.OCI_REGION_CONFIG_KEY, US_ASHBURN_1_REGION);
        }

        String CertCN = "CN=" + certCommonName;
        return new DummySplatMtlsFilter(updatedConfigMap, CertCN);
    }

    private static Stream<Arguments> certificateCNs() {
        return Stream.of(
                arguments(VALID_SPLAT_CERTIFICATE_CN, true),
                arguments(INALID_SPLAT_CERTIFICATE_CN, false)
        );
    }

    private static Stream<Arguments> uriSchemes() {
        return Stream.of(
                arguments("http", false),
                arguments("https", true)
        );
    }

    private static Stream<Arguments> enabledConfigParam() {
        return Stream.of(
                arguments(Map.of(), true),
                arguments(Map.of(SPLAT_MTLS_FILTER_CONFIG_KEY_ENABLED, "true"), true),
                arguments(Map.of(SPLAT_MTLS_FILTER_CONFIG_KEY_ENABLED, "false"), false)
        );
    }

    private static Stream<Arguments> regionConfigParam() {
        return Stream.of(
                arguments(Map.of(SplatMtlsFilter.OCI_REGION_CONFIG_KEY, US_ASHBURN_1_REGION), US_ASHBURN_1_REGION),
                arguments(Map.of(SplatMtlsFilter.OCI_REGION_CONFIG_KEY, "eu-zurich-1"), "eu-zurich-1"),
                arguments(Map.of(SplatMtlsFilter.OCI_REGION_CONFIG_KEY, "ap-tokyo-1"), "ap-tokyo-1")
        );
    }

    private static Stream<Arguments> splatMtlsFilterConfigParam() {
        return Stream.of(
                arguments(Map.of(), null),
                arguments(Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "true",
                                 SPLAT_REJECT_X_REGION_CALLS, "true"),
                          Arrays.asList(true, true)),
                arguments(Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "false",
                                 SPLAT_REJECT_X_REGION_CALLS, "false"),
                          Arrays.asList(false, false)),
                arguments(Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "true",
                                 SPLAT_REJECT_X_REGION_CALLS, "false"),
                          Arrays.asList(true, false)),
                arguments(Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "false",
                                 SPLAT_REJECT_X_REGION_CALLS, "true"),
                          Arrays.asList(false, true)),
                arguments(Map.of(SPLAT_SKIP_AUTHZ_VALIDATION_CHECK, "true"),
                          Arrays.asList(true, false)),
                arguments(Map.of(SPLAT_REJECT_X_REGION_CALLS, "true"),
                          Arrays.asList(false, true))
        );
    }

    private ContainerRequestContext newRequestContext(String baseUri) throws Exception {
        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI(baseUri));
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        doAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0);
            Object value = invocationOnMock.getArgument(1);
            containerRequestProperty.put(key, value);
            return null;
        }).when(requestContext).setProperty(anyString(), any());
        doAnswer(invocationOnMock -> {
            String key = invocationOnMock.getArgument(0);
            return containerRequestProperty.get(key);
        }).when(requestContext).getProperty(anyString());
        return requestContext;
    }

    public static class DummySplatMtlsFilter extends SplatMtlsFilter {
        private String certCN;
        private boolean isFilterNotSkipped;
        private boolean isCreateSplatMtlsFilterInvoked = false;
        private String regionFromServiceRegistry = null;
        private Map<String, String> configMap;
        private String region;
        private SplatMtlsFilterConfig splatMtlsFilterConfig;

        public DummySplatMtlsFilter(Map<String, String> configMap,
                                    String certCN) {
            this.certCN = certCN;
            this.configMap = configMap;
            postConstruct();
        }

        @Override
        protected X509Certificate getClientTlsCertificate() {
            isFilterNotSkipped = true;
            X509Certificate certificate = Mockito.mock(X509Certificate.class);
            X500Principal principal = new X500Principal(certCN);
            when(certificate.getSubjectX500Principal()).thenReturn(principal);
            return certificate;
        }

        @Override
        protected Config getGlobalConfig() {
            return MpConfig.toHelidonConfig(getConfig(configMap));
        }

        @Override
        protected String getRegionFromServiceRegistry() {
            this.regionFromServiceRegistry = US_PHOENIX_1_REGION;
            return US_PHOENIX_1_REGION;
        }

        @Override
        protected void createSplatMtlsFilter(String region, SplatMtlsFilterConfig splatMtlsFilterConfig) {
            this.region = region;
            this.splatMtlsFilterConfig = splatMtlsFilterConfig;
            this.isCreateSplatMtlsFilterInvoked = true;
            super.createSplatMtlsFilter(region, splatMtlsFilterConfig);
        }

        String getRegion() {
            return region;
        }

        SplatMtlsFilterConfig getSplatMtlsFilterConfig() {
            return splatMtlsFilterConfig;
        }

        boolean isCreateSplatMtlsFilterInvoked() {
            return isCreateSplatMtlsFilterInvoked;
        }

        boolean isFilterNotSkipped() {
            return isFilterNotSkipped;
        }

        String regionFromServiceRegistry() {
            return this.regionFromServiceRegistry;
        }

        private org.eclipse.microprofile.config.Config getConfig(Map configMap) {
            return ConfigProviderResolver.instance()
                    .getBuilder()
                    .withSources(MpConfigSources.create(configMap))
                    .build();

        }
    }
}

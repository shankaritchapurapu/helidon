/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.identity;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.mp.MpConfig;

import org.eclipse.microprofile.config.ConfigProvider;

class OciIdentityConfiguration {
    /**
     * The top level config key used to configure the general application-level settings.
     */
    static final String TAG_APP_CONFIG_KEY = "oci.app";
    /**
     * The top level config key used to configure the filter and oci-identity features.
     */
    static final String TAG_AUTH_CONFIG_KEY = "oci.identity";
    private static final Logger LOGGER = Logger.getLogger(OciIdentityConfiguration.class.getName());
    private static final LazyValue<Config> CONFIG = LazyValue
            .create(() -> MpConfig.toHelidonConfig(ConfigProvider.getConfig()));

    private static final LazyValue<Config> APP_CONFIG = LazyValue
            .create(() -> CONFIG.get().get(TAG_APP_CONFIG_KEY));
    private static final LazyValue<Config> AUTH_CONFIG = LazyValue
            .create(() -> CONFIG.get().get(TAG_AUTH_CONFIG_KEY));

    private static AuthConfig authConfigOverride;

    private OciIdentityConfiguration() {
    }

    static Config globalAppConfig() {
        return APP_CONFIG.get();
    }

    static Config globalOciIdentityConfig() {
        return AUTH_CONFIG.get();
    }

    static Config globalMpConfig() {
        return CONFIG.get();
    }

    static AuthConfig authConfig() {
        if (authConfigOverride != null) {
            return authConfigOverride;
        }

        return new AuthConfig(globalOciIdentityConfig());
    }

    /**
     * @param authConfig
     * @deprecated Remove after config refactor
     */
    @Deprecated
    static void authConfigOverride(AuthConfig authConfig) {
        OciIdentityConfiguration.authConfigOverride = authConfig;
    }

    static AppConfig appConfig() {
        return new AppConfig(globalAppConfig());
    }

    static class AppConfig {
        static final String DEVELOPMENT_STAGE = "DEVELOPMENT";
        static final String TEST_STAGE = "TEST";
        static final String PRODUCTION_STAGE = "PRODUCTION";
        static final String DEFAULT_STAGE = DEVELOPMENT_STAGE;
        static final String DEFAULT_NAME = "xxxx";
        static final String DEFAULT_TEAM_NAME = "x-team";
        static final String DEFAULT_GLOBAL_BUSINESS_UNIT = "GBU";

        private final Config config;

        AppConfig(Config config) {
            this.config = Objects.requireNonNull(config);
        }

        /**
         * The application name. The default value is {@link #DEFAULT_NAME}.
         *
         * @return the application name
         */
        public String name() {
            return config.get("name").asString().orElse(DEFAULT_NAME);
        }

        /**
         * The application team name. The default value is {@link #DEFAULT_TEAM_NAME}.
         *
         * @return the application team name
         */
        public String teamName() {
            return config.get("teamName").asString().orElse(DEFAULT_TEAM_NAME);
        }

        /**
         * The global business unit name. The default value is {@link #DEFAULT_TEAM_NAME}.
         *
         * @return the global business unit name
         */
        public String globalBusinessUnit() {
            return config.get("globalBusinessUnit").asString().orElse(DEFAULT_GLOBAL_BUSINESS_UNIT);
        }

        /**
         * The application stage name. Valid values are {@link #DEVELOPMENT_STAGE}, {@link #TEST_STAGE}, and
         * {@link #PRODUCTION_STAGE}.
         * The default value is {@link #DEFAULT_STAGE}.
         *
         * @return the application stage name
         */
        public String stage() {
            return config.get("stage").asString().orElse(DEFAULT_STAGE);
        }

        /**
         * Returns {@code true} if {@link #stage()} is {@link #DEVELOPMENT_STAGE}.
         *
         * @return true if development stage
         */
        public boolean isDevStage() {
            return stage().equals(DEVELOPMENT_STAGE);
        }

        @Override
        public String toString() {
            return "{\tname: " + name()
                    + ";\n\tteamName: " + teamName()
                    + ";\n\tglobalBusinessUnit: " + globalBusinessUnit()
                    + ";\n\tstage: " + stage()
                    + ";\n\tisDevStage: " + isDevStage()
                    + "\n}";
        }
    }

    static class AuthConfig {
        static final boolean DEFAULT_AUTH_ENABLED = false;
        static final String DEFAULT_ROOT_CERT_PATH = "/etc/oci-pki/ca-bundle.pem";
        static final boolean DEFAULT_METRICS_ENABLED = false;
        static final String DEFAULT_AUTH_SERVICE_ENDPOINT = "https://auth.us-phoenix-1.oraclecloud.com";
        static final String DEFAULT_METADATA_ENDPOINT = "http://localhost:8080"; // "http://169.254.169.254" in !DEVELOPMENT stage
        static final int DEFAULT_REFRESH_AUTH_TOKEN_BEFORE_SECONDS_TO_EXPIRE = 5;

        private final Config config;

        AuthConfig(Config config) {
            this.config = Objects.requireNonNull(config);
        }

        /**
         * Returns {@code true} if atn identity is enabled. The default value is {@link #DEFAULT_AUTH_ENABLED}.
         *
         * @return true if atn identity is enabled
         * @see MpInjectionSupport#produceAuthenticatorClient()
         */
        public boolean authEnabled() {
            return config.get("authEnabled").asBoolean().orElse(DEFAULT_AUTH_ENABLED);
        }

        /**
         * Returns the root cert path. This is only applicable when {@link #authEnabled()}. The default value is
         * {@link #DEFAULT_ROOT_CERT_PATH}.
         *
         * @return the root cert path
         * @see #rootCertFilePath()
         */
        public String rootCertPath() {
            return config.get("rootCertPath").asString().orElse(DEFAULT_ROOT_CERT_PATH);
        }

        /**
         * Returns the root file path if the config and backing file is present, otherwise empty.
         *
         * @return the root cert file path
         * @see #rootCertPath()
         */
        public Optional<Path> rootCertFilePath() {
            String rootCertPath = rootCertPath();
            if (rootCertPath == null || rootCertPath.isBlank()) {
                return Optional.empty();
            }

            Path filePath = Path.of(rootCertPath);
            if (filePath.toFile().exists()) {
                return Optional.of(filePath.toAbsolutePath());
            }

            return Optional.empty();
        }

        /**
         * Returns {@code true} if metrics are enabled. This is only applicable when {@link #authEnabled()}. The default value is
         * {@link #DEFAULT_METRICS_ENABLED}.
         *
         * @return flag indicating whether metrics are enabled
         */
        public boolean metricsEnabled() {
            return config.get("metricsEnabled").asBoolean().orElse(DEFAULT_METRICS_ENABLED);
        }

        /**
         * The metadata endpoint. The default value is {@link #DEFAULT_METADATA_ENDPOINT}.
         *
         * @return the metadata endpoint
         */
        public URI metadataEndpoint() {
            return URI.create(config.get("metadataEndpoint").asString()
                                      .orElse(config.get("instancePrincipalUrl").asString().orElse(DEFAULT_METADATA_ENDPOINT)));
        }

        /**
         * The instance principal endpoint. The default value is {@link #DEFAULT_METADATA_ENDPOINT}.
         *
         * @return the instance principal endpoint
         * @deprecated use {@link #metadataEndpoint()} instead
         */
        /*
            # You will need to open tunnel to your instance because
            # InstancePrincipalCertificateSupplier class requires this code to be running inside an instance.
            # ssh -L 8000:169.254.169.254:80 <username, ex: opc>@<your instance IP, ex: 10.2.73.217>
            # ssh -J overlay-host.bastion.us-phoenix-1.oci.oracleiaas.com -L 8000:169.254.169.254:80 jblancos@ocid1.bastion.oc1
            .phx.amaaaaaatqyweeqayg2n3rczn5aglbcp2xt6becbpffbhk6lon4bnvl7sxnq-10.2.30.3
            # More info: https://confluence.oci.oraclecorp.com/x/Jk7lGw
         */
        public URI instancePrincipalUrl() {
            return URI.create(config.get("instancePrincipalUrl").asString()
                                      .orElse(config.get("metadataEndpoint")
                                                      .asString()
                                                      .orElse(DEFAULT_METADATA_ENDPOINT)));
        }

        /**
         * Returns the authentication service endpoint. This is only applicable when {@link #authEnabled()}. The default value is
         * {@link #DEFAULT_AUTH_SERVICE_ENDPOINT}.
         *
         * @return the auth service endpoint
         */
        /*  see AuthTest.conf
            # If you want to test Service Enclave then open a Tunnel to it
            # Here's an example on opening a tunnel to unstable AUTH in R1-AD2
            # ssh -L localhost:9010:authservice-alpha-unstable.svc.ad2.r1:80 bastion-ad2.rb.r1.oci.oracleiaas.com
            # authServiceEndpoint: "http://localhost:9000"
            # Otherwise use the public Identity endpoint
            authServiceEndpoint: "https://auth.us-phoenix-1.oraclecloud.com"
         */
        public URI authServiceEndpoint() {
            return URI.create(config.get("authServiceEndpoint").asString().orElse(DEFAULT_AUTH_SERVICE_ENDPOINT));
        }

        /**
         * See
         * {@link com.oracle.pic.identity.authentication.AuthServiceAuthenticationClient.Builder#expirationBufferInSeconds(int)}.
         * The default value is {@link #DEFAULT_REFRESH_AUTH_TOKEN_BEFORE_SECONDS_TO_EXPIRE}.
         *
         * @return seconds
         */
        public int refreshAuthTokenBeforeSecondsToExpire() {
            return config.get("refreshAuthTokenBeforeSecondsToExpire").asInt()
                    .orElse(DEFAULT_REFRESH_AUTH_TOKEN_BEFORE_SECONDS_TO_EXPIRE);
        }

        @Override
        public String toString() {
            return "{\tauthEnabled: " + authEnabled()
                    + ";\n\tmetricsEnabled: " + metricsEnabled()
                    + ";\n\tauthServiceEndpoint: " + authServiceEndpoint()
                    + ";\n\tmetadataEndpoint: " + metadataEndpoint()
                    + ";\n\trootCertPath: " + rootCertPath()
                    + ";\n\trootCertFilePath: " + rootCertFilePath()
                    + ";\n\trefreshAuthTokenBeforeSecondsToExpire: " + refreshAuthTokenBeforeSecondsToExpire()
                    + "\n}";
        }
    }

}

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.config.ConfigException;
import io.helidon.service.registry.Service;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.ConfigFileReader.ConfigFile;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;

@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class AdpSessionTokenBuilderProvider implements Supplier<Optional<SessionTokenAuthenticationDetailsProvider>> {
    private static final String DEFAULT_PRIVATE_KEY_FILE_PATH = "~/.oci/sessions/DEFAULT/oci_api_key.pem";

    private final OciConfig config;
    private final Supplier<Optional<ConfigFile>> configFileSupplier;

    AdpSessionTokenBuilderProvider(OciConfig config,
                                   Supplier<Optional<ConfigFile>> configFileSupplier) {

        this.config = config;
        this.configFileSupplier = configFileSupplier;
    }

    @Override
    public Optional<SessionTokenAuthenticationDetailsProvider> get() {
        try {
            return build(config.sessionTokenMethodConfig());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteTemporaryFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                path.toFile().deleteOnExit();
            }
        }
    }

    private static void append(StringBuilder content, String key, String value) {
        if (value != null) {
            content.append(key)
                    .append('=')
                    .append(value)
                    .append('\n');
        }
    }

    private Optional<SessionTokenAuthenticationDetailsProvider> build(Optional<SessionTokenMethodConfig> maybeSessionTokenConfig)
            throws IOException {
        if (maybeSessionTokenConfig.isEmpty()) {
            Optional<ConfigFile> maybeConfigFile = configFileSupplier.get();
            if (maybeConfigFile.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SessionTokenAuthenticationDetailsProvider(maybeConfigFile.get()));
        }

        return Optional.of(build(maybeSessionTokenConfig.get()));
    }

    private SessionTokenAuthenticationDetailsProvider build(SessionTokenMethodConfig sessionTokenConfig) throws IOException {
        Optional<String> sessionToken = sessionTokenConfig.sessionToken();
        Optional<Path> sessionTokenPath = sessionTokenConfig.sessionTokenPath();

        if (sessionToken.isEmpty() && sessionTokenPath.isEmpty()) {
            throw new ConfigException("When configuring session token authentication, either session token or session token "
                                              + "path must be provided");
        }

        Path temporarySessionTokenPath = null;
        String effectiveSessionTokenPath = sessionTokenPath
                .map(Path::toString)
                .orElse(null);

        // OCI SDK 2.88 requires security_token_file in the constructor even when an inline token
        // is set immediately after construction.
        if (sessionToken.isPresent() && effectiveSessionTokenPath == null) {
            temporarySessionTokenPath = Files.createTempFile("helidon-oci-session-token", ".tmp");
            Files.writeString(temporarySessionTokenPath, sessionToken.get(), StandardCharsets.UTF_8);
            temporarySessionTokenPath.toFile().deleteOnExit();
            effectiveSessionTokenPath = temporarySessionTokenPath.toString();
        }

        try {
            var provider = new SessionTokenAuthenticationDetailsProvider(configFile(sessionTokenConfig,
                                                                                    effectiveSessionTokenPath));
            sessionToken.ifPresent(provider::setSessionToken);
            return provider;
        } catch (IOException | RuntimeException e) {
            deleteTemporaryFile(temporarySessionTokenPath);
            throw e;
        }
    }

    private ConfigFileReader.ConfigFile configFile(SessionTokenMethodConfig sessionTokenConfig,
                                                   String sessionTokenPath) throws IOException {
        StringBuilder content = new StringBuilder("[DEFAULT]\n");

        append(content, "security_token_file", sessionTokenPath);
        append(content, "tenancy", sessionTokenConfig.tenantId());
        append(content, "key_file", privateKeyFilePath(sessionTokenConfig));
        append(content, "region", sessionTokenConfig.region());
        append(content, "fingerprint", sessionTokenConfig.fingerprint());
        append(content, "user", sessionTokenConfig.userId());

        sessionTokenConfig.passphrase()
                .map(String::new)
                .or(() -> value("pass_phrase"))
                .ifPresent(passPhrase -> append(content, "pass_phrase", passPhrase));

        return ConfigFileReader.parse(new ByteArrayInputStream(content.toString().getBytes(StandardCharsets.UTF_8)),
                                      null);
    }

    private String privateKeyFilePath(SessionTokenMethodConfig sessionTokenConfig) {
        return sessionTokenConfig.privateKeyPath()
                .map(Path::toString)
                .or(() -> value("key_file"))
                .orElse(DEFAULT_PRIVATE_KEY_FILE_PATH);
    }

    private Optional<String> value(String key) {
        return configFileSupplier.get().map(file -> file.get(key));
    }
}

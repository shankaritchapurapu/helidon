/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.helidon.common.config.ConfigException;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RefreshableOnNotAuthenticatedProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdpSessionTokenBuilderProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildUsesSessionTokenConfigWithoutConfigFile() throws Exception {
        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .passphrase("passphrase")
                .tenantId("tenant")
                .userId("user")
                .privateKeyPath(Path.of("private-key.pem"))
                .sessionToken("configured-token")
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();

        var provider = provider(config, Optional.empty());
        var authProvider = (AuthenticationDetailsProvider) provider;

        assertThat(provider, instanceOf(SessionTokenAuthenticationDetailsProvider.class));
        assertThat(provider.getKeyId(), is("ST$configured-token"));
        assertThat(authProvider.getFingerprint(), is("fp"));
        assertThat(authProvider.getTenantId(), is("tenant"));
        assertThat(authProvider.getUserId(), is("user"));
        assertThat(new String(provider.getPassphraseCharacters()), is("passphrase"));
        assertThat(((RegionProvider) provider).getRegion(), is(Region.US_PHOENIX_1));
    }

    @Test
    void buildCanRefreshConfiguredSessionTokenWithoutConfiguredPath() throws Exception {
        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .tenantId("tenant")
                .userId("user")
                .sessionToken("configured-token")
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();

        var provider = provider(config, Optional.empty());
        var refreshable = (RefreshableOnNotAuthenticatedProvider<?>) provider;

        assertThat(refreshable.refresh(), is("configured-token"));
        assertThat(provider.getKeyId(), is("ST$configured-token"));
    }

    @Test
    void buildPrefersConfiguredSessionTokenOverConfiguredPath() throws Exception {
        Path tokenPath = tempDir.resolve("session-token");
        Files.writeString(tokenPath, "file-token\n", StandardCharsets.UTF_8);

        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .tenantId("tenant")
                .userId("user")
                .sessionTokenPath(tokenPath)
                .sessionToken("configured-token")
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();

        var provider = provider(config, Optional.empty());

        assertThat(provider, instanceOf(SessionTokenAuthenticationDetailsProvider.class));
        assertThat(provider.getKeyId(), is("ST$configured-token"));
    }

    @Test
    void buildCanRefreshConfiguredSessionTokenFromConfiguredPath() throws Exception {
        Path tokenPath = tempDir.resolve("session-token");
        Files.writeString(tokenPath, "file-token\n", StandardCharsets.UTF_8);

        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .tenantId("tenant")
                .userId("user")
                .sessionTokenPath(tokenPath)
                .sessionToken("configured-token")
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();

        var provider = provider(config, Optional.empty());

        assertThat(provider.getKeyId(), is("ST$configured-token"));

        Files.writeString(tokenPath, "updated-token\n", StandardCharsets.UTF_8);
        var refreshable = (RefreshableOnNotAuthenticatedProvider<?>) provider;
        assertThat(refreshable.refresh(), is("updated-token"));
        assertThat(provider.getKeyId(), is("ST$updated-token"));
    }

    @Test
    void buildUsesConfiguredSessionTokenPath() throws Exception {
        Path tokenPath = tempDir.resolve("session-token");
        Files.writeString(tokenPath, "file-token\n", StandardCharsets.UTF_8);

        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .tenantId("tenant")
                .userId("user")
                .sessionTokenPath(tokenPath)
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();

        var provider = provider(config, Optional.empty());

        assertThat(provider, instanceOf(SessionTokenAuthenticationDetailsProvider.class));
        assertThat(provider.getKeyId(), is("ST$file-token"));

        Files.writeString(tokenPath, "updated-token\n", StandardCharsets.UTF_8);
        var refreshable = (RefreshableOnNotAuthenticatedProvider<?>) provider;
        assertThat(refreshable.refresh(), is("updated-token"));
        assertThat(provider.getKeyId(), is("ST$updated-token"));
    }

    @Test
    void buildUsesConfigFileWhenSessionTokenConfigIsAbsent() throws Exception {
        Path tokenPath = tempDir.resolve("session-token");
        Files.writeString(tokenPath, "file-token\n", StandardCharsets.UTF_8);

        var configFile = configFile("""
                [DEFAULT]
                security_token_file=%s
                tenancy=tenant
                key_file=private-key.pem
                region=us-phoenix-1
                fingerprint=fp
                user=user
                pass_phrase=passphrase
                """.formatted(tokenPath));

        var provider = provider(OciConfig.create(), Optional.of(configFile));
        var authProvider = (AuthenticationDetailsProvider) provider;

        assertThat(provider, instanceOf(SessionTokenAuthenticationDetailsProvider.class));
        assertThat(provider.getKeyId(), is("ST$file-token"));
        assertThat(authProvider.getFingerprint(), is("fp"));
        assertThat(authProvider.getTenantId(), is("tenant"));
        assertThat(authProvider.getUserId(), is("user"));
        assertThat(new String(provider.getPassphraseCharacters()), is("passphrase"));
        assertThat(((RegionProvider) provider).getRegion(), is(Region.US_PHOENIX_1));
    }

    @Test
    void buildReturnsEmptyWhenSessionTokenConfigAndConfigFileAreAbsent() {
        Optional<SessionTokenAuthenticationDetailsProvider> provider = optionalProvider(OciConfig.create(), Optional.empty());

        assertThat(provider, is(Optional.empty()));
    }

    @Test
    void serviceRegistryReturnsEmptyWhenSessionTokenConfigAndConfigFileAreAbsent() {
        ServiceRegistryConfig registryConfig = ServiceRegistryConfig.builder()
                .discoverServices(false)
                .putContractInstance(OciConfig.class, OciConfig.create())
                .addServiceDescriptor(AdpSessionTokenBuilderProvider__ServiceDescriptor.INSTANCE)
                .build();
        ServiceRegistryManager manager = ServiceRegistryManager.create(registryConfig);
        try {
            Optional<BasicAuthenticationDetailsProvider> provider =
                    manager.registry().first(BasicAuthenticationDetailsProvider.class);

            assertThat(provider, is(Optional.empty()));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void buildFailsWhenSessionTokenConfigHasNoToken() {
        var sessionConfig = SessionTokenMethodConfig.builder()
                .region("us-phoenix-1")
                .fingerprint("fp")
                .tenantId("tenant")
                .userId("user")
                .build();
        var config = OciConfig.builder()
                .sessionTokenMethodConfig(sessionConfig)
                .build();

        ConfigException error = assertThrows(ConfigException.class, () -> provider(config, Optional.empty()));
        assertThat(error.getMessage(), containsString("either session token or session token path"));
    }

    private static SessionTokenAuthenticationDetailsProvider provider(OciConfig config,
                                                                      Optional<ConfigFileReader.ConfigFile> configFile) {
        return optionalProvider(config, configFile).orElseThrow();
    }

    private static Optional<SessionTokenAuthenticationDetailsProvider> optionalProvider(
            OciConfig config,
            Optional<ConfigFileReader.ConfigFile> configFile) {
        return new AdpSessionTokenBuilderProvider(config, () -> configFile).get();
    }

    private static ConfigFileReader.ConfigFile configFile(String content) throws Exception {
        return ConfigFileReader.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), null);
    }
}

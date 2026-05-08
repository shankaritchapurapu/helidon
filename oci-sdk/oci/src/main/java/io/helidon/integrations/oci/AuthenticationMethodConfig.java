/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.configurable.Resource;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.Service;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;

/**
 * Config based authentication method, uses the {@link com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider}.
 */
@Weight(Weighted.DEFAULT_WEIGHT - 10)
@Service.Provider
class AuthenticationMethodConfig implements OciAuthenticationMethod {
    static final String METHOD = "config";

    private static final System.Logger LOGGER = System.getLogger(AuthenticationMethodConfig.class.getName());

    private final LazyValue<Optional<BasicAuthenticationDetailsProvider>> provider;

    AuthenticationMethodConfig(OciConfig config) {
        provider = config.configMethodConfig()
                .map(configMethodConfigBlueprint -> LazyValue.create(() -> {
                    return Optional.of(createProvider(configMethodConfigBlueprint));
                }))
                .orElseGet(() -> {
                    if (LOGGER.isLoggable(Level.TRACE)) {
                        LOGGER.log(Level.TRACE, "Configuration for Config based authentication details provider is"
                                + " not available.");
                    }
                    return LazyValue.create(Optional.empty());
                });
    }

    @Override
    public String method() {
        return METHOD;
    }

    @Override
    public Optional<BasicAuthenticationDetailsProvider> provider() {
        return provider.get();
    }

    private static BasicAuthenticationDetailsProvider createProvider(ConfigMethodConfigBlueprint config) {
        Region region = Region.fromRegionCodeOrId(config.region());

        var builder = SimpleAuthenticationDetailsProvider.builder();

        // private key may be provided through different means
        if (config.privateKey().isPresent()) {
            // as a resource (classpath, file system, base64, plain text)
            Resource resource = config.privateKey().get();
            Supplier<InputStream> supplier = () -> new ByteArrayInputStream(resource.bytes());
            builder.privateKeySupplier(supplier);
        } else {
            // or as the default location in user.home/.oci/oic_api_key.pem
            String keyFile = System.getProperty("user.home");
            if (keyFile == null) {
                keyFile = "/";
            } else {
                if (!keyFile.endsWith("/")) {
                    keyFile = keyFile + "/";
                }
            }
            keyFile = keyFile + ".oci/oci_api_key.pem";

            builder.privateKeySupplier(new SimplePrivateKeySupplier(keyFile));
        }

        config.passphrase().ifPresent(builder::passphraseCharacters);

        return builder.region(region)
                .tenantId(config.tenantId())
                .userId(config.userId())
                .fingerprint(config.fingerprint())
                .build();

    }
}

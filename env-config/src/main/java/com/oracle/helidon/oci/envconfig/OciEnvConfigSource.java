/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.envconfig;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.config.MetaConfig;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.LazyConfigSource;
import io.helidon.integrations.oci.ImdsInstanceInfo;
import io.helidon.service.registry.Service;

// Keep oci-env ahead of default-weight OCI config sources that resolve ${oci.env.*} placeholders.
@Weight(Weighted.DEFAULT_WEIGHT + 100)
@Service.Singleton
@Service.Named(OciEnvConfigSourceProvider.TYPE)
final class OciEnvConfigSource implements ConfigSource, LazyConfigSource {
    static final String DESCRIPTION = "OCI Environment Config";

    private static final System.Logger LOGGER = System.getLogger(OciEnvConfigSource.class.getName());

    private final LazyValue<String> prefix;
    private final LazyValue<OciEnvConfigFactory.ConfigNodes> nodes;

    @Service.Inject
    OciEnvConfigSource(@Service.Named(OciEnvConfigSourceProvider.TYPE) Optional<MetaConfig> metaConfig,
                       Supplier<Optional<ImdsInstanceInfo>> imdsInstanceInfo) {
        this(() -> OciEnvConfigFactory.serviceConfig(metaConfig), imdsInstanceInfo);
    }

    OciEnvConfigSource(Config metaConfig) {
        this(() -> OciEnvConfigFactory.providerConfig(metaConfig), Optional::empty);
    }

    OciEnvConfigSource(String prefix, Supplier<OciEnvConfigFactory.ConfigNodes> nodesSupplier) {
        this.prefix = prefixValue(() -> Objects.requireNonNull(prefix));
        this.nodes = nodesValue(this.prefix, nodesSupplier);
    }

    private OciEnvConfigSource(Supplier<Config> configSupplier,
                               Supplier<Optional<ImdsInstanceInfo>> imdsInstanceInfo) {
        LazyValue<Config> config = LazyValue.create(() -> Objects.requireNonNull(configSupplier.get()));
        this.prefix = prefixValue(() -> configuredPrefix(config.get()));
        this.nodes = nodesValue(prefix, () -> new OciEnvConfigFactory(config.get(), imdsInstanceInfo).createNodes());
    }

    private static LazyValue<String> prefixValue(Supplier<String> prefixSupplier) {
        return LazyValue.create(() -> {
            try {
                return Objects.requireNonNull(prefixSupplier.get());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,
                           "Could not resolve oci-env config source prefix; source will use default prefix",
                           e);
                return OciEnvConfigFactory.DEFAULT_PREFIX;
            }
        });
    }

    private static LazyValue<OciEnvConfigFactory.ConfigNodes> nodesValue(
            LazyValue<String> prefix,
            Supplier<OciEnvConfigFactory.ConfigNodes> nodesSupplier) {
        return LazyValue.create(() -> {
            String currentPrefix = prefix.get();
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Materializing oci-env config source for prefix ''{0}''", currentPrefix);
            }
            try {
                return Objects.requireNonNull(nodesSupplier.get());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,
                           "Could not materialize oci-env config source for prefix '"
                                   + currentPrefix + "'; source will be empty",
                           e);
                return OciEnvConfigFactory.configNodes(ConfigNode.ObjectNode.builder().build());
            }
        });
    }

    private static String configuredPrefix(Config config) {
        return config.get("prefix")
                .asString()
                .orElse(OciEnvConfigFactory.DEFAULT_PREFIX);
    }

    @Override
    public String description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<ConfigNode> node(String key) {
        String currentPrefix = prefix.get();
        if (!handles(key, currentPrefix)) {
            return Optional.empty();
        }
        Optional<ConfigNode> result = Optional.ofNullable(nodes.get().nodesByKey().get(key));
        if (result.isEmpty() && LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "oci-env source handles key ''{0}'' for prefix ''{1}'', but no value is published",
                       key,
                       currentPrefix);
        }
        return result;
    }

    private boolean handles(String key, String prefix) {
        return key.equals(prefix) || key.startsWith(prefix + ".");
    }
}

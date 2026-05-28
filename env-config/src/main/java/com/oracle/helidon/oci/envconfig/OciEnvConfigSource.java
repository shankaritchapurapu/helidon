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
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.LazyConfigSource;
import io.helidon.service.registry.Service;

// Keep oci-env ahead of default-weight OCI config sources that resolve ${oci.env.*} placeholders.
@Weight(Weighted.DEFAULT_WEIGHT + 100)
@Service.Singleton
@Service.Named(OciEnvConfigSourceProvider.TYPE)
final class OciEnvConfigSource implements ConfigSource, LazyConfigSource {
    static final String DESCRIPTION = "OCI Environment Config";

    private static final System.Logger LOGGER = System.getLogger(OciEnvConfigSource.class.getName());

    private final String prefix;
    private final LazyValue<OciEnvConfigFactory.ConfigNodes> nodes;

    @Service.Inject
    OciEnvConfigSource(OciEnvConfigFactory factory) {
        this(factory.prefix(), factory::createNodes);
    }

    OciEnvConfigSource() {
        this(new OciEnvConfigFactory(Optional.empty()));
    }

    OciEnvConfigSource(Config metaConfig) {
        this(new OciEnvConfigFactory(OciEnvConfigFactory.providerConfig(metaConfig)));
    }

    OciEnvConfigSource(String prefix, Supplier<OciEnvConfigFactory.ConfigNodes> nodesSupplier) {
        this.prefix = Objects.requireNonNull(prefix);
        this.nodes = LazyValue.create(() -> {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Materializing oci-env config source for prefix ''{0}''", prefix);
            }
            return Objects.requireNonNull(nodesSupplier.get());
        });
    }

    @Override
    public String description() {
        return DESCRIPTION;
    }

    @Override
    public Optional<ConfigNode> node(String key) {
        if (!handles(key)) {
            return Optional.empty();
        }
        Optional<ConfigNode> result = Optional.ofNullable(nodes.get().nodesByKey().get(key));
        if (result.isEmpty() && LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG,
                       "oci-env source handles key ''{0}'' for prefix ''{1}'', but no value is published",
                       key,
                       prefix);
        }
        return result;
    }

    private boolean handles(String key) {
        return key.equals(prefix) || key.startsWith(prefix + ".");
    }
}

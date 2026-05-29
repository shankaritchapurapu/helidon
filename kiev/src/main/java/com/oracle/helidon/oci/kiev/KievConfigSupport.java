/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.kiev;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.builder.api.Prototype;

import com.oracle.pic.kiev.registry.data.ClientRegistryLocality;

/**
 * Builder decorator for cross-root Kiev configuration defaults.
 */
final class KievConfigSupport implements Prototype.BuilderDecorator<KievConfig.BuilderBase<?, ?>> {
    private static final System.Logger LOGGER = System.getLogger(KievConfigSupport.class.getName());

    KievConfigSupport() {
    }

    @Override
    public void decorate(KievConfig.BuilderBase<?, ?> builder) {
        // Keep root Config access lazy; explicit service.locality values do not need the oci-env default.
        Supplier<Optional<ClientRegistryLocality>> defaultLocality = lazyDefaultLocality(builder);
        List<KievStoreConfig> dataStores = builder.dataStores()
                .stream()
                .map(storeConfig -> applyLocalityDefault(storeConfig, defaultLocality))
                .toList();
        builder.dataStores(dataStores);
    }

    private static KievStoreConfig applyLocalityDefault(KievStoreConfig storeConfig,
                                                       Supplier<Optional<ClientRegistryLocality>> defaultLocality) {
        // Only SERVICE stores have locality, and configured locality always wins over the oci-env default.
        return storeConfig.service()
                .filter(serviceConfig -> serviceConfig.locality().isEmpty())
                .flatMap(serviceConfig -> defaultLocality.get()
                        .map(locality -> KievStoreConfig.builder(storeConfig)
                                .service(KievServiceConfig.builder(serviceConfig)
                                                 .locality(locality)
                                                 .buildPrototype())
                                .buildPrototype()))
                .orElse(storeConfig);
    }

    private static Supplier<Optional<ClientRegistryLocality>> lazyDefaultLocality(KievConfig.BuilderBase<?, ?> builder) {
        return new Supplier<>() {
            private Optional<ClientRegistryLocality> value;

            @Override
            public Optional<ClientRegistryLocality> get() {
                if (value == null) {
                    // Resolve oci.env once per KievConfig build, even when several stores need the default.
                    value = defaultLocality(builder);
                }
                return value;
            }
        };
    }

    private static Optional<ClientRegistryLocality> defaultLocality(KievConfig.BuilderBase<?, ?> builder) {
        /*
         * Kiev is configured under oci.kiev, while oci-env publishes location defaults at oci.env.
         * The decorator uses the oci.kiev config node root to reach oci.env, and only after a
         * SERVICE store with missing service.locality has been found.
         */
        return builder.config()
                .flatMap(config -> config.root()
                        .get("oci.env.ad-number")
                        .asString()
                        .asOptional())
                .flatMap(KievConfigSupport::toLocality);
    }

    private static Optional<ClientRegistryLocality> toLocality(String adNumber) {
        String locality = adNumber.trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(ClientRegistryLocality.valueOf(locality));
        } catch (IllegalArgumentException e) {
            // Warn for now, but keep startup compatible by falling back to the Kiev client locality default.
            LOGGER.log(System.Logger.Level.WARNING,
                       "Ignoring oci.env.ad-number {0}; expected a Kiev client registry locality value",
                       adNumber);
            return Optional.empty();
        }
    }
}

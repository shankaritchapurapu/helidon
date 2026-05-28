/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
 */

/**
 * OCI environment configuration support for Helidon.
 *
 * <p>The package exposes the {@code oci-env} Helidon config source and companion declarative
 * region providers backed by the same lazy OCI environment resolution model. The source can be
 * enabled explicitly from
 * {@link io.helidon.config.MetaConfig meta-config} with {@code type: "oci-env"} or, for
 * declarative {@code Services.get(Config.class)} bootstrap, it is auto-registered when Helidon
 * is using its default bootstrap and no explicit {@code meta-config.*} is present. Explicit
 * source properties are merged with {@code helidon.oci-env} from {@code oci-config.yaml}, with
 * source properties taking precedence per key.
 *
 * <p>Module features:
 * <ul>
 * <li>Lazy resolution of OCI environment metadata. Runtime file reads, derived value calculation,
 * and dynamic core-regions import happen only when a handled key or the
 * declarative region providers are requested.</li>
 * <li>Configurable published key prefix, default {@code oci.env}.</li>
 * <li>Publishes derived OCI environment values such as realm, region, availability-domain,
 * fault-domain, domain names, first region in realm, airport code, database TNS name, and ODO
 * metadata.</li>
 * <li>Resolves runtime location from standard OCI files such as {@code /etc/region},
 * {@code /etc/availability-domain}, {@code /etc/physical-availability-domain}, and
 * {@code /etc/fault-domain}.</li>
 * <li>Supports location overrides, physical availability-domain lookup, and a DEV location
 * override for local development.</li>
 * <li>Supports dynamic core-regions import with enable/disable, import-path, override-path, and
 * validation settings.</li>
 * <li>Higher-priority {@link io.helidon.integrations.oci.spi.OciRegion} implementation that
 * resolves the active public OCI SDK {@link com.oracle.bmc.Region} from the same environment
 * data and can register supported commons-core dynamic regions in the OCI SDK region catalog on
 * demand.</li>
 * <li>Declarative {@link com.oracle.pic.commons.util.Region} provider for services that need the
 * PIC commons/core region type directly.</li>
 * </ul>
 *
 * <p>See {@code docs/utilities/environment-config.md} for usage examples and full key/configuration
 * details.
 */
package com.oracle.helidon.oci.envconfig;

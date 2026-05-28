/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

/**
 * Secret Service config source integration.
 *
 * <p>The config source can be enabled explicitly from Helidon meta-config with
 * {@code type: "oci-secret-service"} or, for declarative {@code Services.get(Config.class)}
 * bootstrap, auto-registered when no explicit {@code meta-config.*} file is present. In that
 * declarative path it can read configuration from {@code helidon.oci-secret-service} in
 * {@code oci-config.yaml}. Explicit source properties are merged with
 * {@code helidon.oci-secret-service}, with source properties taking precedence per key.
 */
package com.oracle.helidon.oci.secret.config;

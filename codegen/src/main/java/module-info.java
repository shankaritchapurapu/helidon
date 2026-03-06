/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

import com.oracle.helidon.oci.codegen.OciAuthorizationExtensionProvider;

/**
 * Code generation support for OCI.
 */
module io.helidon.examples.oci.poc.codegen {
    requires io.helidon.codegen;
    requires io.helidon.codegen.classmodel;
    requires io.helidon.service.codegen;
    requires io.helidon.declarative.codegen.model;
    requires io.helidon.common;
    requires io.helidon.common.types;
    requires io.helidon.common.buffers;

    exports com.oracle.helidon.oci.codegen;

    provides io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider
            with OciAuthorizationExtensionProvider;
}
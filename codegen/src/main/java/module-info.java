/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

import com.oracle.helidon.oci.codegen.IdentityContextParameterCodegenProvider;
import com.oracle.helidon.oci.codegen.OciAuthorizationExtensionProvider;
import com.oracle.helidon.oci.codegen.OciHttpMetricsExtensionProvider;
import com.oracle.helidon.oci.codegen.OciHttpParameterLoaderExtensionProvider;
import com.oracle.helidon.oci.codegen.OciKievTransactionExtensionProvider;
import com.oracle.helidon.oci.codegen.OciMeteringExtensionProvider;
import com.oracle.helidon.oci.codegen.OciRequestIdParameterCodegenProvider;

/**
 * Code generation support for OCI.
 */
module io.helidon.examples.oci.poc.codegen {
    requires io.helidon.codegen;
    requires io.helidon.codegen.classmodel;
    requires io.helidon.service.codegen;
    requires io.helidon.declarative.codegen;
    requires io.helidon.declarative.codegen.model;
    requires io.helidon.common;
    requires io.helidon.common.types;
    requires io.helidon.common.buffers;

    exports com.oracle.helidon.oci.codegen;

    provides io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider
            with OciAuthorizationExtensionProvider,
                    OciHttpMetricsExtensionProvider,
                    OciKievTransactionExtensionProvider,
                    OciMeteringExtensionProvider,
                    OciHttpParameterLoaderExtensionProvider;

    provides io.helidon.declarative.codegen.http.webserver.spi.HttpParameterCodegenProvider
            with OciRequestIdParameterCodegenProvider,
                    IdentityContextParameterCodegenProvider;
}

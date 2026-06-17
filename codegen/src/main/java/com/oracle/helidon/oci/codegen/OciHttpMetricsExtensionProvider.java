/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;
import io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider;

/**
 * Java {@link java.util.ServiceLoader} provider for automatic HTTP metrics endpoint metadata codegen.
 */
public class OciHttpMetricsExtensionProvider implements RegistryCodegenExtensionProvider {
    /**
     * Default constructor.
     *
     * @deprecated required by Java {@link java.util.ServiceLoader}
     */
    @Deprecated
    public OciHttpMetricsExtensionProvider() {
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return OciHttpMetricsAnnotations.ALL;
    }

    @Override
    public RegistryCodegenExtension create(RegistryCodegenContext codegenContext) {
        return new OciHttpMetricsExtension(codegenContext);
    }
}

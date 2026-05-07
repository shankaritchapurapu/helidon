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
 * Provider for metering code generation support.
 */
public class OciMeteringExtensionProvider implements RegistryCodegenExtensionProvider {

    /**
     * Default constructor.
     *
     * @deprecated required by Java {@link java.util.ServiceLoader}
     */
    @Deprecated
    public OciMeteringExtensionProvider() {
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return OciMeteringExtension.ANNOTATIONS;
    }

    @Override
    public RegistryCodegenExtension create(RegistryCodegenContext codegenContext) {
        return new OciMeteringExtension(codegenContext);
    }
}

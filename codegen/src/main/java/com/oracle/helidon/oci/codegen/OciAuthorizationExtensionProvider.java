/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;
import io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider;

import static com.oracle.helidon.oci.codegen.OciTypes.AUTHORIZATION_PERMISSION;

/**
 * Java {@link java.util.ServiceLoader} provider implementation for
 * {@link io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider}.
 */
public class OciAuthorizationExtensionProvider implements RegistryCodegenExtensionProvider {

    /**
     * Default constructor.
     *
     * @deprecated required by Java {@link java.util.ServiceLoader}
     */
    @Deprecated
    public OciAuthorizationExtensionProvider() {
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(AUTHORIZATION_PERMISSION);
    }

    @Override
    public RegistryCodegenExtension create(RegistryCodegenContext codegenContext) {
        return new OciAuthorizationExtension(codegenContext);
    }
}

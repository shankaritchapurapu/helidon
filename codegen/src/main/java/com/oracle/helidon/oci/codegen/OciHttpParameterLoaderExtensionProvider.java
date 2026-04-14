/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.codegen;

import java.util.Set;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;
import io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider;

/**
 * Works around Helidon declarative APT loading {@code HttpParameterCodegenProvider} from the
 * thread context classloader. During annotation processing that classloader does not see
 * {@code helidon-oci-codegen}, so OCI parameter providers such as the {@code OciRequestId}
 * handler are not discovered unless we switch the TCCL first.
 */
@Weight(Weighted.DEFAULT_WEIGHT + 1000)
public class OciHttpParameterLoaderExtensionProvider implements RegistryCodegenExtensionProvider {

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of();
    }

    @Override
    public RegistryCodegenExtension create(RegistryCodegenContext codegenContext) {
        // RestServerExtension caches parameter providers when it is instantiated.
        Thread.currentThread().setContextClassLoader(OciHttpParameterLoaderExtensionProvider.class.getClassLoader());
        return roundContext -> {
        };
    }
}

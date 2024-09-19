/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.swagger.codegen.helidon;

import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenConfig;
import com.oracle.bmc.sdk.swagger.codegen.model.java.OracleJavaCodegenParameter;
import io.swagger.codegen.CodegenParameter;
import io.swagger.models.Swagger;

/**
 * Parameters that can be set for customizing Helidon based code generation from API specification.
 */
public class OracleJavaHelidonServiceCodegenParameter extends OracleJavaCodegenParameter {
    /**
     *
     * @param original
     * @param codegen
     * @param spec
     */
    public OracleJavaHelidonServiceCodegenParameter(
            CodegenParameter original,
            OracleCodegenConfig codegen,
            Swagger spec) {
        super(original, codegen, spec);
        if (codegen == null) {
            throw new NullPointerException("codegen can't be null");
        } else if (spec == null) {
            throw new NullPointerException("spec can't be null");
        }
    }

    /**
     * Returns the copy of current CodegenParameter.
     *
     * @return CodegenParameter
     */
    @Override
    public CodegenParameter copy() {
        CodegenParameter copy = super.copy();
        // no idea why this is not being copied over in CodegenParameter::copy (super.copy())
        copy.isPrimitiveType = this.isPrimitiveType;
        return copy;
    }
}

package com.oracle.helidon.integrations.oci.swagger.codegen.helidon;

import com.oracle.bmc.sdk.swagger.codegen.OracleCodegenConfig;
import com.oracle.bmc.sdk.swagger.codegen.model.java.OracleJavaCodegenParameter;
import io.swagger.codegen.CodegenParameter;
import io.swagger.models.Swagger;
import lombok.NonNull;

public class OracleJavaHelidonServiceCodegenParameter extends OracleJavaCodegenParameter {
    public OracleJavaHelidonServiceCodegenParameter(
            CodegenParameter original,
            @NonNull OracleCodegenConfig codegen,
            @NonNull Swagger spec) {
        super(original, codegen, spec);
    }

    @Override
    public CodegenParameter copy() {
        CodegenParameter copy = super.copy();
        // no idea why this is not being copied over in CodegenParameter::copy (super.copy())
        copy.isPrimitiveType = this.isPrimitiveType;
        return copy;
    }
}

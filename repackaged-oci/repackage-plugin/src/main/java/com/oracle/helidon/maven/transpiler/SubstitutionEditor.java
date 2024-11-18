/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.maven.transpiler;

import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

class SubstitutionEditor extends ExprEditor {

    private final Change change;

    SubstitutionEditor(Change change) {
        this.change = change;
    }

    public void edit(MethodCall m) throws CannotCompileException {
        Substitution substitution = change.getSubstitutions().get(m.getClassName() + "#" + m.getMethodName());
        if (substitution != null) {
            m.replace(substitution.getReplacement());
        }
    }
}

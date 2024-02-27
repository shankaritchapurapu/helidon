/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.maven.transpiler;

import java.util.Objects;

final class ClassMeta {
    private final String simpleName;
    private final String pkg;
    private final String fqdn;

    ClassMeta(String simpleName, String pkg, String fqdn) {
        this.simpleName = simpleName;
        this.pkg = pkg;
        this.fqdn = fqdn;
    }

    static ClassMeta parse(String fqdn) {
        int lastDot = fqdn.lastIndexOf(".");
        String pkg = fqdn.substring(0, lastDot);
        String simpleName = fqdn.substring(lastDot + 1);
        return new ClassMeta(simpleName, pkg, fqdn);
    }

    public String simpleName() {
        return simpleName;
    }

    public String pkg() {
        return pkg;
    }

    public String fqdn() {
        return fqdn;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ClassMeta) obj;
        return Objects.equals(this.simpleName, that.simpleName) &&
                Objects.equals(this.pkg, that.pkg) &&
                Objects.equals(this.fqdn, that.fqdn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simpleName, pkg, fqdn);
    }

    @Override
    public String toString() {
        return "ClassMeta[" +
                "simpleName=" + simpleName + ", " +
                "pkg=" + pkg + ", " +
                "fqdn=" + fqdn + ']';
    }

}

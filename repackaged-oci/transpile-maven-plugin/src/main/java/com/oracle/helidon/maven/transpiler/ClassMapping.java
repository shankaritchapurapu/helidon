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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ClassMapping {
    private static final Pattern PATTERN = Pattern.compile("([^:]*):([^:]*)");
    private final ClassMeta from;
    private final ClassMeta to;

    ClassMapping(ClassMeta from, ClassMeta to) {
        this.from = from;
        this.to = to;
    }

    static ClassMapping parse(String mapping) {
        Matcher m = PATTERN.matcher(mapping);
        if (!m.find()) {
            throw new RuntimeException("Bad class mapping! " + mapping);
        }
        var fromFqdn = m.group(1);
        var toFqdn = m.group(2);
        return new ClassMapping(ClassMeta.parse(fromFqdn), ClassMeta.parse(toFqdn));
    }

    public ClassMeta from() {
        return from;
    }

    public ClassMeta to() {
        return to;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ClassMapping) obj;
        return Objects.equals(this.from, that.from) &&
                Objects.equals(this.to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return "ClassMapping[" +
                "from=" + from + ", " +
                "to=" + to + ']';
    }

}

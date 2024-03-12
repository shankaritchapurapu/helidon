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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ClassMapping(ClassMeta from, ClassMeta to) {
    private static final Pattern PATTERN = Pattern.compile("([^:]*):([^:]*)");

    static ClassMapping parse(String mapping) {
        Matcher m = PATTERN.matcher(mapping);
        if (!m.find()) {
            throw new RuntimeException("Bad class mapping! " + mapping);
        }
        var fromFqdn = m.group(1);
        var toFqdn = m.group(2);
        return new ClassMapping(ClassMeta.parse(fromFqdn), ClassMeta.parse(toFqdn));
    }
}

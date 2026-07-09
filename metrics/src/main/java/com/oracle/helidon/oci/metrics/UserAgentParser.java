/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Optional;

interface UserAgentParser {
    Optional<UserAgentInfo> parse(String userAgentString);
}

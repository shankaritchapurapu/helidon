/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class TerraformUserAgentParser extends TwoPassUserAgentParser {
    private static final Set<String> PLATFORM_PARTS = Set.of("osname",
                                                             "osversion",
                                                             "lang",
                                                             "langversion",
                                                             "terraformversion");

    TerraformUserAgentParser(String name, String baseRegex, String platformRegex) {
        super(name,
              Pattern.compile(baseRegex, Pattern.CASE_INSENSITIVE),
              Pattern.compile(platformRegex, Pattern.CASE_INSENSITIVE),
              PLATFORM_PARTS);
    }

    @Override
    protected UserAgentInfo.Builder buildUserAgentInfo(String clientName,
                                                       String clientVersion,
                                                       Map<String, String> parsedPlatformParts) {
        UserAgentInfo.Builder builder = super.buildUserAgentInfo(clientName, clientVersion, parsedPlatformParts);
        if (parsedPlatformParts.containsKey("terraformversion")) {
            builder.extraInformation(List.of(parsedPlatformParts.get("terraformversion")));
        }
        return builder;
    }
}

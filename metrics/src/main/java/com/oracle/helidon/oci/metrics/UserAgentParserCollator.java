/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Applies the known user-agent parsers and returns the first parsed client identity.
 * Inspired by the similar service-core user-agent parser collator.
 */
final class UserAgentParserCollator {
    private static final String JAVA_PLATFORM_REGEX =
            "\\((?<osname>.*)\\/(?<osversion>\\S+); (?<lang>\\S+)\\/(?<langversion>\\S+);.*\\).*";
    private static final String PYTHON_PLATFORM_REGEX =
            "\\((?<lang>\\S+) (?<langversion>\\S+)(;\\s*(?<osname>\\S+).*)?\\)";
    private static final String PYTHON_PLATFORM_REGEX_WITH_TRAILING =
            "\\((?<lang>\\S+) (?<langversion>\\S+)(;\\s*(?<osname>\\S+).*)?\\).*";
    private static final String RUBY_PLATFORM_REGEX =
            "\\((?<lang>\\S+) (?<langversion>\\S+)(;\\s*(?<osname>\\S+).*)?\\).*";
    private static final String TERRAFORM_PLATFORM_REGEX =
            "\\((?<lang>\\S+)\\/(?<langversion>\\S+); (?<osname>\\S+)\\/(?<osversion>\\S+); "
                    + "(?<terraform>\\S+)\\/(?<terraformversion>\\S+).*\\).*";

    private final List<UserAgentParser> parsers;

    private UserAgentParserCollator(List<UserAgentParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    static UserAgentParserCollator createDefault() {
        return new UserAgentParserCollator(List.of(
                twoPass("HDFSConnector",
                        "^\\S*JavaSDK\\/([^\\(\\)]+)(?<platform>.*) (?<client>.*HDFS_Connector)\\/(?<version>\\S+).*",
                        JAVA_PLATFORM_REGEX),
                twoPass("Jenkins",
                        "^\\S*JavaSDK\\/([^\\(\\)]+)(?<platform>.*) (?<client>.*Jenkins)\\/(?<version>\\S+).*",
                        JAVA_PLATFORM_REGEX),
                twoPass("JavaSDK",
                        "^(?<client>\\S*JavaSDK)\\/(?<version>[^\\(\\)]+)(?<platform>.*)",
                        JAVA_PLATFORM_REGEX),
                twoPass("Ansible",
                        "^\\S*PythonSDK\\/([^\\(\\)]+)(?<platform>.*) (?<client>.*Ansible)\\/(?<version>\\S+).*",
                        PYTHON_PLATFORM_REGEX),
                twoPass("PythonCLI",
                        "^\\S*PythonSDK\\/([^\\(\\)]+)(?<platform>.*) (?<client>.*PythonCLI)\\/(?<version>\\S+).*",
                        PYTHON_PLATFORM_REGEX),
                twoPass("PythonSDK",
                        "^(?<client>\\S*PythonSDK)\\/(?<version>[^\\(\\)]+)(?<platform>(?!.*PythonCLI).*)",
                        PYTHON_PLATFORM_REGEX_WITH_TRAILING),
                twoPass("RubySDK",
                        "^(?<client>\\S*RubySDK)\\/(?<version>[^\\(\\)]+)(?<platform>(?!.*ChefKnife).*)",
                        RUBY_PLATFORM_REGEX),
                twoPass("ChefKnife",
                        "^\\S*RubySDK\\/([^\\(\\)]+)(?<platform>.*) (?<client>.*ChefKnife.*)\\/(?<version>\\S+).*",
                        RUBY_PLATFORM_REGEX),
                new TerraformUserAgentParser("TerraformProvider",
                                             "^\\S*GoSDK\\/([^\\(\\)]+)(?<platform>.*) "
                                                     + "(?<client>.*TerraformProvider.*)\\/(?<version>\\S+).*",
                                             TERRAFORM_PLATFORM_REGEX),
                new TerraformUserAgentParser("ORMTerraformer",
                                             "^\\S*GoSDK\\/([^\\(\\)]+)(?<platform>.*) "
                                                     + "(?<client>.*ORMTerraformer.*)\\/(?<version>\\S+).*",
                                             TERRAFORM_PLATFORM_REGEX),
                twoPass("GoSDK",
                        "^((?:(?<client>\\S*GoSDK)\\/)|baremetal-sdk-go-v)(?<version>[^\\(\\)\\s]+)"
                                + "(?<platform>\\s*\\(.*\\))?.*",
                        "(?<lang>\\S+)\\/(?<langversion>\\S+)(;\\s*(?<osname>darwin)\\/(?:\\S+).*)?"),
                new SingleRegexUserAgentParser("BrowserClient",
                                               Pattern.compile("^(Mozilla|Opera)/\\S+.*$",
                                                               Pattern.CASE_INSENSITIVE),
                                               (name, matcher) -> UserAgentInfo.builder()
                                                       .state(UserAgentInfo.State.DEFINED)
                                                       .clientName(name)
                                                       .build())));
    }

    UserAgentInfo parse(String userAgent) {
        if (isBlank(userAgent)) {
            return UserAgentInfo.UNDEFINED;
        }

        for (UserAgentParser parser : parsers) {
            try {
                Optional<UserAgentInfo> userAgentInfo = parser.parse(userAgent);
                if (userAgentInfo.isPresent()) {
                    return userAgentInfo.get();
                }
            } catch (RuntimeException e) {
                // Try the next known parser; an individual parser failure should not affect the request.
            }
        }
        return UserAgentInfo.UNKNOWN;
    }

    private static TwoPassUserAgentParser twoPass(String name, String baseRegex, String platformRegex) {
        return new TwoPassUserAgentParser.Builder(name, baseRegex, platformRegex).build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

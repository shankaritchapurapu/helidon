/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TwoPassUserAgentParser implements UserAgentParser {
    static final Set<String> DEFAULT_PLATFORM_PARTS = Set.of("osname", "osversion", "lang", "langversion");
    private static final Set<String> BASE_REGEX_PARTS = Set.of("client", "version", "platform");
    private static final String UNKNOWN = "UNKNOWN";

    private final String name;
    private final Pattern userAgentRegex;
    private final Pattern platformRegex;
    private final Set<String> platformParts;

    TwoPassUserAgentParser(String name, Pattern userAgentRegex, Pattern platformRegex, Set<String> platformParts) {
        this.name = name;
        this.userAgentRegex = userAgentRegex;
        this.platformRegex = platformRegex;
        this.platformParts = platformParts;
    }

    @Override
    public Optional<UserAgentInfo> parse(String userAgentString) {
        if (isBlank(userAgentString)) {
            return Optional.empty();
        }

        Matcher matcher = userAgentRegex.matcher(userAgentString);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String platform = matcher.group("platform");
        Map<String, String> parsedPlatformParts = parsePlatform(platform);
        String clientVersion = UserAgentInfo.sanitizeStringForMetric(matcher.group("version"));
        return Optional.of(buildUserAgentInfo(name, clientVersion, parsedPlatformParts).build());
    }

    protected UserAgentInfo.Builder buildUserAgentInfo(String clientName,
                                                       String clientVersion,
                                                       Map<String, String> parsedPlatformParts) {
        return UserAgentInfo.builder()
                .clientName(clientName)
                .clientVersion(clientVersion)
                .state(UserAgentInfo.State.DEFINED)
                .osName(parsedPlatformParts.getOrDefault("osname", UNKNOWN))
                .osVersion(parsedPlatformParts.getOrDefault("osversion", UNKNOWN))
                .lang(parsedPlatformParts.getOrDefault("lang", UNKNOWN))
                .langVersion(parsedPlatformParts.getOrDefault("langversion", UNKNOWN));
    }

    protected Map<String, String> parsePlatform(String platform) {
        if (isBlank(platform)) {
            return Collections.emptyMap();
        }

        Matcher matcher = platformRegex.matcher(platform.trim());
        if (!matcher.matches()) {
            return Collections.emptyMap();
        }

        Map<String, String> metricParts = new HashMap<>();
        for (String part : platformParts) {
            try {
                String sanitized = UserAgentInfo.sanitizeStringForMetric(matcher.group(part));
                if (!isBlank(sanitized)) {
                    metricParts.put(part, sanitized);
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // The selected platform regex might not define every platform part.
            }
        }
        return metricParts;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class Builder {
        private final String name;
        private final String baseRegex;
        private final String platformRegex;
        private final Set<String> platformParts;

        Builder(String name, String baseRegex, String platformRegex) {
            this(name, baseRegex, platformRegex, DEFAULT_PLATFORM_PARTS);
        }

        Builder(String name, String baseRegex, String platformRegex, Set<String> platformParts) {
            this.name = name;
            this.baseRegex = baseRegex;
            this.platformRegex = platformRegex;
            this.platformParts = platformParts;
        }

        TwoPassUserAgentParser build() {
            if (isBlank(name)) {
                throw new IllegalArgumentException("The name can not be blank or null");
            }
            if (isBlank(baseRegex)) {
                throw new IllegalArgumentException("Base regex can not be blank or null");
            }
            if (!stringContainsAll(baseRegex, BASE_REGEX_PARTS)) {
                throw new IllegalArgumentException("The base regex must contain groups");
            }
            if (isBlank(platformRegex)) {
                throw new IllegalArgumentException("Platform regex can not be blank or null");
            }

            return new TwoPassUserAgentParser(name,
                                             Pattern.compile(baseRegex, Pattern.CASE_INSENSITIVE),
                                             Pattern.compile(platformRegex, Pattern.CASE_INSENSITIVE),
                                             platformParts);
        }

        private static boolean stringContainsAll(String string, Collection<String> parts) {
            return parts.stream().allMatch(string::contains);
        }
    }
}

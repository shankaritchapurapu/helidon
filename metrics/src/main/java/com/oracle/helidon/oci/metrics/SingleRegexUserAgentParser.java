/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SingleRegexUserAgentParser implements UserAgentParser {
    private final Pattern pattern;
    private final String name;
    private final BiFunction<String, Matcher, UserAgentInfo> userAgentInfoAdapter;

    SingleRegexUserAgentParser(String name, Pattern regex, BiFunction<String, Matcher, UserAgentInfo> adapter) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Name of parser needs a name");
        }
        this.pattern = Objects.requireNonNull(regex, "Regex can not be blank");
        this.name = name;
        this.userAgentInfoAdapter = Objects.requireNonNull(adapter, "user agent info adapter needs to be set");
    }

    @Override
    public Optional<UserAgentInfo> parse(String userAgentString) {
        if (isBlank(userAgentString)) {
            return Optional.empty();
        }

        Matcher matcher = pattern.matcher(userAgentString);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.ofNullable(userAgentInfoAdapter.apply(name, matcher));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

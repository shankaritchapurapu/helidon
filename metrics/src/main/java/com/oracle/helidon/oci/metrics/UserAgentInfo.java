/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class UserAgentInfo {
    static final UserAgentInfo UNKNOWN = new UserAgentInfo(State.UNKNOWN);
    static final UserAgentInfo UNDEFINED = new UserAgentInfo(State.UNDEFINED);

    private static final Pattern SANITIZE_REGEX = Pattern.compile("[^\\p{Alnum}]");
    private static final String UNKNOWN_VALUE = "UNKNOWN";
    private static final String METRIC_DELIMITER = ".";

    private final String clientName;
    private final String clientVersion;
    private final String osName;
    private final String osVersion;
    private final String lang;
    private final String langVersion;
    private final List<String> extraInformation;
    private final State state;

    private UserAgentInfo(State state) {
        this(null, null, null, null, null, null, null, state);
    }

    private UserAgentInfo(String clientName,
                          String clientVersion,
                          String osName,
                          String osVersion,
                          String lang,
                          String langVersion,
                          List<String> extraInformation,
                          State state) {
        this.clientName = Optional.ofNullable(clientName).orElse(UNKNOWN_VALUE);
        this.clientVersion = Optional.ofNullable(clientVersion).orElse(UNKNOWN_VALUE);
        this.osName = Optional.ofNullable(osName).orElse(UNKNOWN_VALUE);
        this.osVersion = Optional.ofNullable(osVersion).orElse(UNKNOWN_VALUE);
        this.lang = Optional.ofNullable(lang).orElse(UNKNOWN_VALUE);
        this.langVersion = Optional.ofNullable(langVersion).orElse(UNKNOWN_VALUE);
        this.extraInformation = List.copyOf(Optional.ofNullable(extraInformation).orElse(Collections.emptyList()));
        this.state = Optional.ofNullable(state).orElse(State.UNDEFINED);
    }

    static Builder builder() {
        return new Builder();
    }

    static String sanitizeStringForMetric(String metricName) {
        if (isBlank(metricName)) {
            return "";
        }
        return SANITIZE_REGEX.matcher(metricName).replaceAll("").toLowerCase(Locale.ENGLISH);
    }

    List<String> toAggregatedMetricStrings() {
        if (state != State.DEFINED) {
            return List.of(state.name());
        }

        List<String> aggMetrics = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (String part : fieldsAsList()) {
            sb.append(part);
            aggMetrics.add(sb.toString());
            sb.append(METRIC_DELIMITER);
        }
        return aggMetrics;
    }

    String toMetricsString() {
        if (state != State.DEFINED) {
            return state.name();
        }
        return String.join(METRIC_DELIMITER, fieldsAsList());
    }

    private List<String> fieldsAsList() {
        List<String> allFieldValues = new ArrayList<>(6 + extraInformation.size());
        allFieldValues.add(clientName);
        allFieldValues.add(clientVersion);
        allFieldValues.add(osName);
        allFieldValues.add(osVersion);
        allFieldValues.add(lang);
        allFieldValues.add(langVersion);
        allFieldValues.addAll(extraInformation);
        return allFieldValues;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    enum State {
        DEFINED,
        UNKNOWN,
        UNDEFINED
    }

    static final class Builder {
        private String clientName;
        private String clientVersion;
        private String osName;
        private String osVersion;
        private String lang;
        private String langVersion;
        private List<String> extraInformation;
        private State state;

        private Builder() {
        }

        Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        Builder osName(String osName) {
            this.osName = osName;
            return this;
        }

        Builder osVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }

        Builder lang(String lang) {
            this.lang = lang;
            return this;
        }

        Builder langVersion(String langVersion) {
            this.langVersion = langVersion;
            return this;
        }

        Builder extraInformation(List<String> extraInformation) {
            this.extraInformation = extraInformation;
            return this;
        }

        Builder state(State state) {
            this.state = state;
            return this;
        }

        UserAgentInfo build() {
            return new UserAgentInfo(clientName,
                                     clientVersion,
                                     osName,
                                     osVersion,
                                     lang,
                                     langVersion,
                                     extraInformation,
                                     state);
        }
    }
}

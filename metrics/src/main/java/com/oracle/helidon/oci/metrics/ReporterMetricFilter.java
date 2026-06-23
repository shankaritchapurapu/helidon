/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.helidon.config.ConfigException;
import io.helidon.metrics.api.Meter;

final class ReporterMetricFilter implements BiFunction<String, Meter, Boolean> {

    private static final ReporterMetricFilter ALLOW_ALL =
            new ReporterMetricFilter(Mode.ALL,
                                     Set.of(),
                                     Set.of(),
                                     List.of(),
                                     List.of(),
                                     List.of(),
                                     List.of(),
                                     null);

    private final Mode mode;
    private final Set<String> includes;
    private final Set<String> excludes;
    private final List<Pattern> includePatterns;
    private final List<Pattern> excludePatterns;
    private final List<String> includeSubstrings;
    private final List<String> excludeSubstrings;
    private final ConcurrentMap<String, Boolean> decisions;

    private ReporterMetricFilter(Mode mode,
                                 Set<String> includes,
                                 Set<String> excludes,
                                 List<Pattern> includePatterns,
                                 List<Pattern> excludePatterns,
                                 List<String> includeSubstrings,
                                 List<String> excludeSubstrings,
                                 ConcurrentMap<String, Boolean> decisions) {
        this.mode = mode;
        this.includes = Set.copyOf(includes);
        this.excludes = Set.copyOf(excludes);
        this.includePatterns = List.copyOf(includePatterns);
        this.excludePatterns = List.copyOf(excludePatterns);
        this.includeSubstrings = List.copyOf(includeSubstrings);
        this.excludeSubstrings = List.copyOf(excludeSubstrings);
        this.decisions = decisions;
    }

    static ReporterMetricFilter allowAll() {
        return ALLOW_ALL;
    }

    static ReporterMetricFilter create(OciMetricsPublisherConfig.BuilderBase<?, ?> builder) {
        return create(builder.includes(), builder.excludes(), builder.filterMatchingMode());
    }

    private static ReporterMetricFilter create(Set<String> includes,
                                               Set<String> excludes,
                                               FilterMatchingMode filterMatchingMode) {
        if (includes.isEmpty() && excludes.isEmpty()) {
            return ALLOW_ALL;
        }
        if (filterMatchingMode == FilterMatchingMode.REGEX) {
            return new ReporterMetricFilter(Mode.REGEX,
                                            includes,
                                            excludes,
                                            compilePatterns("includes", includes),
                                            compilePatterns("excludes", excludes),
                                            List.of(),
                                            List.of(),
                                            new ConcurrentHashMap<>());
        }
        if (filterMatchingMode == FilterMatchingMode.SUBSTRING) {
            return new ReporterMetricFilter(Mode.SUBSTRING,
                                            includes,
                                            excludes,
                                            List.of(),
                                            List.of(),
                                            List.copyOf(includes),
                                            List.copyOf(excludes),
                                            new ConcurrentHashMap<>());
        }
        return new ReporterMetricFilter(Mode.EXACT,
                                        includes,
                                        excludes,
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        null);
    }

    @Override
    public Boolean apply(String metricName, Meter meter) {
        Objects.requireNonNull(metricName);
        return switch (mode) {
        case ALL -> true;
        case EXACT -> exactDecision(metricName);
        case REGEX, SUBSTRING -> decisions.computeIfAbsent(metricName, this::computeDecision);
        };
    }

    private boolean computeDecision(String metricName) {
        return switch (mode) {
        case ALL -> true;
        case EXACT -> exactDecision(metricName);
        case REGEX -> !matches(excludePatterns, metricName) && (includes.isEmpty() || matches(includePatterns, metricName));
        case SUBSTRING -> !contains(excludeSubstrings, metricName)
                && (includeSubstrings.isEmpty() || contains(includeSubstrings, metricName));
        };
    }

    private boolean exactDecision(String metricName) {
        return !excludes.contains(metricName) && (includes.isEmpty() || includes.contains(metricName));
    }

    private static boolean matches(List<Pattern> patterns, String metricName) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(metricName).matches());
    }

    private static List<Pattern> compilePatterns(String settingName, Set<String> expressions) {
        return expressions.stream()
                .map(expression -> compilePattern(settingName, expression))
                .toList();
    }

    private static Pattern compilePattern(String settingName, String expression) {
        try {
            return Pattern.compile(expression);
        } catch (PatternSyntaxException e) {
            throw new ConfigException("Invalid OCI metrics publisher regex in "
                                              + settingName + ": '" + expression + "'", e);
        }
    }

    private static boolean contains(List<String> expressions, String metricName) {
        return expressions.stream().anyMatch(metricName::contains);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReporterMetricFilter that)) {
            return false;
        }
        return mode == that.mode && includes.equals(that.includes) && excludes.equals(that.excludes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, includes, excludes);
    }

    private enum Mode {
        ALL,
        EXACT,
        REGEX,
        SUBSTRING
    }
}

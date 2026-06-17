/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;

import io.helidon.config.ConfigException;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OciMetricsPublisherFilterTest {

    @Test
    void defaultOciMetricsPublisherConfigPublishesAllMetrics() {
        BiFunction<String, Meter, Boolean> filter = filter(builder -> {
        });
        OciMetricsPublisher publisher = publisher(builder -> {
        });

        assertThat(filter.apply("test.counter", null), is(true));
        assertThat(filter.apply("test.timer", null), is(true));
        assertThat(publisher.shouldPublishAttribute(OciMetricsPublisher.VALUE_ATTRIBUTE), is(true));
    }

    @Test
    void exactIncludesAllowOnlyListedMetricNames() {
        BiFunction<String, Meter, Boolean> filter = filter(builder -> builder.includes(Set.of("test.counter")));

        assertThat(filter.apply("test.counter", null), is(true));
        assertThat(filter.apply("test.timer", null), is(false));
    }

    @Test
    void exactExcludesSuppressListedMetricNames() {
        BiFunction<String, Meter, Boolean> filter = filter(builder -> builder.excludes(Set.of("test.counter")));

        assertThat(filter.apply("test.counter", null), is(false));
        assertThat(filter.apply("test.timer", null), is(true));
    }

    @Test
    void exactExcludesWinOverIncludes() {
        BiFunction<String, Meter, Boolean> filter = filter(builder -> builder.includes(Set.of("test.counter"))
                .excludes(Set.of("test.counter")));

        assertThat(filter.apply("test.counter", null), is(false));
    }

    @Test
    void regexFiltersUseFullPatternMatch() {
        BiFunction<String, Meter, Boolean> filter = filter(builder -> builder.useRegexFilters(true)
                .includes(Set.of("test\\..*")));

        assertThat(filter.apply("test.counter", null), is(true));
        assertThat(filter.apply("custom.test.counter", null), is(false));
    }

    @Test
    void substringFiltersUseNameContainment() {
        BiFunction<String, Meter, Boolean> filter = filter(builder -> builder.useSubstringMatching(true)
                .includes(Set.of("counter"))
                .excludes(Set.of("internal")));

        assertThat(filter.apply("test.counter", null), is(true));
        assertThat(filter.apply("test.timer", null), is(false));
        assertThat(filter.apply("internal.counter", null), is(false));
    }

    @Test
    void failsWhenRegexAndSubstringFiltersAreBothEnabled() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                 () -> publisherConfig(builder -> builder.useRegexFilters(true)
                                                         .useSubstringMatching(true)));

        assertThat(exception.getMessage(), containsString("do not enable both use-regex-filters and use-substring-matching"));
        assertThat(exception.getMessage(), containsString("choose either regex or substring matching"));
    }

    @Test
    void invalidIncludeRegexFailsWithClearMessage() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                 () -> publisherConfig(builder -> builder.useRegexFilters(true)
                                                         .includes(Set.of("test["))));

        assertThat(exception.getMessage(), containsString("Invalid OCI metrics publisher regex in includes: 'test['"));
        assertThat(exception.getCause(), instanceOf(PatternSyntaxException.class));
    }

    @Test
    void invalidExcludeRegexFailsWithClearMessage() {
        ConfigException exception = assertThrows(ConfigException.class,
                                                 () -> publisherConfig(builder -> builder.useRegexFilters(true)
                                                         .excludes(Set.of("test["))));

        assertThat(exception.getMessage(), containsString("Invalid OCI metrics publisher regex in excludes: 'test['"));
        assertThat(exception.getCause(), instanceOf(PatternSyntaxException.class));
    }

    @Test
    void defaultOciMetricsPublisherConfigFilterReflectsConfiguredSettings() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig(builder -> builder.includes(Set.of("test.counter"))
                .excludes(Set.of("test.internal"))
                .useSubstringMatching(true));

        assertThat(publisherConfig.filter(), instanceOf(ReporterMetricFilter.class));
        assertThat(publisherConfig.filter().apply("test.counter", null), is(true));
        assertThat(publisherConfig.filter().apply("test.timer", null), is(false));
        assertThat(publisherConfig.filter().apply("test.internal.counter", null), is(false));
    }

    @Test
    void customFilterOverridesConfiguredIncludesAndExcludes() {
        BiFunction<String, Meter, Boolean> customFilter = (name, meter) -> "custom.allowed".equals(name);
        OciMetricsPublisherConfig publisherConfig = publisherConfig(builder -> builder.includes(Set.of("configured.allowed"))
                .excludes(Set.of("custom.allowed"))
                .filter(customFilter));

        assertThat(publisherConfig.filter(), sameInstance(customFilter));
        assertThat(publisherConfig.filter().apply("custom.allowed", null), is(true));
        assertThat(publisherConfig.filter().apply("configured.allowed", null), is(false));
    }

    @Test
    void publisherConfigEqualityIncludesCustomFilter() {
        OciMetricsPublisherConfig first = publisherConfig(builder -> builder.filter((name, meter) -> true));
        OciMetricsPublisherConfig second = publisherConfig(builder -> builder.filter((name, meter) -> true));

        assertThat(first, not(equalTo(second)));
    }

    @Test
    void equivalentDefaultReporterMetricFiltersCompareEqual() {
        OciMetricsPublisherConfig first = publisherConfig(builder -> builder.includes(Set.of("test.counter"))
                .excludes(Set.of("test.internal"))
                .useRegexFilters(true));
        OciMetricsPublisherConfig second = publisherConfig(builder -> builder.includes(Set.of("test.counter"))
                .excludes(Set.of("test.internal"))
                .useRegexFilters(true));

        assertThat(first.filter(), equalTo(second.filter()));
        assertThat(first, equalTo(second));
    }

    @Test
    void publisherConfigToStringOmitsFilter() {
        OciMetricsPublisherConfig publisherConfig = publisherConfig(builder -> builder.filter((name, meter) -> true));

        assertThat(publisherConfig.toString(), not(containsString("filter")));
    }

    @Test
    void publisherDelegatesToReporterFilter() {
        TestMeter meter = new TestMeter("test.counter");
        BiFunction<String, Meter, Boolean> customFilter = (name, actualMeter) ->
                "test.counter".equals(name) && actualMeter == meter;
        OciMetricsPublisher publisher = publisher(builder -> builder.filter(customFilter));

        assertThat(publisher.shouldPublish(meter), is(true));
        assertThat(publisher.shouldPublish(new TestMeter("test.counter")), is(false));
    }

    @Test
    void attributeIncludesAllowOnlyListedAttributes() {
        OciMetricsPublisher publisher = publisher(builder -> builder.includesAttributes(
                Set.of(OciMetricsPublisher.VALUE_ATTRIBUTE)));

        assertThat(publisher.shouldPublishAttribute(OciMetricsPublisher.VALUE_ATTRIBUTE), is(true));
        assertThat(publisher.shouldPublishAttribute("count"), is(false));
    }

    @Test
    void attributeExcludesSuppressListedAttributes() {
        OciMetricsPublisher publisher = publisher(builder -> builder.excludesAttributes(
                Set.of(OciMetricsPublisher.VALUE_ATTRIBUTE)));

        assertThat(publisher.shouldPublishAttribute(OciMetricsPublisher.VALUE_ATTRIBUTE), is(false));
        assertThat(publisher.shouldPublishAttribute("count"), is(true));
    }

    @Test
    void attributeExcludesWinOverIncludes() {
        OciMetricsPublisher publisher = publisher(builder -> builder.includesAttributes(
                        Set.of(OciMetricsPublisher.VALUE_ATTRIBUTE))
                .excludesAttributes(Set.of(OciMetricsPublisher.VALUE_ATTRIBUTE)));

        assertThat(publisher.shouldPublishAttribute(OciMetricsPublisher.VALUE_ATTRIBUTE), is(false));
    }

    @Test
    void metricNameFilterSuppressesBeforeAttributeFilter() {
        OciMetricsPublisher publisher = publisher(builder -> builder.includes(Set.of("test.allowed"))
                .includesAttributes(Set.of(OciMetricsPublisher.VALUE_ATTRIBUTE)));

        assertThat(publisher.shouldPublish(new TestMeter("test.counter"), OciMetricsPublisher.VALUE_ATTRIBUTE),
                   is(false));
        assertThat(publisher.shouldPublish(new TestMeter("test.allowed"), OciMetricsPublisher.VALUE_ATTRIBUTE),
                   is(true));
    }

    private static BiFunction<String, Meter, Boolean> filter(Consumer<OciMetricsPublisherConfig.Builder> publisherConsumer) {
        return publisherConfig(publisherConsumer).filter();
    }

    private static OciMetricsPublisherConfig publisherConfig(Consumer<OciMetricsPublisherConfig.Builder> publisherConsumer) {
        OciMetricsPublisherConfig.Builder publisherBuilder = OciMetricsPublisherConfig.builder();
        publisherConsumer.accept(publisherBuilder);
        return publisherBuilder.buildPrototype();
    }

    private static OciMetricsPublisher publisher(Consumer<OciMetricsPublisherConfig.Builder> publisherConsumer) {
        return OciMetricsPublisher.create(publisherConfig(publisherConsumer));
    }

    private record TestMeter(String name) implements Meter {
        @Override
        public Id id() {
            return new TestId(name);
        }

        @Override
        public Optional<String> baseUnit() {
            return Optional.empty();
        }

        @Override
        public Optional<String> description() {
            return Optional.empty();
        }

        @Override
        public Type type() {
            return Type.COUNTER;
        }

        @Override
        public Optional<String> scope() {
            return Optional.empty();
        }

        @Override
        public <R> R unwrap(Class<? extends R> type) {
            if (type.isInstance(this)) {
                return type.cast(this);
            }
            throw new IllegalArgumentException("Unsupported unwrap type: " + type.getName());
        }
    }

    private record TestId(String name) implements Meter.Id {
        @Override
        public Iterable<Tag> tags() {
            return List.of();
        }
    }
}

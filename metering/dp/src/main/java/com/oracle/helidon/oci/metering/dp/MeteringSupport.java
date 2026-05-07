/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.dp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;

import com.oracle.pic.bling.clients.ingest.model.Meter;
import com.oracle.pic.bling.clients.ingest.model.Meters;
import com.oracle.pic.bling.usagerecorder.LogFileUsageRecorder;

/**
 * Runtime support for generated data plane metering interceptors.
 */
@Service.Singleton
public class MeteringSupport {
    private final LogFileUsageRecorder recorder;
    private final MeteringConfig config;

    @Service.Inject
    MeteringSupport(LogFileUsageRecorder recorder, MeteringConfig config) {
        this.recorder = recorder;
        this.config = config;
    }

    void recordPoint(String meterName,
                     String compartmentId,
                     String resourceId,
                     float amount,
                     Map<String, String> tags) throws Exception {
        long now = Instant.now().toEpochMilli();
        record(meterName, compartmentId, resourceId, now, now, amount, tags);
    }

    void recordTimed(String meterName,
                     String compartmentId,
                     String resourceId,
                     long fromMillis,
                     long toMillis,
                     Map<String, String> tags) throws Exception {
        record(meterName, compartmentId, resourceId, fromMillis, toMillis, elapsedSeconds(fromMillis, toMillis), tags);
    }

    void startRegion(Context context,
                     String meterName,
                     String compartmentId,
                     String resourceId,
                     Map<String, String> tags) {
        if (context.get(MeteringRegionContext.class).isPresent()) {
            throw new IllegalStateException("A metering region is already active in the current context");
        }
        context.register(new MeteringRegionContext(meterName,
                                                   compartmentId,
                                                   resourceId,
                                                   Instant.now().toEpochMilli(),
                                                   tags));
    }

    void endRegion(Context context,
                   String meterName,
                   Map<String, String> tags) throws Exception {
        MeteringRegionContext region = context.get(MeteringRegionContext.class)
                .orElseThrow(() -> new IllegalStateException("No active metered region in the current context"));
        try {
            long toMillis = Instant.now().toEpochMilli();
            Map<String, String> effectiveTags = Tags.merge(region.tags(), tags);
            String effectiveMeterName = meterName == null || meterName.isBlank() ? region.meterName() : meterName;
            record(effectiveMeterName,
                   region.compartmentId(),
                   region.resourceId(),
                   region.fromMillis(),
                   toMillis,
                   elapsedSeconds(region.fromMillis(), toMillis),
                   effectiveTags);
        } finally {
            context.unregister(region);
        }
    }

    private void record(String meterName,
                        String compartmentId,
                        String resourceId,
                        long fromMillis,
                        long toMillis,
                        float value,
                        Map<String, String> tags) throws Exception {
        Meter meter = Meter.builder()
                .mtr(meterName)
                .compartmentId(compartmentId)
                .resourceId(resourceId)
                .from(fromMillis)
                .to(toMillis)
                .value(value)
                .tags(Tags.toJson(tags))
                .build();
        Meters meters = Meters.builder()
                .compartmentId(compartmentId)
                .timestamp(toMillis)
                .from(fromMillis)
                .to(toMillis)
                .service(config.service().orElse(null))
                .metrics(List.of(meter))
                .build();

        recorder.recordMetersAsync(List.of(meters)).call();
    }

    private static float elapsedSeconds(long fromMillis, long toMillis) {
        return (toMillis - fromMillis) / 1000.0F;
    }

    /**
     * Base interceptor for {@link Metering.Point}.
     */
    public abstract static class PointMethod extends MeteringMethod {
        /**
         * Constructor with no side effects.
         */
        protected PointMethod() {
        }

        @Override
        public <V> V proceed(InterceptionContext ctx, Chain<V> chain, Object... args) throws Exception {
            V result = chain.proceed(args);
            support().recordPoint(meterName(),
                                  stringArg(args, compartmentIdParameterIndex(), compartmentId()),
                                  stringArg(args, resourceIdParameterIndex(), resourceId()),
                                  amount(args),
                                  tags(args));
            return result;
        }

        /**
         * Recorded amount.
         *
         * @return amount
         */
        protected abstract float amount();

        /**
         * Amount parameter index.
         *
         * @return parameter index, or {@code -1}
         */
        protected abstract int amountParameterIndex();

        private float amount(Object[] args) {
            int index = amountParameterIndex();
            if (index < 0) {
                return amount();
            }
            Object value = args[index];
            if (value instanceof Number number) {
                return number.floatValue();
            }
            if (value instanceof CharSequence text) {
                return Float.parseFloat(text.toString());
            }
            throw new IllegalStateException("Metering amount parameter must be numeric or text");
        }
    }

    /**
     * Base interceptor for {@link Metering.Timed}.
     */
    public abstract static class TimedMethod extends MeteringMethod {
        /**
         * Constructor with no side effects.
         */
        protected TimedMethod() {
        }

        @Override
        public <V> V proceed(InterceptionContext ctx, Chain<V> chain, Object... args) throws Exception {
            long fromMillis = Instant.now().toEpochMilli();
            V result = chain.proceed(args);
            support().recordTimed(meterName(),
                                  stringArg(args, compartmentIdParameterIndex(), compartmentId()),
                                  stringArg(args, resourceIdParameterIndex(), resourceId()),
                                  fromMillis,
                                  Instant.now().toEpochMilli(),
                                  tags(args));
            return result;
        }
    }

    /**
     * Base interceptor for {@link Metering.Start}.
     */
    public abstract static class StartMethod extends MeteringMethod {
        /**
         * Constructor with no side effects.
         */
        protected StartMethod() {
        }

        @Override
        public <V> V proceed(InterceptionContext ctx, Chain<V> chain, Object... args) throws Exception {
            support().startRegion(context(args),
                                  meterName(),
                                  stringArg(args, compartmentIdParameterIndex(), compartmentId()),
                                  stringArg(args, resourceIdParameterIndex(), resourceId()),
                                  tags(args));
            return chain.proceed(args);
        }
    }

    /**
     * Base interceptor for {@link Metering.End}.
     */
    public abstract static class EndMethod extends InterceptionBase {
        /**
         * Constructor with no side effects.
         */
        protected EndMethod() {
        }

        @Override
        public <V> V proceed(InterceptionContext ctx, Chain<V> chain, Object... args) throws Exception {
            V result = chain.proceed(args);
            support().endRegion(context(args), meterName(), tags(args));
            return result;
        }

        /**
         * Metering support.
         *
         * @return support
         */
        protected abstract MeteringSupport support();

        /**
         * Meter name.
         *
         * @return meter name
         */
        protected abstract String meterName();

        /**
         * Static tags.
         *
         * @return tags
         */
        protected abstract Map<String, String> staticTags();

        /**
         * Tag parameter indexes.
         *
         * @return tag indexes by key
         */
        protected abstract Map<String, Integer> tagParameterIndexes();

        private Map<String, String> tags(Object[] args) {
            return Tags.from(staticTags(), tagParameterIndexes(), args);
        }
    }

    /**
     * Shared base for generated metering interceptors.
     */
    public abstract static class MeteringMethod extends InterceptionBase {
        /**
         * Constructor with no side effects.
         */
        protected MeteringMethod() {
        }

        /**
         * Metering support.
         *
         * @return support
         */
        protected abstract MeteringSupport support();

        /**
         * Meter name.
         *
         * @return meter name
         */
        protected abstract String meterName();

        /**
         * Static compartment ID.
         *
         * @return compartment ID
         */
        protected abstract String compartmentId();

        /**
         * Compartment ID parameter index.
         *
         * @return parameter index, or {@code -1}
         */
        protected abstract int compartmentIdParameterIndex();

        /**
         * Static resource ID.
         *
         * @return resource ID
         */
        protected abstract String resourceId();

        /**
         * Resource ID parameter index.
         *
         * @return parameter index, or {@code -1}
         */
        protected abstract int resourceIdParameterIndex();

        /**
         * Static tags.
         *
         * @return tags
         */
        protected abstract Map<String, String> staticTags();

        /**
         * Tag parameter indexes.
         *
         * @return tag indexes by key
         */
        protected abstract Map<String, Integer> tagParameterIndexes();

        Map<String, String> tags(Object[] args) {
            return Tags.from(staticTags(), tagParameterIndexes(), args);
        }
    }

    private abstract static class InterceptionBase implements Interception.ElementInterceptor {
        Context context(Object[] args) {
            return Contexts.context().orElseGet(() -> contextArg(args));
        }

        String stringArg(Object[] args, int index, String configured) {
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
            if (index < 0) {
                return null;
            }
            Object value = args[index];
            return value == null ? null : value.toString();
        }

        private Context contextArg(Object[] args) {
            for (Object arg : args) {
                if (arg instanceof Context context) {
                    return context;
                }
            }
            throw new IllegalStateException("Metering regions require a current Helidon Context or a Context argument");
        }
    }

    private record MeteringRegionContext(String meterName,
                                         String compartmentId,
                                         String resourceId,
                                         long fromMillis,
                                         Map<String, String> tags) {
    }

    private static final class Tags {
        private Tags() {
        }

        static Map<String, String> from(Map<String, String> staticTags,
                                        Map<String, Integer> tagParameterIndexes,
                                        Object[] args) {
            if (tagParameterIndexes.isEmpty()) {
                return staticTags;
            }
            java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>(staticTags);
            tagParameterIndexes.forEach((key, index) -> {
                Object value = args[index];
                if (value != null) {
                    result.put(key, value.toString());
                }
            });
            return Map.copyOf(result);
        }

        static Map<String, String> merge(Map<String, String> first, Map<String, String> second) {
            if (first.isEmpty()) {
                return second;
            }
            if (second.isEmpty()) {
                return first;
            }
            java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>(first);
            result.putAll(second);
            return Map.copyOf(result);
        }

        static String toJson(Map<String, String> tags) {
            if (tags.isEmpty()) {
                return null;
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(escape(entry.getKey())).append("\":\"")
                        .append(escape(entry.getValue())).append('"');
            }
            return builder.append('}').toString();
        }

        private static String escape(String text) {
            return text.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}

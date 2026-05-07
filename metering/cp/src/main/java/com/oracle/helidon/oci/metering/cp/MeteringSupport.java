/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Instant;
import java.util.Map;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;

import com.oracle.pic.kiev.Transaction;

/**
 * Runtime support for generated control plane metering interceptors.
 */
@Service.Singleton
public class MeteringSupport {
    private final MeteringRecorder recorder;

    @Service.Inject
    MeteringSupport(MeteringRecorder recorder) {
        this.recorder = recorder;
    }

    void recordPoint(String meterName,
                     String compartmentId,
                     String resourceId,
                     float amount,
                     Map<String, String> tags) throws Exception {
        long now = Instant.now().toEpochMilli();
        record(context(), eventBuilder(meterName, compartmentId, resourceId, now, now, amount, tags));
    }

    void recordTimed(String meterName,
                     String compartmentId,
                     String resourceId,
                     long fromMillis,
                     long toMillis,
                     Map<String, String> tags) throws Exception {
        record(context(),
               eventBuilder(meterName,
                            compartmentId,
                            resourceId,
                            fromMillis,
                            toMillis,
                            elapsedSeconds(fromMillis, toMillis),
                            tags));
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
            record(context,
                   eventBuilder(effectiveMeterName,
                                region.compartmentId(),
                                region.resourceId(),
                                region.fromMillis(),
                                toMillis,
                                elapsedSeconds(region.fromMillis(), toMillis),
                                effectiveTags));
        } finally {
            context.unregister(region);
        }
    }

    private static MeteringEvent.Builder eventBuilder(String meterName,
                                                      String compartmentId,
                                                      String resourceId,
                                                      long fromMillis,
                                                      long toMillis,
                                                      float value,
                                                      Map<String, String> tags) {
        return MeteringEvent.builder()
                .meterName(meterName)
                .compartmentId(compartmentId)
                .resourceId(resourceId)
                .from(Instant.ofEpochMilli(fromMillis))
                .to(Instant.ofEpochMilli(toMillis))
                .amount(value)
                .tags(tags);
    }

    private static Context context() {
        return Contexts.context().orElse(null);
    }

    private static Transaction transaction(Context context) {
        if (context == null) {
            return null;
        }
        return context.get(Transaction.class)
                .or(() -> kievTransaction(context))
                .orElse(null);
    }

    private static java.util.Optional<Transaction> kievTransaction(Context context) {
        try {
            Class<?> kievTransactions = Class.forName("com.oracle.helidon.oci.kiev.KievTransactions");
            return context.get(kievTransactions, Transaction.class);
        } catch (ClassNotFoundException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static float elapsedSeconds(long fromMillis, long toMillis) {
        return (toMillis - fromMillis) / 1000.0F;
    }

    private void record(Context context, MeteringEvent.Builder eventBuilder) throws Exception {
        Transaction transaction = transaction(context);
        if (transaction != null) {
            eventBuilder.transaction(transaction);
        }
        recorder.record(eventBuilder.build());
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

    record MeteringRegionContext(String meterName,
                                 String compartmentId,
                                 String resourceId,
                                 long fromMillis,
                                 Map<String, String> tags) {
    }
}

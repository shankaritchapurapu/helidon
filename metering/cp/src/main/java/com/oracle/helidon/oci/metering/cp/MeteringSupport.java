/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.Service;

import com.oracle.helidon.oci.metering.common.Tags;
import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.bling.emit.store.MeteringLogStore;
import com.oracle.pic.kiev.Transaction;

/**
 * Runtime support for generated control plane metering interceptors.
 */
@Service.Singleton
public class MeteringSupport {
    private static final System.Logger LOGGER = System.getLogger(MeteringSupport.class.getName());

    private final MeteringLogStores logStores;

    @Service.Inject
    MeteringSupport(MeteringLogStores logStores) {
        this.logStores = logStores;
    }

    void recordPoint(String meterName,
                     String compartmentId,
                     String resourceId,
                     float amount,
                     Map<String, String> tags) throws Exception {
        Instant now = Instant.now();
        record(context(), new MeteringMeasurement(meterName, compartmentId, resourceId, now, now, amount, tags));
    }

    void recordTimed(String meterName,
                     String compartmentId,
                     String resourceId,
                     long fromMillis,
                     long toMillis,
                     Map<String, String> tags) throws Exception {
        record(context(), new MeteringMeasurement(meterName,
                                                  compartmentId,
                                                  resourceId,
                                                  Instant.ofEpochMilli(fromMillis),
                                                  Instant.ofEpochMilli(toMillis),
                                                  elapsedSeconds(fromMillis, toMillis),
                                                  tags));
    }

    void startMeteredSection(Context context,
                             String meterName,
                             String compartmentId,
                             String resourceId,
                             Map<String, String> tags) {
        MeteredSections sections = meteringSections(context);
        sections.start(new MeteredSectionContext(meterName,
                                                 compartmentId,
                                                 resourceId,
                                                 Instant.now().toEpochMilli(),
                                                 tags));
    }

    void endMeteredSection(Context context,
                           String meterName,
                           Map<String, String> tags) throws Exception {
        MeteredSections sections = context.get(MeteredSections.class)
                .orElseThrow(MeteringSupport::noActiveSection);
        MeteredSectionContext sectionContext = sections.end(meterName);
        try {
            long toMillis = Instant.now().toEpochMilli();
            Map<String, String> effectiveTags = Tags.merge(sectionContext.tags(), tags);
            String effectiveMeterName = meterName == null || meterName.isBlank() ? sectionContext.meterName() : meterName;
            record(context, new MeteringMeasurement(effectiveMeterName,
                                                    sectionContext.compartmentId(),
                                                    sectionContext.resourceId(),
                                                    Instant.ofEpochMilli(sectionContext.fromMillis()),
                                                    Instant.ofEpochMilli(toMillis),
                                                    elapsedSeconds(sectionContext.fromMillis(), toMillis),
                                                    effectiveTags));
        } finally {
            unregisterIfEmpty(context, sections);
        }
    }

    void discardSection(Context context, String meterName) {
        context.get(MeteredSections.class)
                .ifPresent(sections -> {
                    sections.discard(meterName);
                    unregisterIfEmpty(context, sections);
                });
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

    private void record(Context context, MeteringMeasurement measurement) throws Exception {
        Transaction transaction = transaction(context);
        if (transaction == null) {
            throw new IllegalStateException("CP metering requires a Kiev transaction");
        }
        MeteringLogStore logStore = logStores.getByMeterName(measurement.meterName());
        if (logStore == null) {
            throw new IllegalStateException("No metering log store configured for meter " + measurement.meterName());
        }
        logStore.addMeter(transaction,
                          measurement.resourceId(),
                          measurement.compartmentId(),
                          measurement.from(),
                          measurement.to(),
                          measurement.amount(),
                          Tags.toJson(measurement.tags()));
    }

    private record MeteringMeasurement(String meterName,
                                       String compartmentId,
                                       String resourceId,
                                       Instant from,
                                       Instant to,
                                       double amount,
                                       Map<String, String> tags) {
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
            try {
                V result = chain.proceed(args);
                record(args);
                return result;
            } catch (Exception e) {
                if (measureOnFailure()) {
                    record(args);
                }
                throw e;
            }
        }

        private void record(Object[] args) {
            recordSafely("point " + meterName(),
                         () -> support().recordPoint(meterName(),
                                                     stringArg(args, compartmentIdParameterIndex(), compartmentId()),
                                                     stringArg(args, resourceIdParameterIndex(), resourceId()),
                                                     amount(args),
                                                     tags(args)));
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
            try {
                V result = chain.proceed(args);
                record(args, fromMillis);
                return result;
            } catch (Exception e) {
                if (measureOnFailure()) {
                    record(args, fromMillis);
                }
                throw e;
            }
        }

        private void record(Object[] args, long fromMillis) {
            recordSafely("timed " + meterName(),
                         () -> support().recordTimed(meterName(),
                                                     stringArg(args, compartmentIdParameterIndex(), compartmentId()),
                                                     stringArg(args, resourceIdParameterIndex(), resourceId()),
                                                     fromMillis,
                                                     Instant.now().toEpochMilli(),
                                                     tags(args)));
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
            Context context = context(args);
            start(context, args);
            try {
                return chain.proceed(args);
            } catch (Exception e) {
                if (!measureOnFailure()) {
                    support().discardSection(context, meterName());
                }
                throw e;
            }
        }

        private void start(Context context, Object[] args) {
            recordSafely("start " + meterName(),
                         () -> support().startMeteredSection(context,
                                                             meterName(),
                                                             stringArg(args, compartmentIdParameterIndex(), compartmentId()),
                                                             stringArg(args, resourceIdParameterIndex(), resourceId()),
                                                             tags(args)));
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
            Context context = context(args);
            try {
                V result = chain.proceed(args);
                end(context, args);
                return result;
            } catch (Exception e) {
                if (measureOnFailure()) {
                    end(context, args);
                } else {
                    support().discardSection(context, meterName());
                }
                throw e;
            }
        }

        private void end(Context context, Object[] args) {
            recordSafely("end " + meterName(), () -> support().endMeteredSection(context, meterName(), tags(args)));
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

        /**
         * Whether to record this measurement when the metered operation fails.
         *
         * @return {@code true} to record failed operations
         */
        protected abstract boolean measureOnFailure();

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

        /**
         * Whether to record this measurement when the metered operation fails.
         *
         * @return {@code true} to record failed operations
         */
        protected abstract boolean measureOnFailure();

        Map<String, String> tags(Object[] args) {
            return Tags.from(staticTags(), tagParameterIndexes(), args);
        }
    }

    private abstract static class InterceptionBase implements Interception.ElementInterceptor {
        void recordSafely(String description, RecordingAction action) {
            try {
                action.record();
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to record metered usage for " + description, e);
            }
        }

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
            throw new IllegalStateException("Metering sections require a current Helidon Context or a Context argument");
        }
    }

    @FunctionalInterface
    private interface RecordingAction {
        void record() throws Exception;
    }

    record MeteredSectionContext(String meterName,
                                 String compartmentId,
                                 String resourceId,
                                 long fromMillis,
                                 Map<String, String> tags) {
    }

    static MeteredSections meteringSections(Context context) {
        return context.get(MeteredSections.class)
                .orElseGet(() -> {
                    MeteredSections sections = new MeteredSections();
                    context.register(sections);
                    return sections;
                });
    }

    static void replaceMeteredSections(Context context, MeteredSectionsSnapshot snapshot) {
        context.get(MeteredSections.class).ifPresent(context::unregister);
        context.register(MeteredSections.create(snapshot));
    }

    static void clearMeteredSections(Context context) {
        context.get(MeteredSections.class).ifPresent(context::unregister);
    }

    private static void unregisterIfEmpty(Context context, MeteredSections sections) {
        if (sections.isEmpty()) {
            context.unregister(sections);
        }
    }

    private static IllegalStateException noActiveSection() {
        return new IllegalStateException("No active metered section in the current context");
    }

    static final class MeteredSections {
        private final Deque<MeteredSectionContext> sections = new ArrayDeque<>();

        static MeteredSections create(MeteredSectionsSnapshot snapshot) {
            MeteredSections sections = new MeteredSections();
            sections.restore(snapshot);
            return sections;
        }

        synchronized void start(MeteredSectionContext section) {
            sections.push(section);
        }

        synchronized MeteredSectionContext end(String meterName) {
            try {
                if (meterName == null || meterName.isBlank()) {
                    return sections.pop();
                }
                return removeMostRecentMatching(meterName);
            } catch (NoSuchElementException e) {
                throw noActiveSection();
            }
        }

        synchronized void discard(String meterName) {
            if (sections.isEmpty()) {
                return;
            }
            if (meterName == null || meterName.isBlank()) {
                sections.pop();
                return;
            }
            Iterator<MeteredSectionContext> iterator = sections.iterator();
            while (iterator.hasNext()) {
                if (meterName.equals(iterator.next().meterName())) {
                    iterator.remove();
                    return;
                }
            }
        }

        synchronized MeteredSectionsSnapshot snapshot() {
            return new MeteredSectionsSnapshot(new ArrayList<>(sections));
        }

        synchronized boolean isEmpty() {
            return sections.isEmpty();
        }

        private void restore(MeteredSectionsSnapshot snapshot) {
            sections.addAll(snapshot.sections());
        }

        private MeteredSectionContext removeMostRecentMatching(String meterName) {
            Iterator<MeteredSectionContext> iterator = sections.iterator();
            while (iterator.hasNext()) {
                MeteredSectionContext section = iterator.next();
                if (meterName.equals(section.meterName())) {
                    iterator.remove();
                    return section;
                }
            }
            throw noActiveSection();
        }
    }

    record MeteredSectionsSnapshot(List<MeteredSectionContext> sections) {
        MeteredSectionsSnapshot {
            sections = List.copyOf(sections);
        }
    }
}

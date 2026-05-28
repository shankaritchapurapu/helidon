/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import java.time.Instant;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.bling.emit.MeteringLogStores;
import com.oracle.pic.bling.emit.store.MeteringLogStore;
import com.oracle.pic.kiev.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeteringInterceptorGenerationTest {
    private ServiceRegistryManager registryManager;

    @BeforeEach
    void setUpRegistry() {
        registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());
    }

    @AfterEach
    void shutdownRegistry() {
        registryManager.shutdown();
    }

    @Test
    void generatedPointInterceptorAddsMeter() throws Exception {
        MeteringLogStores logStores = mock(MeteringLogStores.class);
        MeteringLogStore logStore = mock(MeteringLogStore.class);
        Transaction transaction = mock(Transaction.class);
        Context context = Context.create();
        when(logStores.getByMeterName("requests")).thenReturn(logStore);
        Services.set(MeteringLogStores.class, logStores);

        Interception.ElementInterceptor interceptor = generatedInterceptor("Metering Point interceptor");

        Object result = Contexts.runInContextWithThrow(context, () -> {
            context.register(transaction);
            try {
                return interceptor.proceed(mock(InterceptionContext.class),
                                           args -> "ok",
                                           "ocid1.compartment.oc1..example",
                                           "resource-1",
                                           3.5F,
                                           "create");
            } finally {
                context.unregister(transaction);
            }
        });

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.captor();
        verify(logStores).getByMeterName("requests");
        verify(logStore).addMeter(same(transaction),
                                  anyString(),
                                  anyString(),
                                  fromCaptor.capture(),
                                  toCaptor.capture(),
                                  amountCaptor.capture(),
                                  tagsCaptor.capture());

        assertThat(result, is("ok"));
        assertThat(amountCaptor.getValue(), is(3.5D));
        assertThat(tagsCaptor.getValue(), containsString("\"source\":\"cp-test\""));
        assertThat(tagsCaptor.getValue(), containsString("\"operation\":\"create\""));
    }

    @Test
    void generatedStartAndEndInterceptorsAddMeter() throws Exception {
        MeteringLogStores logStores = mock(MeteringLogStores.class);
        MeteringLogStore logStore = mock(MeteringLogStore.class);
        Transaction transaction = mock(Transaction.class);
        Context context = Context.create();
        when(logStores.getByMeterName("duration")).thenReturn(logStore);
        Services.set(MeteringLogStores.class, logStores);
        Interception.ElementInterceptor start = generatedInterceptor("Metering Start interceptor");
        Interception.ElementInterceptor end = generatedInterceptor("Metering End interceptor");

        Object startResult = Contexts.runInContextWithThrow(context, () -> {
            context.register(transaction);
            try {
                return start.proceed(mock(InterceptionContext.class),
                                     args -> "started",
                                     "ocid1.compartment.oc1..example",
                                     "resource-1",
                                     "create");
            } finally {
                context.unregister(transaction);
            }
        });

        Object endResult = Contexts.runInContextWithThrow(context, () -> {
            context.register(transaction);
            try {
                return end.proceed(mock(InterceptionContext.class),
                                   args -> "ended",
                                   "ok");
            } finally {
                context.unregister(transaction);
            }
        });

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.captor();
        verify(logStores).getByMeterName("duration");
        verify(logStore).addMeter(same(transaction),
                                  anyString(),
                                  anyString(),
                                  fromCaptor.capture(),
                                  toCaptor.capture(),
                                  amountCaptor.capture(),
                                  tagsCaptor.capture());

        assertThat(startResult, is("started"));
        assertThat(endResult, is("ended"));
        assertThat(amountCaptor.getValue() >= 0.0D, is(true));
        assertThat(tagsCaptor.getValue(), containsString("\"phase\":\"create\""));
        assertThat(tagsCaptor.getValue(), containsString("\"end\":\"true\""));
        assertThat(tagsCaptor.getValue(), containsString("\"result\":\"ok\""));
    }

    private static Interception.ElementInterceptor generatedInterceptor(String text) {
        return Services.all(Interception.ElementInterceptor.class)
                .stream()
                .filter(interceptor -> interceptor.toString().contains(text))
                .filter(interceptor -> interceptor.toString().contains(SampleService.class.getCanonicalName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected generated interceptor containing: " + text));
    }

    static class SampleService {
        @Metering.Point(value = "requests", tags = @Metering.Tag(key = "source", value = "cp-test"))
        void create(@Metering.CompartmentId String compartmentId,
                    @Metering.ResourceId String resourceId,
                    @Metering.Amount float amount,
                    @Metering.TagValue("operation") String operation) {
        }

        @Metering.Start(value = "duration")
        void begin(@Metering.CompartmentId String compartmentId,
                   @Metering.ResourceId String resourceId,
                   @Metering.TagValue("phase") String phase) {
        }

        @Metering.End(tags = @Metering.Tag(key = "end", value = "true"))
        void finish(@Metering.TagValue("result") String result) {
        }
    }
}

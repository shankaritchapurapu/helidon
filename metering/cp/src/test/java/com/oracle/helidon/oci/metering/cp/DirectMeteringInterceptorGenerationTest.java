/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metering.cp;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Interception;
import io.helidon.service.registry.InterceptionContext;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.pic.bling.clients.ingest.model.Meters;
import com.oracle.pic.bling.emit.client.MeteringClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DirectMeteringInterceptorGenerationTest {
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
    void generatedPointInterceptorSendsWithNativeClient() throws Exception {
        MeteringClient client = mock(MeteringClient.class);
        Services.set(MeteringRecorder.class, new DirectMeteringRecorder("direct", client));

        Interception.ElementInterceptor interceptor = generatedInterceptor("Metering Point interceptor");

        Object result = interceptor.proceed(mock(InterceptionContext.class),
                                            args -> "ok",
                                            "ocid1.compartment.oc1..example",
                                            "resource-1",
                                            3.5F,
                                            "create");

        ArgumentCaptor<Meters> metersCaptor = ArgumentCaptor.captor();
        verify(client).send(metersCaptor.capture());
        Meters meters = metersCaptor.getValue();

        assertThat(result, is("ok"));
        assertThat(meters.getMetrics(), hasSize(1));
        assertThat(meters.getMetrics().getFirst().getMtr(), is("requests"));
        assertThat(meters.getMetrics().getFirst().getCompartmentId(), is("ocid1.compartment.oc1..example"));
        assertThat(meters.getMetrics().getFirst().getResourceId(), is("resource-1"));
        assertThat(meters.getMetrics().getFirst().getValue(), is(3.5F));
        assertThat(meters.getMetrics().getFirst().getTags(), containsString("\"source\":\"test\""));
        assertThat(meters.getMetrics().getFirst().getTags(), containsString("\"operation\":\"create\""));
    }

    @Test
    void generatedStartAndEndInterceptorsUseContextRegion() throws Exception {
        MeteringClient client = mock(MeteringClient.class);
        Services.set(MeteringRecorder.class, new DirectMeteringRecorder("direct", client));
        Interception.ElementInterceptor start = generatedInterceptor("Metering Start interceptor");
        Interception.ElementInterceptor end = generatedInterceptor("Metering End interceptor");
        Context context = Context.create();

        Object startResult = Contexts.runInContextWithThrow(context,
                                                            () -> start.proceed(mock(InterceptionContext.class),
                                                                                args -> "started",
                                                                                "ocid1.compartment.oc1..example",
                                                                                "resource-1",
                                                                                "create"));
        Object endResult = Contexts.runInContextWithThrow(context,
                                                          () -> end.proceed(mock(InterceptionContext.class),
                                                                            args -> "ended",
                                                                            "ok"));

        ArgumentCaptor<Meters> metersCaptor = ArgumentCaptor.captor();
        verify(client).send(metersCaptor.capture());
        Meters meters = metersCaptor.getValue();

        assertThat(startResult, is("started"));
        assertThat(endResult, is("ended"));
        assertThat(meters.getMetrics(), hasSize(1));
        assertThat(meters.getMetrics().getFirst().getMtr(), is("duration"));
        assertThat(meters.getMetrics().getFirst().getCompartmentId(), is("ocid1.compartment.oc1..example"));
        assertThat(meters.getMetrics().getFirst().getResourceId(), is("resource-1"));
        assertThat(meters.getMetrics().getFirst().getTags(), containsString("\"phase\":\"create\""));
        assertThat(meters.getMetrics().getFirst().getTags(), containsString("\"end\":\"true\""));
        assertThat(meters.getMetrics().getFirst().getTags(), containsString("\"result\":\"ok\""));
    }

    @Test
    void generatedStartInterceptorRejectsNestedContextRegion() throws Exception {
        Services.set(MeteringRecorder.class, new DirectMeteringRecorder("direct", mock(MeteringClient.class)));
        Interception.ElementInterceptor start = generatedInterceptor("Metering Start interceptor");
        Context context = Context.create();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                       () -> Contexts.runInContextWithThrow(context, () -> {
                                                           start.proceed(mock(InterceptionContext.class),
                                                                         args -> "started",
                                                                         "ocid1.compartment.oc1..example",
                                                                         "resource-1",
                                                                         "create");
                                                           return start.proceed(mock(InterceptionContext.class),
                                                                                args -> "nested",
                                                                                "ocid1.compartment.oc1..example",
                                                                                "resource-1",
                                                                                "update");
                                                       }));

        assertThat(exception.getMessage(), containsString("already active"));
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
        @Metering.Point(value = "requests", tags = @Metering.Tag(key = "source", value = "test"))
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

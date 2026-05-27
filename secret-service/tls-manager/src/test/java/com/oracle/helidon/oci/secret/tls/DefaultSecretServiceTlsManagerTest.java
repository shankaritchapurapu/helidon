/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import io.helidon.Main;
import io.helidon.common.Errors;
import io.helidon.common.configurable.Resource;
import io.helidon.common.configurable.ResourceConfig;
import io.helidon.common.tls.TlsConfig;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.metrics.api.Gauge;
import io.helidon.metrics.api.MeterRegistry;
import io.helidon.metrics.api.MetricsConfig;
import io.helidon.metrics.api.MetricsFactory;
import io.helidon.metrics.api.Tag;
import io.helidon.metrics.providers.micrometer.MicrometerMetricsFactoryProvider;
import io.helidon.scheduling.Cron;
import io.helidon.scheduling.CronConfig;
import io.helidon.scheduling.Task;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;
import io.helidon.spi.HelidonShutdownHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSecretServiceTlsManagerTest {
    @BeforeAll
    static void initMetrics() {
        ServiceRegistryManager registryManager = ServiceRegistryManager.create();
        GlobalServiceRegistry.registry(registryManager.registry());
        Services.set(Config.class, Config.empty());
        MetricsConfig metricsConfig = MetricsConfig.builder()
                .config(Config.empty())
                .buildPrototype();
        MetricsFactory metricsFactory = new MicrometerMetricsFactoryProvider()
                .create(Config.empty(), metricsConfig, List.of());
        metricsFactory.globalRegistry(metricsConfig);
        Services.set(MetricsFactory.class, metricsFactory);
    }

    @Test
    void pkiPasswordHasNoDefault() {
        PkiConfig pki = PkiConfig.builder()
                .secretPath("/secret/helidon/test/latest")
                .buildPrototype();

        assertThat(pki.password(), is(Optional.empty()));
    }

    @Test
    void pkiPasswordBindsAsCharArray() {
        PkiConfig pki = PkiConfig.create(config(Map.of(
                "password", "secret",
                "resource.path", "src/test/resources/mtls/client-pki.json"
        )));

        assertThat(new String(pki.password().orElseThrow()), is("secret"));
    }

    @Test
    void pkiSecretPathBindsFromConfig() {
        PkiConfig pki = PkiConfig.create(config(Map.of(
                "secret-path", "/secret/helidon/test/latest"
        )));

        assertThat(pki.secretPath().orElseThrow(), is("/secret/helidon/test/latest"));
    }

    @Test
    void readsTrustResourceRepeatedly() throws Exception {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(pkiResource("client-pki.json"))
                .trust(ResourceConfig.builder().resourcePath("mtls/ca.pem").build())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);
        byte[] priorBytes = null;

        for (int i = 0; i < 5; i++) {
            byte[] currentBytes;
            try (var stream = manager.getTrustStream(config.trust().orElseThrow())) {
                currentBytes = stream.readAllBytes();
            }
            if (priorBytes != null) {
                assertThat(currentBytes, equalTo(priorBytes));
            }
            priorBytes = currentBytes;
        }
    }

    @Test
    void reloadsWhenTrustResourceChangesWithoutPkiChange(@TempDir Path tempDir) throws Exception {
        Path trustPath = tempDir.resolve("ca.pem");
        String ca = Files.readString(Path.of("src/test/resources/mtls/ca.pem"));
        Files.writeString(trustPath, ca);
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(pkiResource("client-pki.json"))
                .trust(Resource.create(trustPath))
                .buildPrototype();
        RecordingSecretServiceTlsManager manager = new RecordingSecretServiceTlsManager(config);

        manager.init(TlsConfig.builder().buildPrototype());
        assertThat(manager.loadContext(false), is(false));

        Files.writeString(trustPath, ca + System.lineSeparator() + ca);

        assertThat(manager.loadContext(false), is(true));
        assertThat(manager.reloadCount(), is(1));
    }

    @Test
    void initializesFromLocalMaterialAndSkipsUnchangedReload() {
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(localConfig("client-pki.json"));

        manager.init(TlsConfig.builder().buildPrototype());

        assertThat(manager.sslContext(), notNullValue());
        assertThat(manager.keyManager().isPresent(), is(true));
        assertThat(manager.trustManager().isPresent(), is(true));
        assertThat(manager.loadContext(false), is(false));
    }

    @Test
    void initializesFromInlineResourceMaterial() throws Exception {
        String material = Files.readString(Path.of("src/test/resources/mtls/client-pki.json"));
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .resource(ResourceConfig.builder().contentPlain(material).build())
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);

        manager.init(TlsConfig.builder().buildPrototype());

        assertThat(manager.sslContext(), notNullValue());
        assertThat(manager.keyManager().isPresent(), is(true));
        assertThat(manager.trustManager().isPresent(), is(true));
    }

    @Test
    void reloadsChangedSsv2Material() throws Exception {
        AtomicReference<byte[]> material = new AtomicReference<>(pkiMaterial("server-pki.json"));
        Function<String, Optional<byte[]>> resolver = path -> {
            assertThat(path, is("/secret/helidon/test/latest"));
            return Optional.of(material.get());
        };
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config, resolver);

        manager.init(TlsConfig.builder().buildPrototype());
        Object initialSslContext = manager.sslContext();
        material.set(pkiMaterial("client-pki.json"));

        assertThat(manager.loadContext(false), is(true));
        assertThat(manager.sslContext(), sameInstance(initialSslContext));
    }

    @Test
    void closePreventsCronReload() throws Exception {
        AtomicReference<byte[]> material = new AtomicReference<>(pkiMaterial("server-pki.json"));
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(true))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        RecordingSecretServiceTlsManager manager = new RecordingSecretServiceTlsManager(config,
                                                                                       path -> Optional.of(material.get()));

        manager.init(TlsConfig.builder().buildPrototype());
        manager.close();
        material.set(pkiMaterial("client-pki.json"));
        manager.maybeReload();

        assertThat(manager.reloadCount(), is(0));
        assertThat(manager.reloadedSubject(), nullValue());
    }

    @Test
    void recordsReloadMetricsForCronReloads(@TempDir Path tempDir) throws Exception {
        Path pkiPath = tempDir.resolve("pki.json");
        Files.write(pkiPath, pkiMaterial("server-pki.json"));
        String managerName = "metrics-" + System.nanoTime();
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(true))
                .pki(PkiConfig.builder()
                             .resource(Resource.create(pkiPath))
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        RecordingSecretServiceTlsManager manager =
                new RecordingSecretServiceTlsManager(config, managerName);

        try {
            manager.init(TlsConfig.builder().buildPrototype());
            manager.maybeReload();

            assertThat(reloadMetricCount(managerName, SecretServiceTlsManagerMetrics.RESULT_SKIPPED), is(1L));
            assertThat(reloadDurationCount(managerName), is(1L));
            assertThat(consecutiveReloadFailures(managerName), is(0));

            Files.writeString(pkiPath, "{");
            manager.maybeReload();

            assertThat(reloadMetricCount(managerName, SecretServiceTlsManagerMetrics.RESULT_FAILURE), is(1L));
            assertThat(reloadDurationCount(managerName), is(2L));
            assertThat(consecutiveReloadFailures(managerName), is(1));
            assertThat(manager.reloadCount(), is(0));

            Files.write(pkiPath, pkiMaterial("client-pki.json"));
            manager.maybeReload();

            assertThat(reloadMetricCount(managerName, SecretServiceTlsManagerMetrics.RESULT_SUCCESS), is(1L));
            assertThat(reloadDurationCount(managerName), is(3L));
            assertThat(consecutiveReloadFailures(managerName), is(0));
            assertThat(manager.reloadCount(), is(1));
            assertThat(manager.reloadedSubject(), is("CN=Helidon-Test-Client"));
        } finally {
            manager.close();
        }
    }

    @Test
    void reloadDisabledDoesNotScheduleCron() throws Exception {
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(localConfig("client-pki.json"));

        manager.init(TlsConfig.builder().buildPrototype());

        assertThat(reloadTask(manager), nullValue());
    }

    @Test
    void initIsIdempotentAndSchedulesSingleCron() throws Exception {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(true))
                .pki(pkiResource("client-pki.json"))
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);

        try {
            manager.init(TlsConfig.builder().buildPrototype());
            Task first = reloadTask(manager);
            manager.init(TlsConfig.builder().buildPrototype());

            assertThat(reloadTask(manager), sameInstance(first));
        } finally {
            manager.close();
        }
    }

    @Test
    void scheduledReloadDoesNotAllowConcurrentCronExecutions() throws Exception {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(true))
                .pki(pkiResource("client-pki.json"))
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);

        try {
            manager.init(TlsConfig.builder().buildPrototype());

            assertThat(reloadCron(manager).prototype().concurrentExecution(), is(false));
        } finally {
            manager.close();
        }
    }

    @Test
    void configuresReloadCronConfigFromConfig() {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.create(config(Map.of(
                "reload.enabled", "false",
                "reload.expression", "0/5 * * * * ? *",
                "reload.concurrent", "true",
                "pki.resource.path", "src/test/resources/mtls/client-pki.json",
                "trust.path", "src/test/resources/mtls/ca.pem"
        )));

        assertThat(config.reload().enabled(), is(false));
        assertThat(config.reload().expression(), is("0/5 * * * * ? *"));
        assertThat(config.reload().concurrentExecution(), is(false));
    }

    @Test
    void closeStopsScheduledReloadExecutor() throws Exception {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(true))
                .pki(pkiResource("client-pki.json"))
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);

        manager.init(TlsConfig.builder().buildPrototype());
        Task task = reloadTask(manager);

        assertThat(shutdownHandlers().contains(manager), is(true));

        manager.close();
        manager.close();

        assertThat(task.executor().isShutdown(), is(true));
        assertThat(shutdownHandlers().contains(manager), is(false));
    }

    @Test
    void shutdownStopsScheduledReloadExecutor() throws Exception {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(true))
                .pki(pkiResource("client-pki.json"))
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);

        manager.init(TlsConfig.builder().buildPrototype());
        Task task = reloadTask(manager);

        manager.shutdown();

        assertThat(task.executor().isShutdown(), is(true));
    }

    @Test
    void reloadReadsDirectSecretServiceMaterial() throws Exception {
        AtomicReference<byte[]> material = new AtomicReference<>(pkiMaterial("server-pki.json"));
        AtomicInteger secretReads = new AtomicInteger();
        Function<String, Optional<byte[]>> resolver = path -> {
            secretReads.incrementAndGet();
            assertThat(path, is("/secret/helidon/test/latest"));
            return Optional.of(material.get());
        };
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        RecordingSecretServiceTlsManager manager = new RecordingSecretServiceTlsManager(config, resolver);

        manager.init(TlsConfig.builder().buildPrototype());
        assertThat(leafSubject(manager), is("CN=Helidon-Test-Server"));

        material.set(pkiMaterial("client-pki.json"));

        assertThat(manager.loadContext(false), is(true));
        assertThat(manager.reloadedSubject(), is("CN=Helidon-Test-Client"));
        assertThat(secretReads.get(), is(2));
    }

    @Test
    void directSecretReadFailureDoesNotFallbackToStaleMaterial() throws Exception {
        byte[] initialMaterial = pkiMaterial("server-pki.json");
        AtomicInteger secretReads = new AtomicInteger();
        Function<String, Optional<byte[]>> resolver = path -> {
            assertThat(path, is("/secret/helidon/test/latest"));
            if (secretReads.incrementAndGet() == 1) {
                return Optional.of(initialMaterial);
            }
            throw new IllegalStateException("SSv2 read failed");
        };
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        RecordingSecretServiceTlsManager manager = new RecordingSecretServiceTlsManager(config, resolver);

        manager.init(TlsConfig.builder().buildPrototype());

        assertThrows(IllegalStateException.class, () -> manager.loadContext(false));
        assertThat(manager.reloadCount(), is(0));
        assertThat(leafSubject(manager), is("CN=Helidon-Test-Server"));
        assertThat(secretReads.get(), is(2));
    }

    @Test
    void serializesConcurrentReloadAttempts() throws Exception {
        AtomicReference<byte[]> material = new AtomicReference<>(pkiMaterial("server-pki.json"));
        Function<String, Optional<byte[]>> resolver = path -> {
            assertThat(path, is("/secret/helidon/test/latest"));
            return Optional.of(material.get());
        };
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        BlockingReloadSecretServiceTlsManager manager =
                new BlockingReloadSecretServiceTlsManager(config, resolver);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        manager.init(TlsConfig.builder().buildPrototype());
        material.set(pkiMaterial("client-pki.json"));

        try {
            Future<Boolean> first = executor.submit(() -> manager.loadContext(false));
            assertThat(manager.firstReloadEntered().await(5, TimeUnit.SECONDS), is(true));

            Future<Boolean> second = executor.submit(() -> manager.loadContext(false));
            assertThat(manager.concurrentReloadEntered().await(200, TimeUnit.MILLISECONDS), is(false));

            manager.releaseReloads();
            assertThat(first.get(5, TimeUnit.SECONDS), is(true));
            assertThat(second.get(5, TimeUnit.SECONDS), is(false));
            assertThat(manager.maxConcurrentReloads(), is(1));
        } finally {
            manager.releaseReloads();
            executor.shutdownNow();
        }
    }

    @Test
    void closePreventsInProgressMaterialReadFromInstallingReload() throws Exception {
        byte[] initialMaterial = pkiMaterial("server-pki.json");
        byte[] updatedMaterial = pkiMaterial("client-pki.json");
        AtomicInteger secretReads = new AtomicInteger();
        CountDownLatch reloadReadEntered = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);
        Function<String, Optional<byte[]>> resolver = path -> {
            assertThat(path, is("/secret/helidon/test/latest"));
            if (secretReads.incrementAndGet() == 1) {
                return Optional.of(initialMaterial);
            }
            reloadReadEntered.countDown();
            try {
                if (!releaseRead.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release SSv2 read");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting to release SSv2 read", e);
            }
            return Optional.of(updatedMaterial);
        };
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        RecordingSecretServiceTlsManager manager = new RecordingSecretServiceTlsManager(config, resolver);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        manager.init(TlsConfig.builder().buildPrototype());

        try {
            Future<Boolean> reload = executor.submit(() -> manager.loadContext(false));
            assertThat(reloadReadEntered.await(5, TimeUnit.SECONDS), is(true));

            Future<?> close = executor.submit(manager::close);
            releaseRead.countDown();

            assertThat(reload.get(5, TimeUnit.SECONDS), is(false));
            close.get(5, TimeUnit.SECONDS);
            assertThat(manager.reloadCount(), is(0));
            assertThat(leafSubject(manager), is("CN=Helidon-Test-Server"));
        } finally {
            releaseRead.countDown();
            manager.close();
            executor.shutdownNow();
        }
    }

    @Test
    void closeDuringInitialMaterialReadFailsInit() throws Exception {
        byte[] initialMaterial = pkiMaterial("server-pki.json");
        CountDownLatch initReadEntered = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);
        Function<String, Optional<byte[]>> resolver = path -> {
            assertThat(path, is("/secret/helidon/test/latest"));
            initReadEntered.countDown();
            try {
                if (!releaseRead.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release SSv2 read");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting to release SSv2 read", e);
            }
            return Optional.of(initialMaterial);
        };
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .buildPrototype())
                .trust(trustResource())
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config, resolver);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> init = executor.submit(() -> manager.init(TlsConfig.builder().buildPrototype()));
            assertThat(initReadEntered.await(5, TimeUnit.SECONDS), is(true));

            Future<?> close = executor.submit(manager::close);
            releaseRead.countDown();

            ExecutionException thrown = assertThrows(ExecutionException.class, () -> init.get(5, TimeUnit.SECONDS));
            assertThat(thrown.getCause(), instanceOf(IllegalStateException.class));
            close.get(5, TimeUnit.SECONDS);
        } finally {
            releaseRead.countDown();
            manager.close();
            executor.shutdownNow();
        }
    }

    @Test
    void failsFastWhenTrustIsMissing() {
        SecretServiceTlsManagerConfig config = SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(pkiResource("client-pki.json"))
                .buildPrototype();
        DefaultSecretServiceTlsManager manager = new DefaultSecretServiceTlsManager(config);

        assertThrows(IllegalStateException.class, () -> manager.init(TlsConfig.builder().buildPrototype()));
    }

    @Test
    void failsFastWhenPkiConfigIsMissing() {
        Errors.ErrorMessagesException thrown = assertThrows(Errors.ErrorMessagesException.class,
                                                            () -> SecretServiceTlsManagerConfig.builder()
                                                                    .reload(reload(false))
                                                                    .trust(trustResource())
                                                                    .buildPrototype());

        assertThat(thrown.getMessage().contains("Property \"pki\" must not be null, but not set"), is(true));
    }

    @Test
    void rejectsBothPkiSecretAndResource() {
        assertThrows(IllegalArgumentException.class,
                     () -> PkiConfig.builder()
                             .secretPath("/secret/helidon/test/latest")
                             .resource(Resource.create(Path.of("src/test/resources/mtls/client-pki.json")))
                             .buildPrototype());
    }

    @Test
    void rejectsMissingPkiSecretAndResource() {
        assertThrows(IllegalArgumentException.class,
                     () -> PkiConfig.builder().buildPrototype());
    }

    @Test
    void rejectsConfiguredPkiWithoutSecretOrResource() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                                                       () -> SecretServiceTlsManagerConfig.create(config(Map.of(
                                                               "pki.password", "secret"
                                                       ))));

        assertThat(thrown.getMessage(), is("Either SSv2 secret or PKI JSON resource must be configured."));
    }

    private static SecretServiceTlsManagerConfig localConfig(String pkiFile) {
        return SecretServiceTlsManagerConfig.builder()
                .reload(reload(false))
                .pki(pkiResource(pkiFile))
                .trust(trustResource())
                .buildPrototype();
    }

    private static String leafSubject(DefaultSecretServiceTlsManager manager) {
        return leafSubject(manager.keyManager().orElseThrow());
    }

    private static String leafSubject(X509KeyManager keyManager) {
        String alias = keyManager.chooseServerAlias("RSA", null, null);
        X509Certificate[] chain = keyManager.getCertificateChain(alias);
        return chain[0].getSubjectX500Principal().getName();
    }

    private static Cron reloadCron(DefaultSecretServiceTlsManager manager) throws Exception {
        return (Cron) reloadTask(manager);
    }

    private static Task reloadTask(DefaultSecretServiceTlsManager manager) throws Exception {
        Field field = DefaultSecretServiceTlsManager.class.getDeclaredField("reloadTask");
        field.setAccessible(true);
        return (Task) field.get(manager);
    }

    @SuppressWarnings("unchecked")
    private static Set<HelidonShutdownHandler> shutdownHandlers() throws Exception {
        Field field = Main.class.getDeclaredField("SHUTDOWN_HANDLERS");
        field.setAccessible(true);
        return (Set<HelidonShutdownHandler>) field.get(null);
    }

    private static long reloadMetricCount(String managerName, String result) {
        return metricsRegistry()
                .counter(SecretServiceTlsManagerMetrics.RELOADS, metricTags(managerName, result))
                .orElseThrow()
                .count();
    }

    private static long reloadDurationCount(String managerName) {
        return metricsRegistry()
                .timer(SecretServiceTlsManagerMetrics.RELOAD_DURATION, managerTag(managerName))
                .orElseThrow()
                .count();
    }

    private static int consecutiveReloadFailures(String managerName) {
        Gauge<?> gauge = metricsRegistry()
                .gauge(SecretServiceTlsManagerMetrics.CONSECUTIVE_RELOAD_FAILURES, managerTag(managerName))
                .orElseThrow();
        return gauge.value().intValue();
    }

    private static MeterRegistry metricsRegistry() {
        return Services.get(MetricsFactory.class).globalRegistry();
    }

    private static List<Tag> metricTags(String managerName, String result) {
        return List.of(metricTag("manager", managerName), metricTag("result", result));
    }

    private static List<Tag> managerTag(String managerName) {
        return List.of(metricTag("manager", managerName));
    }

    private static Tag metricTag(String key, String value) {
        return Services.get(MetricsFactory.class).tagCreate(key, value);
    }

    private static PkiConfig pkiResource(String fileName) {
        return PkiConfig.builder()
                .resource(Resource.create(Path.of("src/test/resources/mtls/" + fileName)))
                .buildPrototype();
    }

    private static Resource trustResource() {
        return Resource.create(Path.of("src/test/resources/mtls/ca.pem"));
    }

    private static CronConfig reload(boolean enabled) {
        return CronConfig.builder(SecretServiceTlsManagerConfigSupport.defaultReload())
                .enabled(enabled)
                .buildPrototype();
    }

    private static byte[] pkiMaterial(String pkiFile) throws Exception {
        return Files.readAllBytes(Path.of("src/test/resources/mtls/" + pkiFile));
    }

    private static Config config(Map<String, String> values) {
        return Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .addSource(ConfigSources.create(values))
                .build();
    }

    private static class RecordingSecretServiceTlsManager extends DefaultSecretServiceTlsManager {
        private final AtomicReference<String> reloadedSubject = new AtomicReference<>();
        private final AtomicInteger reloadCount = new AtomicInteger();

        private RecordingSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg) {
            super(cfg);
        }

        private RecordingSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg,
                                                 String name) {
            super(cfg, name);
        }

        private RecordingSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg,
                                                 Function<String, Optional<byte[]>> secretResolver) {
            super(cfg, secretResolver);
        }

        @Override
        protected void reload(Optional<X509KeyManager> keyManager, Optional<X509TrustManager> trustManager) {
            reloadCount.incrementAndGet();
            keyManager.map(DefaultSecretServiceTlsManagerTest::leafSubject)
                    .ifPresent(reloadedSubject::set);
            super.reload(keyManager, trustManager);
        }

        private String reloadedSubject() {
            return reloadedSubject.get();
        }

        private int reloadCount() {
            return reloadCount.get();
        }
    }

    private static final class BlockingReloadSecretServiceTlsManager extends RecordingSecretServiceTlsManager {
        private final CountDownLatch firstReloadEntered = new CountDownLatch(1);
        private final CountDownLatch concurrentReloadEntered = new CountDownLatch(1);
        private final CountDownLatch releaseReloads = new CountDownLatch(1);
        private final AtomicInteger activeReloads = new AtomicInteger();
        private final AtomicInteger maxConcurrentReloads = new AtomicInteger();

        private BlockingReloadSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg,
                                                      Function<String, Optional<byte[]>> secretResolver) {
            super(cfg, secretResolver);
        }

        @Override
        protected void reload(Optional<X509KeyManager> keyManager, Optional<X509TrustManager> trustManager) {
            int active = activeReloads.incrementAndGet();
            maxConcurrentReloads.accumulateAndGet(active, Math::max);
            if (active == 1) {
                firstReloadEntered.countDown();
                try {
                    if (!releaseReloads.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release TLS reload");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted waiting to release TLS reload", e);
                }
            } else {
                concurrentReloadEntered.countDown();
            }
            try {
                super.reload(keyManager, trustManager);
            } finally {
                activeReloads.decrementAndGet();
            }
        }

        private CountDownLatch firstReloadEntered() {
            return firstReloadEntered;
        }

        private CountDownLatch concurrentReloadEntered() {
            return concurrentReloadEntered;
        }

        private void releaseReloads() {
            releaseReloads.countDown();
        }

        private int maxConcurrentReloads() {
            return maxConcurrentReloads.get();
        }
    }
}

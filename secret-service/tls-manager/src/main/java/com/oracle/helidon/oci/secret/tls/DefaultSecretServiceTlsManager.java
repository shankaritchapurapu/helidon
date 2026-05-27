/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import io.helidon.Main;
import io.helidon.common.LazyValue;
import io.helidon.common.configurable.Resource;
import io.helidon.common.configurable.ResourceConfig;
import io.helidon.common.pki.PemReader;
import io.helidon.common.tls.ConfiguredTlsManager;
import io.helidon.common.tls.TlsConfig;
import io.helidon.metrics.api.Timer;
import io.helidon.scheduling.CronConfig;
import io.helidon.scheduling.Task;
import io.helidon.service.registry.Services;
import io.helidon.spi.HelidonShutdownHandler;

import com.oracle.helidon.oci.secret.config.Ssv2Client;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

/**
 * Default implementation of {@link SecretServiceTlsManager}.
 */
class DefaultSecretServiceTlsManager extends ConfiguredTlsManager
        implements SecretServiceTlsManager, AutoCloseable, HelidonShutdownHandler {
    static final String TYPE = "oci-ssv2";

    private static final System.Logger LOGGER = System.getLogger(DefaultSecretServiceTlsManager.class.getName());
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final SecretServiceTlsManagerConfig cfg;
    private final AtomicReference<byte[]> lastMaterialDigest = new AtomicReference<>(new byte[0]);
    private final LazyValue<ScheduledExecutorService> asyncExec = LazyValue.create(DefaultSecretServiceTlsManager::scheduler);
    private final LazyValue<Ssv2Client> secretClient;
    private final Function<String, Optional<byte[]>> secretResolver;
    private final SecretServiceTlsManagerMetrics metrics;
    private final Lock reloadLock = new ReentrantLock();
    private final AtomicReference<LifecycleState> lifecycleState = new AtomicReference<>(LifecycleState.NEW);

    private volatile Task reloadTask;
    private TlsConfig tlsConfig;

    DefaultSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg) {
        this(cfg, "@default");
    }

    DefaultSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg, String name) {
        this(cfg, name, () -> Services.get(Ssv2Client.class), null);
    }

    DefaultSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg,
                                   Function<String, Optional<byte[]>> secretResolver) {
        this(cfg, "@default", () -> Services.get(Ssv2Client.class), secretResolver);
    }

    private DefaultSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg,
                                           String name,
                                           Supplier<Ssv2Client> secretClientSupplier,
                                           Function<String, Optional<byte[]>> secretResolver) {
        super(name, TYPE);
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        this.secretClient = LazyValue.create(Objects.requireNonNull(secretClientSupplier, "secretClientSupplier"));
        this.secretResolver = secretResolver == null
                ? path -> secretClient.get().getSecretAsBytes(path)
                : Objects.requireNonNull(secretResolver, "secretResolver");
        this.metrics = SecretServiceTlsManagerMetrics.create(name);
    }

    @Override
    public void init(TlsConfig tls) {
        reloadLock.lock();
        try {
            initLocked(tls);
        } finally {
            reloadLock.unlock();
        }
    }

    @Override
    public SecretServiceTlsManagerConfig prototype() {
        return cfg;
    }

    boolean loadContext(boolean initialLoad) {
        reloadLock.lock();
        try {
            if (!initialLoad && !reloadActive()) {
                return false;
            }
            return loadContextLocked(initialLoad, pkiMaterial());
        } finally {
            reloadLock.unlock();
        }
    }

    private void initLocked(TlsConfig tls) {
        LifecycleState state = lifecycleState.get();
        if (state == LifecycleState.INITIALIZED) {
            return;
        }
        if (!lifecycleState.compareAndSet(LifecycleState.NEW, LifecycleState.INITIALIZING)) {
            throw new IllegalStateException("Cannot initialize a closed Secret Service TLS manager");
        }
        try {
            this.tlsConfig = Objects.requireNonNull(tls, "tls");
            if (!loadContextLocked(true, pkiMaterial())) {
                throw closedDuringInit();
            }
            if (cfg.reload().enabled()) {
                scheduleReload();
            }
            if (!lifecycleState.compareAndSet(LifecycleState.INITIALIZING, LifecycleState.INITIALIZED)) {
                throw closedDuringInit();
            }
        } catch (RuntimeException e) {
            lifecycleState.compareAndSet(LifecycleState.INITIALIZING, LifecycleState.NEW);
            throw e;
        }
    }

    private static IllegalStateException closedDuringInit() {
        return new IllegalStateException("Secret Service TLS manager was closed during initialization");
    }

    private boolean loadContextLocked(boolean initialLoad, byte[] certJsonBlob) {
        if (!initialLoad && !reloadActive()) {
            return false;
        }

        PkiConfig pki = cfg.pki();
        byte[] trustMaterial = trustMaterial();
        byte[] digest = digest(certJsonBlob, trustMaterial, tlsConfig);
        if (!initialLoad && MessageDigest.isEqual(lastMaterialDigest.get(), digest)) {
            if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "Identical mTLS material, skipping TLS context reload");
            }
            return false;
        }

        PkiCertificate certificate = PkiCertificate.newInstance(certJsonBlob, pki.password().orElse(null));
        KeyManagerFactory kmf = keyManagerFactory(certificate);
        TrustManagerFactory tmf = trustManagerFactory(trustMaterial);

        Optional<X509KeyManager> keyManager = Arrays.stream(kmf.getKeyManagers())
                .filter(X509KeyManager.class::isInstance)
                .map(X509KeyManager.class::cast)
                .findFirst();
        if (keyManager.isEmpty()) {
            throw new IllegalStateException("Unable to find X.509 key manager in PKI material: " + pki);
        }

        Optional<X509TrustManager> trustManager = Arrays.stream(tmf.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst();
        if (trustManager.isEmpty()) {
            throw new IllegalStateException("Unable to find X.509 trust manager in configured trust material");
        }

        if (!reloadCanInstall(initialLoad)) {
            return false;
        }
        if (initialLoad) {
            initSslContext(tlsConfig, secureRandom(tlsConfig), kmf.getKeyManagers(), tmf.getTrustManagers());
        } else {
            reload(keyManager, trustManager);
        }
        lastMaterialDigest.set(digest);
        return true;
    }

    InputStream getTrustStream(Resource trust) {
        return freshResource(trust).stream();
    }

    @Override
    public void close() {
        requestClose();
        CloseResources resources = closeResources();
        closeTask(resources.reloadTask());
        shutdownReloadExecutor();
        if (resources.removeShutdownHandler()) {
            removeShutdownHandler();
        }
    }

    private CloseResources closeResources() {
        reloadLock.lock();
        try {
            return closeLocked();
        } finally {
            reloadLock.unlock();
        }
    }

    private void requestClose() {
        lifecycleState.updateAndGet(state -> state == LifecycleState.CLOSED
                ? LifecycleState.CLOSED
                : LifecycleState.CLOSING);
    }

    private CloseResources closeLocked() {
        if (lifecycleState.get() == LifecycleState.CLOSED) {
            return CloseResources.EMPTY;
        }
        CloseResources resources = new CloseResources(reloadTask, reloadTask != null);
        reloadTask = null;
        lifecycleState.set(LifecycleState.CLOSED);
        return resources;
    }

    private void closeTask(Task taskToClose) {
        try {
            if (taskToClose != null) {
                taskToClose.close();
            }
        } catch (Exception e) {
            LOGGER.log(WARNING, "Failed to close Secret Service TLS reload task", e);
        }
    }

    private void shutdownReloadExecutor() {
        try {
            if (asyncExec.isLoaded()) {
                asyncExec.get().shutdownNow();
            }
        } catch (Exception e) {
            LOGGER.log(WARNING, "Failed to shut down Secret Service TLS reload executor", e);
        }
    }

    private void removeShutdownHandler() {
        try {
            Main.removeShutdownHandler(this);
        } catch (Exception e) {
            LOGGER.log(WARNING, "Failed to remove Secret Service TLS shutdown handler", e);
        }
    }

    @Override
    public void shutdown() {
        close();
    }

    void maybeReload() {
        if (!reloadReady()) {
            return;
        }
        Timer.Sample sample = metrics.reloadStarted();
        try {
            if (loadContext(false)) {
                metrics.reloadSucceeded(sample);
                LOGGER.log(DEBUG, "Certificates were downloaded and dynamically updated");
            } else {
                metrics.reloadSkipped(sample);
            }
        } catch (RuntimeException e) {
            metrics.reloadFailed(sample);
            LOGGER.log(WARNING, "TLS reload failed; keeping the last good context", e);
        }
    }

    private boolean reloadReady() {
        return cfg.reload().enabled()
                && reloadActive();
    }

    private boolean reloadActive() {
        return tlsConfig != null
                && lifecycleState.get() == LifecycleState.INITIALIZED;
    }

    private boolean reloadCanInstall(boolean initialLoad) {
        return initialLoad
                ? lifecycleState.get() == LifecycleState.INITIALIZING
                : reloadActive();
    }

    private byte[] pkiMaterial() {
        PkiConfig pki = cfg.pki();
        return pki.resource()
                .map(DefaultSecretServiceTlsManager::resourceBytes)
                .or(() -> pki.secretPath()
                        .map(this::secretMaterial))
                .filter(value -> value.length > 0)
                .orElseThrow(() -> new IllegalStateException(
                        "PKI material is required; configure pki.resource or pki.secret-path"));
    }

    private byte[] secretMaterial(String secretPath) {
        if (!secretPath.startsWith("/")) {
            throw new IllegalStateException("pki.secret-path must start with '/': " + secretPath);
        }
        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG, "Retrieving SSv2 PKI secret {0}", secretPath);
        }
        return secretResolver.apply(secretPath)
                .filter(value -> value.length > 0)
                .orElseThrow(() -> new IllegalStateException("PKI material not found in Secret Service: " + secretPath));
    }

    private KeyManagerFactory keyManagerFactory(PkiCertificate certificate) {
        List<Certificate> certificates = new ArrayList<>(1 + certificate.getIntermediates().size());
        certificates.add(certificate.getCert());
        certificates.addAll(certificate.getIntermediates());
        return buildKmf(tlsConfig,
                        secureRandom(tlsConfig),
                        certificate.getKey(),
                        certificates.toArray(new Certificate[0]));
    }

    private byte[] trustMaterial() {
        if (tlsConfig.trustAll() || cfg.trust().isEmpty()) {
            return new byte[0];
        }
        Resource trust = cfg.trust().get();
        try (InputStream inputStream = getTrustStream(trust)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read trust material resource: " + trust.location(), e);
        }
    }

    private TrustManagerFactory trustManagerFactory(byte[] trustMaterial) {
        if (tlsConfig.trustAll()) {
            return trustAllTmf();
        }

        Optional<Resource> trust = cfg.trust();
        if (tlsConfig.trust().isEmpty() && trust.isEmpty()) {
            throw new IllegalStateException("Trust CA bundle is required unless TLS trust-all is enabled");
        }

        TrustManagerFactory tmf = createTmf(tlsConfig);
        KeyStore trustStore = internalKeystore(tlsConfig);
        List<X509Certificate> trustCaList = new ArrayList<>(tlsConfig.trust());

        if (trust.isPresent()) {
            trustCaList.addAll(PemReader.readCertificates(new ByteArrayInputStream(trustMaterial)));
        }

        if (trustCaList.isEmpty()) {
            throw new IllegalStateException("Trust CA bundle is empty");
        }

        for (int i = 0; i < trustCaList.size(); i++) {
            try {
                trustStore.setCertificateEntry("trust-ca-" + (i + 1), trustCaList.get(i));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Unable to initialize trust store", e);
            }
        }
        initializeTmf(tmf, trustStore, tlsConfig);
        return tmf;
    }

    private static byte[] digest(byte[] pkiMaterial, byte[] trustMaterial, TlsConfig tlsConfig) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pkiMaterial);
            digest.update((byte) 0);
            digest.update(trustMaterial);
            digest.update((byte) 0);
            digest.update((byte) (tlsConfig.trustAll() ? 1 : 0));
            for (X509Certificate certificate : tlsConfig.trust()) {
                digest.update(certificate.getEncoded());
                digest.update((byte) 0);
            }
            return digest.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute TLS material digest", e);
        }
    }

    private static byte[] resourceBytes(Resource resource) {
        try (InputStream inputStream = freshResource(resource).stream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read PKI material resource: " + resource.location(), e);
        }
    }

    private static Resource freshResource(Resource resource) {
        ResourceConfig prototype = resource.prototype();
        if (configuredResource(prototype)) {
            return Resource.create(prototype);
        }
        return switch (resource.sourceType()) {
        case FILE -> Resource.create(Path.of(resource.location()));
        case CLASSPATH -> Resource.create(resource.location());
        case URL -> Resource.create(URI.create(resource.location()));
        case CONTENT, BINARY_CONTENT, UNKNOWN -> {
            resource.cacheBytes();
            yield resource;
        }
        };
    }

    private static boolean configuredResource(ResourceConfig resource) {
        return resource.path().isPresent()
                || resource.resourcePath().isPresent()
                || resource.uri().isPresent()
                || resource.contentPlain().isPresent()
                || resource.content().isPresent();
    }

    private void scheduleReload() {
        Task task = CronConfig.builder(cfg.reload())
                .executor(asyncExec.get())
                .concurrentExecution(false)
                .task(inv -> maybeReload())
                .build();
        try {
            Main.addShutdownHandler(this);
            reloadTask = task;
        } catch (RuntimeException e) {
            closeTask(task);
            throw e;
        }

        if (LOGGER.isLoggable(DEBUG)) {
            LOGGER.log(DEBUG,
                       () -> SecretServiceTlsManagerConfig.class.getSimpleName()
                               + " scheduled: " + task.description());
        }
    }

    private static ScheduledExecutorService scheduler() {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                                       "secret-service-tls-reload-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    private enum LifecycleState {
        NEW,
        INITIALIZING,
        INITIALIZED,
        CLOSING,
        CLOSED
    }

    private record CloseResources(Task reloadTask, boolean removeShutdownHandler) {
        private static final CloseResources EMPTY = new CloseResources(null, false);
    }
}

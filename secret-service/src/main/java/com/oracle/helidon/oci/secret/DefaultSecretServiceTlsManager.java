/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import io.helidon.Main;
import io.helidon.common.LazyValue;
import io.helidon.common.configurable.Resource;
import io.helidon.common.pki.PemReader;
import io.helidon.common.tls.ConfiguredTlsManager;
import io.helidon.common.tls.TlsConfig;
import io.helidon.config.Config;

import com.oracle.pic.commons.crypto.KeystoreGenerator;
import org.eclipse.microprofile.config.ConfigProvider;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

/**
 * The default implementation (service loader and provider-driven) implementation of {@link SecretServiceTlsManager}.
 */
class DefaultSecretServiceTlsManager extends ConfiguredTlsManager implements SecretServiceTlsManager {
    static final String TYPE = "oci-ssv2";
    private static final System.Logger LOGGER = System.getLogger(DefaultSecretServiceTlsManager.class.getName());

    private final SecretServiceTlsManagerConfig cfg;
    private final AtomicReference<Integer> lastPkiHash = new AtomicReference<>(0);
    private final KeystoreGenerator ksg = new KeystoreGenerator();
    private final LazyValue<ScheduledExecutorService> asyncExec = LazyValue.create(Executors::newSingleThreadScheduledExecutor);

    private TlsConfig tlsConfig;
    private org.eclipse.microprofile.config.Config mpConfig;

    DefaultSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg) {
        this(cfg, "@default", null);
    }

    DefaultSecretServiceTlsManager(SecretServiceTlsManagerConfig cfg,
                                   String name,
                                   io.helidon.common.config.Config config) {
        super(name, TYPE);
        this.cfg = Objects.requireNonNull(cfg);

        // if config changes then will do a reload
        if (config instanceof Config watchableConfig) {
            watchableConfig.onChange(this::config);
        }
    }

    @Override
    public void init(TlsConfig tls) {
        this.tlsConfig = tls;
        this.mpConfig = ConfigProvider.getConfig();

        // the initial loading of the tls
        loadContext(true);

        Main.addShutdownHandler(this::shutdown);

        // Scheduled reloading enabled
        if (cfg.reload().enabled()) {

            // now schedule for reload checking
            String taskIntervalDescription =
                    io.helidon.scheduling.Scheduling.cron()
                            .executor(asyncExec.get())
                            .expression(cfg.reload().cron())
                            .task(inv -> maybeReload())
                            .build()
                            .description();

            LOGGER.log(DEBUG,
                       () -> SecretServiceTlsManagerConfig.class.getSimpleName() + " scheduled: " + taskIntervalDescription);
        }
    }

    @Override // RuntimeType
    public SecretServiceTlsManagerConfig prototype() {
        return cfg;
    }

    /**
     * Sets the backing (possibly updated) configuration for this manager. This will trigger a reload.
     *
     * @param config the new config
     */
    void config(io.helidon.common.config.Config config) {
        Objects.requireNonNull(config);
        maybeReload();
    }

    boolean loadContext(boolean initialLoad) {
        try {
            PkiConfig pki = cfg.pki();
            String certJsonBlob = pki
                    .secret()
                    .map(pkiCertificatePath -> {
                        String configKey = pki.prefix() + pkiCertificatePath;
                        if (LOGGER.isLoggable(DEBUG)) {
                            LOGGER.log(DEBUG, "Retrieving the SSv2 PKI secret " + configKey);
                        }
                        // Loading the secret from config, SecretServiceMpConfigSource is going take care of the download.
                        return mpConfig.getValue(configKey, String.class);
                    })
                    .or(() -> pki.resource().map(Resource::string))
                    .orElseThrow();

            int hash = certJsonBlob.hashCode();
            if (lastPkiHash.getAndSet(hash) == hash) {
                // Same mTls material, no need to reload tls context
                if (LOGGER.isLoggable(DEBUG)) {
                    LOGGER.log(DEBUG, "Identical mTls material, skipping TLS context reload");
                }
                return false;
            }

            PkiCertificate crt = PkiCertificate.newInstance(certJsonBlob, pki.password());

            SecureRandom secureRandom = secureRandom(tlsConfig);

            // See com.oracle.cloudsql.frameworks.core.http.AutoReloadingSslContext
            String keystorePassword = UUID.randomUUID().toString();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            byte[] intermediate = crt.getIntermediatesAsPEM().getBytes();
            byte[] leaf = crt.getLeafCertAsPEM().getBytes();
            byte[] privateKey = crt.getKeyAsPEM().getBytes();
            var keystore = this.ksg.createKeyStoreWithCertChainAndPrivateKey(leaf,
                                                                             intermediate,
                                                                             privateKey,
                                                                             null,
                                                                             keystorePassword);
            kmf.init(keystore, keystorePassword.toCharArray());
            TrustManagerFactory tmf;
            if (tlsConfig.trustAll()) {
                tmf = trustAllTmf();
            } else {
                tmf = createTmf(tlsConfig);
                KeyStore trustStore = internalKeystore(tlsConfig);
                trustStore.load(null, null);

                List<X509Certificate> trustCaList = new ArrayList<>(tlsConfig.trust());

                // Load trust CAs
                try {
                    trustCaList.addAll(PemReader.readCertificates(getTrustStream()));
                } catch (Exception e) {
                    LOGGER.log(WARNING, "Failed to load trust CAs: " + cfg.trust(), e);
                }

                for (int i = 0; i < trustCaList.size(); i++) {
                    trustStore.setCertificateEntry("trust-server-ca-" + (i + 1), trustCaList.get(i));
                }
                tmf.init(trustStore);
            }

            Optional<X509KeyManager> keyManager = Arrays.stream(kmf.getKeyManagers())
                    .filter(m -> m instanceof X509KeyManager)
                    .map(X509KeyManager.class::cast)
                    .findFirst();
            if (keyManager.isEmpty()) {
                throw new RuntimeException("Unable to find X.509 key manager in download: " + pki);
            }

            Optional<X509TrustManager> trustManager = Arrays.stream(tmf.getTrustManagers())
                    .filter(m -> m instanceof X509TrustManager)
                    .map(X509TrustManager.class::cast)
                    .findFirst();
            if (trustManager.isEmpty()) {
                throw new RuntimeException("Unable to find X.509 trust manager in download: " + pki);
            }

            if (initialLoad) {
                initSslContext(tlsConfig, secureRandom, kmf.getKeyManagers(), tmf.getTrustManagers());
            } else {
                reload(keyManager, trustManager);
            }

            return true;
        } catch (KeyStoreException | UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException
                 | IOException e) {
            throw new IllegalStateException("Error while loading context from SSv2", e);
        }
    }

    InputStream getTrustStream() {
        cfg.trust().cacheBytes();
        return cfg.trust().stream();
    }

    private void shutdown() {
        try {
            LOGGER.log(DEBUG, "Shutting down");
            if (asyncExec.isLoaded() && !asyncExec.get().isShutdown()) {
                asyncExec.get().shutdownNow();
            }
        } catch (Exception e) {
            LOGGER.log(WARNING, "Shut down failed", e);
        }
    }

    private void maybeReload() {
        if (loadContext(false)) {
            LOGGER.log(DEBUG, "Certificates were downloaded and dynamically updated");
        }
    }
}

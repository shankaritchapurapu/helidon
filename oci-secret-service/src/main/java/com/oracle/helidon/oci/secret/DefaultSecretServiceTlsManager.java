/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.secret;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import io.helidon.common.tls.ConfiguredTlsManager;
import io.helidon.common.tls.TlsConfig;
import io.helidon.config.Config;
import io.helidon.faulttolerance.Async;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
//import com.oracle.pic.vault.SecretServiceConfig;
//import com.oracle.pic.vault.VaultClient;

/**
 * The default implementation (service loader and provider-driven) implementation of {@link SecretServiceTlsManager}.
 *
 */
class DefaultSecretServiceTlsManager extends ConfiguredTlsManager implements SecretServiceTlsManager {
    static final String TYPE = "oci-certificates-tls-manager";
    private static final System.Logger LOGGER = System.getLogger(DefaultSecretServiceTlsManager.class.getName());

    private final SecretServiceTlsManagerConfig cfg;
    private final AtomicReference<String> lastVersionDownloaded = new AtomicReference<>("");

    // these will only be non-null when enabled
    private ScheduledExecutorService asyncExecutor;
    private Async async;
    private TlsConfig tlsConfig;
//    private final VaultClient vaultClient = null;
    private String VAULT_SECRET_KEY = "secret";

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

    @Override // TlsManager
    public void init(TlsConfig tls) {
        this.tlsConfig = tls;
        Services services = InjectionServices.realizedServices();
        this.asyncExecutor = Executors.newSingleThreadScheduledExecutor();
        this.async = Async.builder().executor(asyncExecutor).build();

        // the initial loading of the tls
        loadContext(true);

        // register for any available graceful shutdown events
        Optional<ServiceProvider<LifecycleHook>> shutdownHook = services.lookupFirst(LifecycleHook.class, false);
        shutdownHook.ifPresent(sp -> sp.get().registerShutdownConsumer(this::shutdown));

        // now schedule for reload checking
        String taskIntervalDescription =
                io.helidon.scheduling.Scheduling.cron()
                        .executor(asyncExecutor)
                        .expression(cfg.schedule())
                        .task(inv -> maybeReload())
                        .build()
                        .description();
        LOGGER.log(System.Logger.Level.DEBUG, () ->
                SecretServiceTlsManagerConfig.class.getSimpleName() + " scheduled: " + taskIntervalDescription);
    }

    private void shutdown(Object event) {
        try {
            LOGGER.log(System.Logger.Level.DEBUG, "Shutting down");
            asyncExecutor.shutdownNow();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Shut down failed", e);
        }
    }

    @Override // RuntimeType
    public SecretServiceTlsManagerConfig prototype() {
        return cfg;
    }

    // ConfiguredTlsManager
    private void maybeReload() {
        if (loadContext(false)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Certificates were downloaded and dynamically updated");
        }
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

    /**
     * Will download new certificates, and if those are determined to be changed will affect the reload of the new key and trust
     * managers.
     *
     * @return true if a reload occurred
     */
    boolean loadContext(boolean initialLoad) {
        try {
            // download all of our security collateral from OCI
//            SecretServiceDownloader.Certificates certificates = cd.loadCertificates(cfg.certOcid());
//            if (lastVersionDownloaded.get().equals(certificates.version())) {
//                assert (!initialLoad);
//                return false;
//            }

            // reset start time for the next update phase
            Certificate ca = null;
//            Certificate ca = cd.loadCACertificate(cfg.caOcid());

//            PrivateKey key = pd.loadKey(cfg.keyOcid(), cfg.vaultCryptoEndpoint());

//            VaultClient vaultClient = new VaultClient(
//                    SecretServiceConfig.builder().build(),
//                    InstancePrincipalsAuthenticationDetailsProvider.builder().build()
//            );

            Map<String, String> rawSecret = Map.of();//vaultClient.getSecret(cfg.pkiCertificatePath()).getData();
            // vault client's getSecret returns a map of key-value pairs
            // however, in practice there is only one key which is 'secret'
            if (rawSecret.size() != 1) {
                throw new RuntimeException("Too many SSv2 secret entries: " + rawSecret.size());
            } else if (!rawSecret.containsKey(VAULT_SECRET_KEY)) {
                throw new RuntimeException("SSv2 vault secret key is missing");
            }
            LOGGER.log(System.Logger.Level.INFO, "retrieving the SSv2 secret");
            String certJsonBlob = rawSecret.get(VAULT_SECRET_KEY);
            PrivateKey key = null;
            SecureRandom secureRandom = secureRandom(tlsConfig);
            //TODO: see com.oracle.cloudsql.frameworks.core.http.AutoReloadingSslContext
            KeyManagerFactory kmf = buildKmf(tlsConfig, secureRandom, key, new Certificate[0]);

            TrustManagerFactory tmf;
            if (tlsConfig.trustAll()) {
                tmf = trustAllTmf();
            } else {
                tmf = createTmf(tlsConfig);
                KeyStore keyStore = internalKeystore(tlsConfig);
                keyStore.setCertificateEntry("trust-ca", ca);
                tmf.init(keyStore);
            }

            Optional<X509KeyManager> keyManager = Arrays.stream(kmf.getKeyManagers())
                    .filter(m -> m instanceof X509KeyManager)
                    .map(X509KeyManager.class::cast)
                    .findFirst();
            if (keyManager.isEmpty()) {
                throw new RuntimeException("Unable to find X.509 key manager in download: " + cfg.pkiCertificatePath());
            }

            Optional<X509TrustManager> trustManager = Arrays.stream(tmf.getTrustManagers())
                    .filter(m -> m instanceof X509TrustManager)
                    .map(X509TrustManager.class::cast)
                    .findFirst();
            if (trustManager.isEmpty()) {
                throw new RuntimeException("Unable to find X.509 trust manager in download: " + cfg.pkiCertificatePath());
            }

            if (initialLoad) {
                initSslContext(tlsConfig, secureRandom, kmf.getKeyManagers(), tmf.getTrustManagers());
            } else {
                reload(keyManager, trustManager);
            }

            return true;
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Error while loading context from OCI", e);
        }
    }

}

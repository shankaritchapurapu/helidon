/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.integrations.oci.tls.pki;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.tls.TlsConfig;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.DefaultConfigurator;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.pic.vault.CacheConfig;
import com.oracle.pic.vault.RetryConfig;
import com.oracle.pic.vault.SecretServiceConfig;
import com.oracle.pic.vault.VaultClient;
import com.oracle.pic.vault.VaultUserClient;
import com.oracle.pic.vault.requests.GetSecretRequest;
import com.oracle.pic.vault.responses.GetSecretResponse;
//import com.oracle.pic.secinf.externalsdk.SecretServiceClient;
//import com.oracle.pic.secinf.externalsdk.model.SecretVersion;
//import com.oracle.pic.secinf.externalsdk.requests.GetSecretVersionsRequest;
//import com.oracle.pic.secinf.externalsdk.responses.GetSecretVersionsResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.List;

public class SSV2 {
    static final System.Logger LOGGER = System.getLogger(SSV2.class.getName());
    static final String CA_BUNDLE_PATH = "./target/classes/ca-bundle.pem";

    public static void main(String args[]) {
        if (args.length < 3) {
            throw new IllegalArgumentException("argument required; usage:\n\t <c=config | i=instance-principals> <command> <commandArgs>");
        }

        String auth = args[0].toLowerCase();
        String command = args[1].toLowerCase();
        String commandArg = args[2];

//        AbstractAuthenticationDetailsProvider provider = OciExtension.ociAuthenticationProvider().get();
        AbstractAuthenticationDetailsProvider provider;
        try {
            if (auth.startsWith("i")) {
                provider = InstancePrincipalsAuthenticationDetailsProvider.builder().build();
                LOGGER.log(System.Logger.Level.INFO, "Using Instance Principals Auth");
            } else {
                provider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
                LOGGER.log(System.Logger.Level.INFO, "Using DEFAULT Config File Auth");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

//        com.oracle.pic.commons.client.http.auth.TlsConfig tlsConfig = com.oracle.pic.commons.client.http.auth.TlsConfig.builder()
//                .caBundle(CA_BUNDLE_PATH)
//                .build();
        TlsConfig tlsConfig = TlsConfig.builder()
                .caBundle(CA_BUNDLE_PATH)
                .build();
        CacheConfig cacheConfig = CacheConfig.builder()
                .cacheType(CacheConfig.CacheType.NO_CACHE)
                .build();
        RetryConfig retryConfig = RetryConfig.builder()
                .maxRetries(5)
                .minRetryDelayInMs(200)
                .maxRetryDelayInMs(2000)
                .build();
        SecretServiceConfig config = SecretServiceConfig.builder()
                .tlsConfig(tlsConfig)
                .cacheConfig(cacheConfig)
                .retryConfig(retryConfig)
                .endpoint("https://secret-management-service.us-ashburn-1.oracleiaas.com/")
                .build();

        VaultClient vaultClient = new VaultClient(config, (BasicAuthenticationDetailsProvider) provider);
        com.oracle.pic.vault.model.GetSecretResponse resp = vaultClient.getSecret("/secret/helidon/test-secret/latest");
        System.out.println("Secret Value: " + resp.toString());

//        GetSecretRequest request = GetSecretRequest.builder()
//                .path("/secret/helidon/test-secret/latest")
//                .buildWithoutInvocationCallback();
//        try (com.oracle.pic.vault.VaultUserClient vaultUserClient = VaultUserClient.builder()
//                .build(provider)) {
//            GetSecretResponse response = vaultUserClient.getSecret(request);
//            com.oracle.pic.vault.model.GetSecretResponse r = response.getGetSecretResponse();
//            System.out.println("Secret Value: " + r.toString());
//        }

//        try (SecretServiceClient client = SecretServiceClient.builder()
//                .clientConfigurator(ClientConfiguratorHelper.buildClientConfiguratorWithTrustStore(CA_BUNDLE_PATH))
//                .endpoint("https://secret-management-service.us-ashburn-1.oracleiaas.com/")
//                .build(provider)) {
//            String out;
//
//            switch (command) {
//                case "getsecretversions":
//                    out = new GetSecretDefinition().handle(client, commandArg);
//                break;
//
//                default:
//                    throw new IllegalArgumentException("unknown command: " + commandArg);
//            }
//
//            System.out.println(out);
//        }
    }

//    interface Handler {
//        String handle(SecretServiceClient client,
//                      String arg);
//    }
//
//    static class GetSecretDefinition implements Handler {
//        @Override
//        public String handle(SecretServiceClient client,
//                             String secretDefOcid) {
//            GetSecretVersionsRequest getSecretVersionsRequest = GetSecretVersionsRequest.builder()
//                    .secretDefOcid(secretDefOcid)
//                    .buildWithoutInvocationCallback();
//            GetSecretVersionsResponse getSecretVersionResponse = client.getSecretVersions(getSecretVersionsRequest);
//            List<SecretVersion> list = getSecretVersionResponse.getItems();
//            return list.toString();
//        }
//    }
//
//    static class ClientConfiguratorHelper {
//        public static ClientConfigurator buildClientConfiguratorWithTrustStore(String bundlePath) {
//            return new DefaultConfigurator.NonBuffering() {
//                @Override
//                public void customizeClient(HttpClientBuilder builder) {
//                    super.customizeClient(builder);
//
//                    try {
//                        KeyStore trustStore =
//                                KeystoreGenerator.createTrustStoreWithServerCa(bundlePath);
////                        builder.trustStore(new KeystoreGenerator().createTrustStoreWithServerCa(bundlePath));
//                        builder.property(StandardClientProperties.TRUST_STORE, trustStore);
//                    } catch (Exception ex) {
//                        throw new RuntimeException("Failed generating client configurator", ex);
//                    }
//                }
//            };
//        }
//    }
//
//    static class KeystoreGenerator {
//        /**
//         * Given a path to a PEM formatted certificate or certificate bundle, create a {@link KeyStore}
//         * containing the certificate as a trusted entity.
//         *
//         * @param serverCaPemPath path to the server CA or CA bundle file in PEM format
//         * @return KeyStore configured to trust the given server CA
//         * @throws java.security.KeyStoreException if we had issues creating or modifying a KeyStore instance
//         * @throws IOException if we had issues reading the file
//         * @throws NoSuchAlgorithmException if there were issues modifying a KeyStore instance
//         * @throws CertificateException if there were issues converting the file into a {@link
//         *     Certificate}
//         */
//        public static KeyStore createTrustStoreWithServerCa(String serverCaPemPath)
//                throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
//            if (serverCaPemPath == null) {
//                throw new java.lang.NullPointerException(
//                        "serverCaPemPath is marked non-null but is null");
//            }
//            // Create a keystore and load it. We must load a keystore before we can read or write to it
//            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            trustStore.load(null, null);
//            Collection<? extends Certificate> serverCaCerts = getCertificateFromPem(serverCaPemPath);
//            // we use a static cert alias rather than some identifier from the certs, because these
//            // keystores are going to be
//            // in memory and nobody is going to be reading them
//            int i = 1;
//            for (Certificate serverCaCert : serverCaCerts) {
//                trustStore.setCertificateEntry("server-ca-" + i++, serverCaCert);
//                LOGGER.log(System.Logger.Level.INFO, "Adding trust for cert: server-ca-" + i);
//            }
//            return trustStore;
//        }
//
//
//        /**
//         * Given path to the client certificate , generate a client certificate
//         *
//         * @param pemPath path to the certificate
//         * @return client certificate.
//         * @throws CertificateException if there were issues converting a file into a {@link
//         *     Certificate}
//         */
//        private static Collection<? extends Certificate> getCertificateFromPem(String pemPath)
//                throws IOException, CertificateException {
//            try (FileInputStream inputStream = new FileInputStream(pemPath)) {
//                return getCertificateFromInputStream(inputStream);
//            }
//        }
//
//        /**
//         * Given input stream of the client certificate , generate a client certificate
//         *
//         * @param pem input stream of the certificate
//         * @return client certificate.
//         * @throws CertificateException if there were issues converting a file into a {@link
//         *     Certificate}
//         */
//        private static Collection<? extends Certificate> getCertificateFromInputStream(InputStream pem)
//                throws CertificateException {
//            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//            return cf.generateCertificates(pem);
//        }
//    }
//
}

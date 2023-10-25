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

import java.net.URI;

public class SSV2SecretDownloader implements SecretDownloader {
    public final String VERSION_LATEST = "latest";
    final int MAX_ATTEMPTS = 10;

    // TODO:
    final String bundlePath = "target/ca-bundle.pem";
//    final URI secretServiceEndpoint = new URI("https://secret-management-service.ashburn-1.oracleiaas.com");

    @Override
    public char[] loadSecret(String secretOcid,
                             URI secretServiceEndpoint) {
//        // CP Throttling retries
//        Retriers.setDefaultRetryConfiguration(
//                RetryConfiguration.builder()
//                        .terminationStrategy(new MaxAttemptsTerminationStrategy(MAX_ATTEMPTS)) // set max-attempts to whatever makes sense for your app
//                        .delayStrategy(new ExponentialBackoffDelayStrategy(61_000L)) // do not change this
//                        .retryCondition((BmcException e) -> e.getStatusCode() == 429)
//                        .build());
//        ClientConfigurator clientConfigurator = ClientConfiguratorHelper
//                .buildClientConfiguratorWithTrustStore(bundlePath);
//        ClientConfiguration clientConfig = ClientConfiguration.builder()
//                .build();
//        AbstractAuthenticationDetailsProvider provider = OciExtension.ociAuthenticationProvider().get();
//
////        String opcRequestId = OpcRequestHelper.setAndGetClientOpcRequestId();
//        try (SecretServiceClient client = SecretServiceClient.builder()
////                .clientConfigurator(clientConfigurator)
//                .build(provider)) {
////            client.setEndpoint(secretServiceEndpoint.toString());
//            GetSecretVersionsRequest getSecretVersionsRequest = GetSecretVersionsRequest.builder()
//                    .secretDefOcid(secretOcid)
//                    .buildWithoutInvocationCallback();
//            GetSecretVersionsResponse getSecretVersionResponse = client.getSecretVersions(getSecretVersionsRequest);
//            List<SecretVersion> list = getSecretVersionResponse.getItems();
//        }

        return null;
    }


//    static class ClientConfiguratorHelper {
//        // TODO reloading the cert has to be done
//        public static ClientConfigurator buildClientConfiguratorWithTrustStore(String bundlePath) {
//            return new DefaultConfigurator.NonBuffering() {
//                @Override
//                public void customizeClient(HttpClientBuilder builder) {
//                    super.customizeClient(builder);
//                    try {
//                        builder.trustStore(new KeystoreGenerator().createTrustStoreWithServerCa(bundlePath));
//                    } catch (Exception ex) {
//                        throw new RuntimeException("Failed generating client configurator", ex);
//                    }
//                }
//            };
//        }
//
//    }
}

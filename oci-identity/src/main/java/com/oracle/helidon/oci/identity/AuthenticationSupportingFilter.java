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

package com.oracle.helidon.oci.identity;

import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.mp.MpConfig;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Makes the header value representing the SHA hash digest of the request.
 */
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-identity/src/main/java/com/oracle/oci/sfw/netty/identity/authentication/Authenticator.java
@PreMatching
@Priority(Priorities.AUTHENTICATION - 50) // runs after the opc request id generator
public class AuthenticationSupportingFilter implements ContainerRequestFilter {
    /**
     * The top level config key used to configure this filter.
     */
    public static final String TAG_CONFIG_KEY = "oci-identity";

    /**
     * The header key that will be used to add the calculated digest value (when the URI matches what is configured).
     */
    public static final String TAG_HEADER = "X-HELIDON-DIGEST";

    private static final LazyValue<Configuration> CONFIG = LazyValue
            .create(() -> new Configuration(globalMpConfig().get(TAG_CONFIG_KEY)));

    private final Configuration config;

    public AuthenticationSupportingFilter() {
        this(null);
    }

    AuthenticationSupportingFilter(Configuration config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext rc) {
        boolean hasBody = rc.hasEntity();
        if (hasBody
                && matches("/" + rc.getUriInfo().getPath())) {
            RepeatableInputStreamer.Stream stream = RepeatableInputStreamer.create(rc.getEntityStream());
            RepeatableInputStreamer.ReplayStream replayStream = stream.replay();

            String digest = DigestStreamer.calculateDigest(stream);
            rc.getHeaders().add(TAG_HEADER, digest);

            rc.setEntityStream(replayStream);
        }
    }

    /**
     * Returns true if the given request URI matches against one of the configured {@link Configuration#uriPrefix()} list.
     *
     * @param requestUriPath the request uri path
     * @return true if matches
     */
    boolean matches(String requestUriPath) {
        return configuration().uriPrefix().stream()
                .anyMatch(requestUriPath::startsWith);
    }

    /**
     * Returns {@code true} if this feature was explicitly configured.
     *
     * @return true if explicitly configured, false if using defaults
     */
    public boolean isConfigured() {
        return configuration().exists();
    }

    Configuration configuration() {
        return (config != null) ? config : CONFIG.get();
    }

    private static Config globalMpConfig() {
        return MpConfig.toHelidonConfig(ConfigProvider.getConfig());
    }


    /**
     * The configuration governing this feature's behavior.
     */
    static class Configuration {
        private final Config config;
        private final LazyValue<List<String>> uriPrefix = LazyValue
                .create(this::loadUriPrefix);

        Configuration(Config config) {
            this.config = config;
        }

        /**
         * The list of URI prefix that if matched on the request will trigger the digest hash to be calculated on the body. The
         * default is {@code "/"}.
         *
         * @return the list of prefix hashes
         */
        public List<String> uriPrefix() {
            return uriPrefix.get();
        }

        boolean exists() {
            return config.exists();
        }

        private List<String> loadUriPrefix() {
            return config.get("uriPrefix").asList(String.class).orElse(List.of("/"));
        }
    }

}

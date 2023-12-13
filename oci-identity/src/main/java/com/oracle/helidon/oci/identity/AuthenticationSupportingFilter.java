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

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;

/**
 * Makes the header value representing the SHA hash digest of the request.
 * <p>
 * Be sure to configure the filter (see README.md of this module) as necessary.
 */
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-identity/src/main/java/com/oracle/oci/sfw/netty/identity/authentication/Authenticator.java
@PreMatching
@Priority(Priorities.AUTHENTICATION - 50) // runs after the opc request id generator
public class AuthenticationSupportingFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationSupportingFilter.class.getName());
    private static boolean LOGGED;

    /**
     * The header key that will be used to add the calculated digest value (when the URI matches what is configured).
     */
    public static final String TAG_DEFAULT_HEADER = OciHeaderNames.X_CONTENT_SHA256;

    private static final LazyValue<Configuration> CONFIG = LazyValue
            .create(() -> new Configuration(OciIdentityConfiguration.globalOciIdentityConfig()));

    private final Configuration config;

    public AuthenticationSupportingFilter() {
        this(CONFIG.get());
    }

    AuthenticationSupportingFilter(Config config) {
        this.config = new Configuration(Objects.requireNonNull(config));
    }

    AuthenticationSupportingFilter(Configuration config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public void filter(ContainerRequestContext rc) {
        boolean hasBody = rc.hasEntity();
        boolean qualifies = (hasBody && matches("/" + rc.getUriInfo().getPath()));
        LOGGER.log(Level.FINE, "filter qualifies: " + rc.getUriInfo().getPath() + ": " + qualifies);
        if (qualifies) {
            RepeatableInputStreamer.Stream stream = createRepeatableStream(rc.getEntityStream());
            RepeatableInputStreamer.ReplayStream replayStream = stream.replay();

            String digest = DigestStreamer.calculateDigest(stream);
            rc.getHeaders().add(configuration().headerTag(), digest);

            rc.setEntityStream(replayStream);
            assert(rc.hasEntity());
        }
    }

    /**
     * Returns {@code true} if this feature was explicitly configured.
     *
     * @return true if explicitly configured, false if using defaults
     */
    public boolean isConfigured() {
        return configuration().exists();
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

    RepeatableInputStreamer.Stream createRepeatableStream(InputStream is) {
        return RepeatableInputStreamer.create(is,
                                       // share the same config that we use for ourselves
                                       RepeatableInputStreamer.loadConfig(config.config, false));
    }

    Configuration configuration() {
        if (!LOGGED) {
            LOGGED = true;
            LOGGER.log(Level.FINE, AuthenticationSupportingFilter.class.getSimpleName() + " configuration: " + config);
        }
        return config;
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

        /**
         * The tag to use as the header key. The default value is {@link #TAG_DEFAULT_HEADER}.
         *
         * @return the header key
         */
        public String headerTag() {
            return config.get("headerTag").asString().orElse(TAG_DEFAULT_HEADER);
        }

        @Override
        public String toString() {
            return "{\theaderTag: " + headerTag() + ";\n\turiPrefix: " + uriPrefix() + "\n}";
        }

        boolean exists() {
            return config.exists();
        }

        private List<String> loadUriPrefix() {
            return config.get("uriPrefix").asList(String.class).orElse(List.of("/"));
        }
    }

}

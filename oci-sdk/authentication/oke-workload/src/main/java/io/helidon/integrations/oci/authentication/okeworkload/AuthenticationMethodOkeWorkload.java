/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
 */

package io.helidon.integrations.oci.authentication.okeworkload;

import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.integrations.oci.OciResourcePrincipalProvider;
import io.helidon.integrations.oci.spi.OciAuthenticationMethod;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder;

/**
 * OKE workload identity authentication method and resource-principal provider.
 */
@Weight(Weighted.DEFAULT_WEIGHT - 50)
@Service.Provider
class AuthenticationMethodOkeWorkload implements OciAuthenticationMethod, OciResourcePrincipalProvider {
    private static final System.Logger LOGGER = System.getLogger(AuthenticationMethodOkeWorkload.class.getName());
    private static final String METHOD = "oke-workload-identity";

    /*
     * These constants are copied from OkeWorkloadIdentityAuthenticationDetailsProvider because
     * they are not public.
     */
    private static final String SERVICE_ACCOUNT_CERT_PATH_DEFAULT =
            "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    private static final String SERVICE_ACCOUNT_CERT_PATH_ENV = "OCI_KUBERNETES_SERVICE_ACCOUNT_CERT_PATH";

    private final LazyValue<Optional<BasicAuthenticationDetailsProvider>> provider;

    AuthenticationMethodOkeWorkload(
            Supplier<Optional<OkeWorkloadIdentityAuthenticationDetailsProviderBuilder>> builder) {
        provider = createProvider(builder);
    }

    @Override
    public String method() {
        return METHOD;
    }

    @Override
    public Optional<BasicAuthenticationDetailsProvider> provider() {
        return provider.get();
    }

    private static LazyValue<Optional<BasicAuthenticationDetailsProvider>> createProvider(
            Supplier<Optional<OkeWorkloadIdentityAuthenticationDetailsProviderBuilder>> builder) {
        return LazyValue.create(() -> {
            if (available()) {
                return builder.get().map(OkeWorkloadIdentityAuthenticationDetailsProviderBuilder::build);
            }
            return Optional.empty();
        });
    }

    private static boolean available() {
        String usedPath = System.getenv(SERVICE_ACCOUNT_CERT_PATH_ENV);
        usedPath = usedPath == null ? SERVICE_ACCOUNT_CERT_PATH_DEFAULT : usedPath;
        Path certPath = Paths.get(usedPath);

        if (!Files.exists(certPath)) {
            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE,
                           "OKE workload authentication is unavailable because the certificate file does not exist: "
                                   + certPath.toAbsolutePath());
            }
            return false;
        }
        if (Files.isRegularFile(certPath)) {
            return true;
        }

        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE,
                       "OKE workload authentication is unavailable because the certificate path is not a file: "
                               + certPath.toAbsolutePath());
        }
        return false;
    }
}

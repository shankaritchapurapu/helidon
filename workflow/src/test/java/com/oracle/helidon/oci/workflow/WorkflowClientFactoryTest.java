/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.workflow;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.Services;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.Region;
import com.oracle.pic.workflow.Utils.ClientRole;
import com.oracle.pic.workflow.module.WorkflowClientModule;
import com.oracle.pic.workflow.client.v1.WFaaSClient;
import com.oracle.pic.workflow.worker.ChastWorkflowClient;
import com.oracle.pic.workflow.worker.RetryPolicy;
import com.oracle.pic.workflow.worker.WorkflowClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowClientFactoryTest {

    @BeforeAll
    static void setUpRegistry() {
        ServiceRegistryManager.start();
        Services.set(Config.class, Config.empty());
        Services.set(BasicAuthenticationDetailsProvider.class, authProvider());
    }

    @Test
    void createConfiguredChastWorkflowClientWithOldEndpointConfig() {
        WorkflowConfig config = WorkflowConfig.builder()
                .domainId("localhost")
                .endpointDetails(EndpointConfig.builder()
                                         .serverEndpoint("http://localhost:39000")
                                         .connectTimeout(Duration.ofSeconds(5))
                                         .workerReadTimeout(Duration.ofSeconds(45))
                                         .pollerReadTimeout(Duration.ofSeconds(90))
                                         .build())
                .workerIdentifier("workflow-worker-1")
                .build();

        BasicAuthenticationDetailsProvider authProvider = authProvider();
        WorkflowClient client = new WorkflowClientFactory(config, authProvider).get();

        try {
            assertEquals("localhost", client.getDomainId(), "Workflow domain id should match");
            WFaaSClient innerClient = (WFaaSClient) ((ChastWorkflowClient) client).getClient();
            assertEquals("http://localhost:39000", innerClient.getEndpoint(),
                         "WFaaS endpoint should match configured value");
            assertEquals("workflow-worker-1", workerIdentifier((ChastWorkflowClient) client),
                         "Configured worker identifier should be used");
        } finally {
            ((WFaaSClient) ((ChastWorkflowClient) client).getClient()).close();
        }
    }

    @Test
    void createConfiguredChastWorkflowClientUsesDeclarativeRetryPolicy() {
        WorkflowConfig config = WorkflowConfig.builder()
                .domainId("localhost")
                .retryPolicy(RetryPolicyConfig.builder()
                                     .maxRetryCount(9)
                                     .delayBetweenRetry(Duration.ofMillis(150))
                                     .jitterFactor(0.25d)
                                     .build())
                .build();

        BasicAuthenticationDetailsProvider authProvider = authProvider();
        WorkflowClient client = new WorkflowClientFactory(config, authProvider).get();

        try {
            RetryPolicy retryPolicy = retryPolicy((ChastWorkflowClient) client);
            assertEquals(9, intField(retryPolicy, "maxRetryCount"),
                         "Configured retry max count should be used");
            assertEquals(150L, longField(retryPolicy, "delayBetweenRetryInMillis"),
                         "Configured retry delay should be used");
            assertEquals(0.25d, doubleField(retryPolicy, "jitterFactor"),
                         "Configured retry jitter should be used");
        } finally {
            ((WFaaSClient) ((ChastWorkflowClient) client).getClient()).close();
        }
    }

    @Test
    void createConfiguredChastWorkflowClientUsesOldWorkerIdentifierDefaultWhenUnset() {
        WorkflowConfig config = WorkflowConfig.builder()
                .build();

        BasicAuthenticationDetailsProvider authProvider = authProvider();
        WorkflowClient client = new WorkflowClientFactory(config, authProvider).get();

        try {
            assertEquals("localhost", client.getDomainId(), "Default workflow domain id should match");
            assertEquals(ManagementFactory.getRuntimeMXBean().getName(), workerIdentifier((ChastWorkflowClient) client),
                         "Default worker identifier should match the old module behavior");
        } finally {
            ((WFaaSClient) ((ChastWorkflowClient) client).getClient()).close();
        }
    }

    @Test
    void createConfiguredChastWorkflowClientRejectsTimeoutLongerThanIntMillis() {
        WorkflowConfig config = WorkflowConfig.builder()
                .endpointDetails(EndpointConfig.builder()
                                         .connectTimeout(Duration.ofDays(30))
                                         .build())
                .build();

        BasicAuthenticationDetailsProvider authProvider = authProvider();

        assertThrows(ArithmeticException.class,
                     () -> new WorkflowClientFactory(config, authProvider).get(),
                     "Timeouts larger than int millis should fail fast");
    }

    @Test
    void createConfiguredChastWorkflowClientDoesNotResolveAuthProviderWhenDynamicSslAuthIsNotNeeded() {
        AtomicInteger calls = new AtomicInteger();
        WorkflowConfig config = WorkflowConfig.builder()
                .domainId("localhost")
                .build();
        Config rootConfig = Config.just(ConfigSources.create(Map.of(
                "oci.dynamic-ssl-context-provider.root-cert-path", "/etc/oci-pki/ca-bundle.pem")));

        WorkflowClient client = new WorkflowClientFactory(rootConfig, config, () -> {
            calls.incrementAndGet();
            return Optional.empty();
        }).get();

        try {
            assertEquals(0, calls.get(),
                         "Auth provider supplier should not be called when localhost skips dynamic SSL auth details");
        } finally {
            ((WFaaSClient) ((ChastWorkflowClient) client).getClient()).close();
        }
    }

    @Test
    void createConfiguredChastWorkflowClientRequiresAuthProviderWhenDynamicSslRootCertIsConfigured() {
        WorkflowConfig config = WorkflowConfig.builder()
                .domainId("workflow-domain")
                .build();
        Config rootConfig = Config.just(ConfigSources.create(Map.of(
                "oci.dynamic-ssl-context-provider.root-cert-path", "/etc/oci-pki/ca-bundle.pem")));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                       () -> new WorkflowClientFactory(rootConfig,
                                                                                       config,
                                                                                       Optional::empty).get());

        assertTrue(exception.getMessage().contains("BasicAuthenticationDetailsProvider"),
                   "Failure should explain the missing auth provider");
    }

    @Test
    void workerNamedSupplierCreatesWorkerClient() {
        AtomicReference<ClientRole> requestedRole = new AtomicReference<>();
        WorkflowClient expectedClient = workflowClientProxy();
        WorkflowClientFactory workflowClientFactory = recordingFactory(requestedRole, expectedClient);

        WorkflowClient client = new WorkerClientSupplier(workflowClientFactory).get();

        assertSame(expectedClient, client, "Worker supplier should return the created client");
        assertEquals(ClientRole.WORKER, requestedRole.get(), "Worker supplier should request the worker role");
    }

    @Test
    void pollerNamedSupplierCreatesPollerClient() {
        AtomicReference<ClientRole> requestedRole = new AtomicReference<>();
        WorkflowClient expectedClient = workflowClientProxy();
        WorkflowClientFactory workflowClientFactory = recordingFactory(requestedRole, expectedClient);

        WorkflowClient client = new PollerClientSupplier(workflowClientFactory).get();

        assertSame(expectedClient, client, "Poller supplier should return the created client");
        assertEquals(ClientRole.POLLER, requestedRole.get(), "Poller supplier should request the poller role");
    }

    @Test
    void namedPollerClientIsAvailableFromServiceRegistry() {
        WorkflowClient pollerClient = Services.getNamed(WorkflowClient.class,
                                                        WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME);

        try {
            assertEquals("localhost", pollerClient.getDomainId(),
                         "Named poller client should be resolved from the service registry");
        } finally {
            ((WFaaSClient) ((ChastWorkflowClient) pollerClient).getClient()).close();
        }
    }

    private static WorkflowClientFactory recordingFactory(AtomicReference<ClientRole> requestedRole,
                                                          WorkflowClient expectedClient) {
        return new WorkflowClientFactory(WorkflowConfig.builder().build(), authProvider()) {
            @Override
            WorkflowClient createWorkflowClient(ClientRole clientRole) {
                requestedRole.set(clientRole);
                return expectedClient;
            }
        };
    }

    private static WorkflowClient workflowClientProxy() {
        return (WorkflowClient) Proxy.newProxyInstance(
                WorkflowClient.class.getClassLoader(),
                new Class<?>[]{WorkflowClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDomainId" -> "localhost";
                    case "toString" -> "WorkflowClientProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                });
    }

    private static String workerIdentifier(ChastWorkflowClient client) {
        try {
            Field workerIdentifier = ChastWorkflowClient.class.getDeclaredField("workerIdentifier");
            workerIdentifier.setAccessible(true);
            return (String) workerIdentifier.get(client);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inspect worker identifier", e);
        }
    }

    private static RetryPolicy retryPolicy(ChastWorkflowClient client) {
        try {
            Field retryPolicy = ChastWorkflowClient.class.getDeclaredField("retryPolicy");
            retryPolicy.setAccessible(true);
            return (RetryPolicy) retryPolicy.get(client);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inspect retry policy", e);
        }
    }

    private static int intField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inspect int field: " + fieldName, e);
        }
    }

    private static long longField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getLong(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inspect long field: " + fieldName, e);
        }
    }

    private static double doubleField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getDouble(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inspect double field: " + fieldName, e);
        }
    }

    private static BasicAuthenticationDetailsProvider authProvider() {
        Path keyPath = Path.of("..", "limits", "src", "test", "resources", "key.pem");
        return SimpleAuthenticationDetailsProvider.builder()
                .tenantId("ocid1.tenancy.oc1..testtenant")
                .userId("ocid1.user.oc1..testuser")
                .fingerprint("11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00")
                .region(Region.US_ASHBURN_1)
                .privateKeySupplier(() -> {
                    try {
                        return Files.newInputStream(keyPath);
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to read test private key", e);
                    }
                })
                .build();
    }
}

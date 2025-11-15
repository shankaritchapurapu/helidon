# WFaaS java client

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

---

## Overview

The Workflow as a Service (WFaaS) java client library provides basic control plane APIs, as well as a worker library that manages a thread of workers, polls for new work, polls while doing work, and interrupts work-doing threads when operators cancel work. Application code resides in callback functions invoked by the worker library when it receives new work.

This module packages all of the dependencies required to make the WFaaS java client work without having to define them individually in your project. Moreover, the generated java client is adjusted so that the library will be compatible with OCI Java SDK 3 which is the version used by the oci-helidon project.

---

## Maven Coordinates

To be able to integrate with WFaaS java client, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-workflow-java-client</artifactId>
</dependency>
```

---

## Usage

Once the module is added as a dependency in the application's pom.xml, you can programmatically add workflow definition and steps as annotations
and then launch, cancel or poll workflows in your application. The [sample directory](https://bitbucket.oci.oraclecorp.com/projects/WOR/repos/wfaas-client/browse/chast-javaclient/src/main/java/com/oracle/pic/workflow/sample/DelayedExecutionWorkflow.java) from the wfaas-client repository provides some example standalone applications that can be used as a template for using WFaaS.

The module also contains a Jakarta bean that will allow the worker and poller clients to be injected as Named annotations in a Helidon MP application. 

```java
@Inject
public ReferenceServiceResource(@Named(WorkflowClientModule.WORKFLOW_WORKER_CLIENT_NAME) WorkflowClient workerClient,
                                @Named(WorkflowClientModule.WORKFLOW_POLLER_CLIENT_NAME) WorkflowClient pollerClient) {
    this.workerClient = workerClient;
    this.pollerCLient = pollerClient;
}

public static void demoWorkflow(WorkflowClient workerClient, WorkflowClient pollerClient) throws SerializationException, InterruptedException, BmcException {
    final long startTimestamp = Instant.now().toEpochMilli();

    WorkflowDefinition definition = createWorkflowDef();
    workerClient.postWorkflowDefinition(definition);

    WorkerManagerV3<DemoWorkflowArguments, DemoWorkflowState> workerManager;

    String tag = "tag";
    DemoWorkflow wfDefInstance = new DemoWorkflow();
    workerManager = WorkerManagerV3.<DemoWorkflowArguments, DemoWorkflowState>builder()
            .fromAnnotations(wfDefInstance, new TypeToken<DemoWorkflowArguments>() {
            }, new TypeToken<DemoWorkflowState>() { })
            .workerClient(workerClient)
            .pollerClient(pollerClient)
            .longPollTimeoutMillis(WFaaSClientUtil.LONG_POLL_TIMEOUT_MILLIS)
            .registerInterceptorForEachStep(stepInterceptor)
            .numWorkerThreads(200)
            .workerTag(tag)
            .workerProject("workerProject")
            .workerFleet("workerFleet")
            .build();
    // Process workerManager here...
}
```
Following are some tips on how to successfully run a WFaaS client:

1. When injecting named annotations to retrieve worker and poller clients (like the example shown above), use the Helidon configuration to set them up. Check [Configuration](#configuration) section for more details,
2. For programmatically creating the Workflow client, configure authentication by ensuring that the AuthDetailsConfig is set properly, that includes instantiating a DynamicSslContextProviderConfig with the proper root certificate and adding the right authentication details provider. The authentication details provider can be retrieved from Helidon's service registry if configured properly:
   * Setting **oci-config.yaml** for Instance Principal authentication:
     ```yaml
     authentication-method: "instance-principal"
     imds-timeout: "PT3S"
     imds-detect-retries: 1
     ```
   * Programatically setting up AuthDetailsConfig
     ```java
     String rootCertPath = "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem";
     var dynSslCtxConfig = new DynamicSslContextProviderConfig(null, null, null, rootCertPath);
     var authenticationDetailsProvider = GlobalServiceRegistry.registry().get(BasicAuthenticationDetailsProvider.class);
  
     AuthDetailsConfig authDetailsConfig = new AuthDetailsConfig(dynSslCtxConfig,  authenticationDetailsProvider)
     ```
3. Check out [How do I find my domainID?](https://confluence.oraclecorp.com/confluence/display/Workflow/Workflow+FAQ#WorkflowFAQ-HowdoIfindmydomainID) to understand how a DomainID can be set.
   ```java
   String ad = getAD();
   String workflowName = "oh-ref-cp-demo.wfaas-overlay"
   String domainId = workflowName + "." + ad + ".us-ashburn-1.oracleiaas.com";
   ```
   *Note:* Use `localhost` as the value of domain ID when running against an in-memory Workflow Server used for testing.
4. Check out [What is the endpoint of WFaaS?](https://confluence.oraclecorp.com/confluence/display/Workflow/Workflow+FAQ#WorkflowFAQ-WhatistheendpointofWFaaS) to understand how to set up theWFaaS endpoint.
   ```java
   String endpoint = "https://wfaas-overlay." + ad + ".us-ashburn-1.oracleiaas.com";
   var endpointConfig = WorkflowEndpointConfiguration.builder()
                .workflowServerEndpoint(endpoint)
                .connectTimeoutMillis(20_000)
                .workerReadTimeoutMillis(20_000)
                .pollerReadTimeoutMillis(POLLER_READ_TIMEOUT_MILLIS)
                .build();
   ```
   *Note:* Use `http://localhost:39000` as the Workflow server endpoint when running against an in-memory Workflow Server used for testing.
5. Below is an example of putting all the above steps together to programmatically build a Workflow client: 
   ```java
   return ChastWorkflowClient.builder()
                .clientRole(clientRole)
                .workerIdentifier(workerIdentifier)
                .domainId(domainId)
                .clientConfigurator(httpClientBuilder -> { })
                .endpointConfig(endpointConfig)
                .authDetailsConfig(authDetailsConfig)
                .build();
   ``` 
6. To avoid getting `javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target` error when running the application, add `-Djavax.net.ssl.trustStore=/etc/pki/java/cacerts` option in the java command line.
   ```java
   java -Djavax.net.ssl.trustStore=/etc/pki/java/cacerts -jar app/reference-service.jar 
   ```

---

## Configuration

If the named worker and poller clients are injected, you can set their configuration using the `microprofile-config.properties`. Alternatively, you can also set them in another ConfigSource like `application.yaml`.

| COnfiguration key                                        | Default Value          | Description                                                                                                                                                                                                                                           |
|----------------------------------------------------------|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| oci.workflow.domain-id                                   | localhost              | The domain id of your WFaaS instance. Check [How do I find my domainID?](https://confluence.oraclecorp.com/confluence/display/Workflow/Workflow+FAQ#WorkflowFAQ-WhatistheendpointofWFaaS) for more details.                                           |
| oci.workflow.endpoint-details.server-endpoint            | http://localhost:39000 | The Workflow server endpoint. Check [What is the endpoint of WFaaS?](https://confluence.oraclecorp.com/confluence/display/Workflow/Workflow+FAQ#WorkflowFAQ-WhatistheendpointofWFaaS) for more details.                                               |
| oci.workflow.endpoint-details.connection-timeout-millis  | 30000                  | The socket connection timeout in milliseconds.                                                                                                                                                                                                        |
| oci.workflow.endpoint-details.worker-read-timeout-millis | 30000                  | The worker client socket read timeout in millisecond.                                                                                                                                                                                                 |
| oci.workflow.endpoint-details.poller-read-timeout-millis | 30000                  | The poller client socket read timeout in millisecond.                                                                                                                                                                                                 |
| oci.dynamic-ssl-context-provider.root-cert-path          |                        | The root CA (Certificate Authority certificate path for the DynamicSslContextProvider which is used to allows client applications to consume a renewable SSLContext and SSLSocketFactory without bothering with the details of certificate reloading. |

---

## References

* [Workflow as a Service Guide](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/workflow/landing-wfaas.htm)
* [Getting Started with WFaaS](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/workflow/getting-started-with-wfaas.htm)
* [WFaaS Shepherd Provider](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/shepherd/providers/wfaas-provider.htm)
* [Provision with Shepherd WFaaS Provider](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/workflow/workflow-self-service-provisioning.htm#step-2-provision-with-shepherd-wfaas-provider)

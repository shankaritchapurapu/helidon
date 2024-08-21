# Secret Service V2

---

## Contents

* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)

---

## Overview

The Secret Service V2 (SSv2) integration provides the following features:
1. Secret Service Config Source - SSv2 secrets retrieval over MicroProfile Config
2. Secret Service TLS Manager - Server and Client mTls rotation

---

## Maven Coordinates

To enable Secret Service V2 (SSv2) add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-secret-service</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Usage

### Secret Service Config Source

SSv2 config source maps MicroProfile configuration properties to SSv2 secret paths.

Properties with default prefix `oci.ssv2` are being resolved as following:
* `oci.ssv2/secret/helidon/test-secret/latest` is being resolved from SSv2 as `/secret/helidon/test-secret/latest`

```java
@Inject
@ConfigProperty(name = "oci.ssv2/secret/helidon/test-secret/latest")
Supplier<String> testSecret;
```

Optional config source configuration in `mp-meta-config.yaml`:
```yaml
sources:
  - type: 'oci-secret-service'
    # Optional config
    prefix: oci.ssv2
    # <<region>> placeholder is automatically resolved 
    endpoint: "https://secret-service-ce.<<region>>.oracleiaas.com/v1"
    tlsConfig.caBundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
    cacheConfig:
      cacheType: IN_MEMORY_CACHE
      cacheExpiryInSeconds: 10
      cacheRefreshIntervalInSeconds: 2
    retryConfig:
      maxRetries: 3
      minRetryDelayInMs: 100
```
### Secret Service TLS Manager

TLS manager `oci-ssv2` is capable of mTLS rotation with keys and certificates produced by PKI service and stored in SSv2 in JSON format.

PKI material can be loaded by one of the available options exclusively:
* From SSv2 by providing SSv2 path over `pki.secret` property.
* By loading the file with mTls material in JSON format from file system - `pki.resource.path` or classpath - `pki.resource-path`.

Trust CA bundle can be loaded from file system - `trust.resource.path` or classpath - `trust.resource-path`.

### Server mTLS rotation

Helidon server side mTls can be configured to use our `oci-ssv2` Tls manager. The example below demonstrates rotation every 30 minutes of certificates delivered by PKI to an SSV2 secret:
```yaml
server:
  port: 8080
  host: localhost
  tls:
    endpoint-identification-algorithm: "NONE"
    client-auth: "REQUIRED"
    manager:
      oci-ssv2:
        # Download mTls context every 30 seconds (default is 30 mins)
        reload.cron: "0/30 * * * * ? *"
        # Fetch PKI mTls material from SSv2
        pki.secret: /secret/xxx/test-mtls-server/latest
        # Use trust CA bundle from local filesystem
        trust.path: /etc/oci-pki/ca-bundle.pem
```

### Client mTLS rotation

Following clients can use `oci-ssv2` Tls manager in Helidon:

* [MicroProfile REST Client](https://helidon.io/docs/v4/mp/restclient)
* [JAX-RS Client](https://helidon.io/docs/v4/mp/jaxrs/jaxrs-client)
* [Helidon WebClient](https://helidon.io/docs/v4/mp/jaxrs/jaxrs-client)
* Any client able to use `javax.net.ssl.SSLContext` produced by `oci-ssv2` Tls manager

Consider following example:
```yaml
# Client configuration
acme-client:
  tls:
    manager:
      oci-ssv2:
        # Download mTls context every 30 seconds (default is 30 mins)
        reload.cron: "0/30 * * * * ? *"
        # Fetch PKI mTls material from SSv2
        pki.secret: /secret/xxx/acme-mtls-client/latest
        # Use trust CA bundle from local filesystem
        trust.path: /etc/oci-pki/ca-bundle.pem
        
# Another client configuration
another-client:
  tls:
    manager:
      oci-ssv2:
        reload.cron: "0/50 * * * * ? *"
        pki.secret: /secret/xxx/other-mtls-custom-client/latest
        trust.path: /etc/oci-pki/ca-bundle.pem
```

#### JAX-RS clients

  ```java
  Config acmeTlsConfig = GlobalConfig.config().get("acme-client.tls");
  ClientBuilder.newBuilder()
              .sslContext(Tls.create(acmeTlsConfig).sslContext())
              .build()
              .target(uri);
  ```

#### Helidon WebClient

  ```java
  WebClient.builder()
           .baseUri(uri)
           .config(GlobalConfig.config().get("acme-client"))
           .build();
  ```

#### MicroProfile Rest Client

  ```java
  RestClientBuilder.newBuilder()
           .baseUri(uri)
           .sslContext(Tls.create(GlobalConfig.config().get("acme-client.tls")).sslContext())
           .build(GreetRestClient.class);
  ```

#### Other HTTP clients

Any Java HTTP Client can use `javax.net.ssl.SSLContext` created by `oci-ssv2` Tls manager.

```java
  Config custTlsConfig = GlobalConfig.config().get("client.tls");
  javax.net.ssl.SSLContext sslContext = Tls.create(custTlsConfig).sslContext();
```

---

## Configuration

| Key                          | Example Value                             | Default value                     | Description                                        |
|------------------------------|-------------------------------------------|-----------------------------------|----------------------------------------------------|
| `reload.cron`                | `0/30 * * * * ? *`                        | `*/30 * * * * ? *` - every 30 min | Cron expression for reload interval configuration. |
| `reload.enabled`             | `false`                                   | `true`                            | When `false`, only initial load happens.           |
| `pki.prefix`                 | `custom-prefix`                           | `oci.ssv2`                        | SSv2 config source prefix.                         |
| `pki.secret`                 | `/secret/helidon/test-mtls-server/latest` |                                   | SSv2 secret path to load PKI JSON material.        |
| `pki.resource.path`          | `/opt/mtls/client-pki.json`               |                                   | File path to load PKI JSON material from file.     |
| `pki.resource.resource-path` | `mtls/client-pki.json`                    |                                   | Class path to load PKI JSON material from file.    |
| `pki.password`               | `password123`                             | `password`                        | Password used in case private key is encrypted.    |
| `trust.path`                 | `/etc/oci-pki/ca-bundle.pem`              | `/etc/oci-pki/ca-bundle.pem`      | File path to load rust CA bundle.                  |
| `trust.resource-path`        | `/etc/oci-pki/ca-bundle.pem`              |                                   | Class path to load rust CA bundle.                 |

---

## References

* [Secret Service V2](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/secret/landing-secrets.htm)

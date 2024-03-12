# Secret Service integration

## Secret Service ConfigSource
SSv2 config source maps MicroProfile configuration properties to SSv2 secret paths.

Properties with default prefix `oci.ssv2` are being resolved as following:
* `oci.ssv2.secret.helidon.test-secret.latest` is being resolved from SSv2 as `/secret/helidon/test-secret/latest`

```java
@Inject
@ConfigProperty(name = "oci.ssv2.secret.helidon.test-secret.latest")
Supplier<String> testSecret;
```

Optional config source configuration in `mp-meta-config.yaml`:
```yaml
sources:
  - type: 'environment-variables'
  - type: 'system-properties'
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
    authProvider:
      timeout: 500
      retries: 8
```
## mTLS rotation
Scheduled mTLS rotation with keys and certificates produced by PKI service and stored in SSv2.  

### Server
TBD

## Client
TBD

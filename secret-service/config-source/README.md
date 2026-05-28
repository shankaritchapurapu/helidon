# Helidon OCI Secret Service Config Source

## Overview

`helidon-oci-secret-service-config-source` provides a Helidon SE config source backed by Secret Service V2 (SSv2).

It is intended for secrets that should:

- be resolved lazily from SSv2
- use a configurable config-key prefix, defaulting to `oci.ssv2`
- map the suffix after the prefix directly to the SSv2 secret path
- detect external secret updates by polling
- expose updated values to Helidon config change listeners
- keep a configurable source-level cache TTL

For the Secret Service feature background, see [Secret Service](../../docs/services/secret-service.md).

## Maven Coordinates

```xml
<dependency>
    <groupId>com.oracle.helidon.oci.secret-service</groupId>
    <artifactId>helidon-oci-secret-service-config-source</artifactId>
</dependency>
```

## Key Mapping

- default prefix: `oci.ssv2`
- key format: `<prefix>/<ssv2-path>`
- example config key: `oci.ssv2/secret/app/password`
- resulting SSv2 path: `/secret/app/password`

Keys outside the configured prefix are ignored by this source.

## Configuration

Primary declarative configuration uses `oci-config.yaml` under
`helidon.oci-secret-service` when the application uses Helidon's default declarative
bootstrap and no explicit `meta-config.*` file is present.

Example:

```yaml
helidon:
  oci-env:
    prefix: "oci.env"
    location-override:
      region: "sol-mars-1"
      availability-domain: "sol-mars-1-ad-1"
      fault-domain: 5

  oci-secret-service:
    prefix: "oci.ssv2"
    cache-ttl: "PT5M"
    poll-interval: "PT30M"
    client:
      endpoint: "https://secret-service-ce.${oci.env.iaas-domain-name}/v1"
      tls-config:
        ca-bundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
```

Supported Helidon meta-config type:

- `oci-secret-service`

The `oci-env` source shown in the example below is a companion env-config source, not an
SSv2 source type. It is required in explicit `meta-config.*` when SSv2 endpoint settings
use `${oci.env.*}` placeholders.

Supported source properties:

- `prefix`
- `cache-ttl`
- `poll-interval`
- `client` subtree for SSv2 client settings

If `cache-ttl` is omitted, the direct-read secret-value cache defaults to `PT5M`.
If `poll-interval` is omitted, background polling defaults to `PT30M`.
`cache-ttl` does not change the default polling cadence.

Client settings may be provided either:

- under `client`
- directly next to the source settings

If both styles are used in the same source definition, they are merged. Values under `client` override duplicate root-level SSv2 client settings. In all cases the source strips its own settings before building the SSv2 client config.

Explicit `meta-config.yaml` example:

```yaml
sources:
  - type: "oci-env"
  - type: "oci-secret-service"
    properties:
      prefix: "oci.ssv2"
      cache-ttl: "PT5M"
      poll-interval: "PT30M"
      client:
        endpoint: "https://secret-service-ce.${oci.env.iaas-domain-name}/v1"
        tls-config:
          ca-bundle: "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
        retry-config:
          max-retries: 3
```

`client.retry` is also accepted as an alias for `client.retry-config`. Aliases are provided for user convenience,
either to align with native OCI parameter names or with similar settings in other Helidon OCI modules. Specify at
most one name for an aliased setting, not both.

The default endpoint is `https://secret-service-ce.${oci.env.iaas-domain-name}/v1`.
`helidon-oci-secret-service-config-source` brings `helidon-oci-envconfig` at runtime,
and `oci-env` publishes `oci.env.iaas-domain-name` from the resolved runtime region and
core-regions domain metadata. For example, `us-phoenix-1` resolves to
`r2.oracleiaas.com`, and `us-seattle-1` resolves to `r1.oracleiaas.com`.

When an application uses explicit `meta-config.*`, list `type: "oci-env"` before
`type: "oci-secret-service"` as shown above. Explicit meta-config disables the automatic
`oci-env` source registration path, so omitting it leaves `${oci.env.iaas-domain-name}`
unresolved unless `client.endpoint` is configured to a concrete URL. See
[Environment Configuration](../../docs/utilities/environment-config.md) for the full region,
domain, and bootstrap behavior.

The same SSv2 keys are used on both configuration paths. `oci-config.yaml` uses
`helidon.oci-secret-service`; explicit `meta-config.*` uses `sources[].properties`.
When both inputs are present for an explicit `oci-secret-service` source,
`sources[].properties` overrides `helidon.oci-secret-service` from `oci-config.yaml` per key.
Values from `oci-config.yaml` fill only keys missing from provider properties.

## Runtime Model

Each requested key is tracked independently.

On direct lookup:

- if the tracked value is still inside `cache-ttl`, the cached value is returned
- otherwise the value is fetched from SSv2 and the cache entry is refreshed

On background polling:

- only tracked keys are polled
- each tracked key is fetched fresh regardless of its current cache TTL
- if any tracked key changes, appears, or disappears, the source emits a root snapshot event containing the latest tracked state
- if a previously unreadable tracked key becomes readable later, the recovered value is also emitted as a change

This gives two useful properties:

- direct reads stay lazy and cacheable
- external updates can still reach Helidon listeners without waiting for another on-demand read

## Change Notifications

When polling detects a change, the source emits:

- key: `""`
- node: rebuilt root snapshot of all tracked keys

Root snapshot publication is serialized internally. Poll-driven refreshes and on-demand lazy refreshes may both request publication, but listeners observe them through a single ordered publication lane.

Using a root snapshot is intentional:

- value changes are straightforward
- deletions are represented correctly
- Helidon can replace the source state atomically for the tracked subset

Because the source is lazy, that root snapshot contains only keys that were already requested at least once through this source. It does not enumerate all keys under the configured prefix in SSv2.

When Helidon change listeners are active, the first successful read of a newly tracked key may also publish an immediate root snapshot even if SSv2 itself did not change. This is intentional. It seeds Helidon's internal source state with the tracked key so a later SSv2 deletion can be emitted as a proper removal diff instead of being lost as `missing -> missing`.

When Helidon change listeners are active, an on-demand read that refreshes an already tracked key to a new value may also publish an immediate root snapshot instead of waiting for the next poll cycle. This keeps change notifications aligned with the source's latest tracked state even if the refreshed value is first observed by a direct lazy read.

## Why This Is Not a `PollableSource`

Helidon `PollableSource` is built around whole-source stamps and reloads. That model is a poor fit for an SSv2-backed lazy source because:

- keys are not enumerable up front for this use case
- only actually requested keys should be tracked and refreshed
- value deletion is easier to represent by replacing the whole tracked snapshot than by emitting per-key partial updates

The source therefore uses:

- `LazyConfigSource` for on-demand reads
- `EventConfigSource` for change notifications
- `NodeConfigSource` for a tracked-key snapshot

## Client Caching

The config source uses an internal SSv2 vault client backed by OCI SDK request
signing, retry support, and the generated SSv2 response model. It does not use
the higher-level cached SDK wrapper.

As a result, the only effective secret-value cache for this feature is the config source's own `cache-ttl`.

## Limitations

- only keys that have already been requested are polled
- `load()` and change-event root snapshots include only keys that have already been requested
- the source does not enumerate SSv2 subtrees
- secret values are exposed as UTF-8 strings
- if polling is never started by Helidon change listeners, direct reads still refresh after `cache-ttl`, but there is no background update push

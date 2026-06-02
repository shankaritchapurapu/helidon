# Tagging

---

## Overview

The Tagging integration registers the OCI internal
`com.oracle.pic.tagging.client.entities.TaggingClient` with the Helidon service registry. The
client converts between:

* a `TagSet` composed of `freeformTags`, `definedTags`, and optional `systemTags`
* the binary tag slug persisted with the resource or passed to downstream authorization flows

A runnable Helidon example is available under [`examples/tagging`](../../examples/tagging/).

---

## Maven Coordinates

To enable tagging support, add the following dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-tagging</artifactId>
</dependency>
```

---

## Usage

The tagging module registers the internal tagging client with the Helidon service registry. You can obtain a
`TaggingClient` instance using the `Services` class:

```java
import java.util.Map;

import io.helidon.service.registry.Services;

import com.oracle.pic.tagging.client.entities.TaggingClient;
import com.oracle.pic.tagging.client.tag.TagSet;
import com.oracle.pic.tagging.common.tagset.tagslice.DefinedTags;
import com.oracle.pic.tagging.common.tagset.tagslice.FreeformTags;
import com.oracle.pic.tagging.common.tagset.tagslice.SystemTags;

TaggingClient client = Services.get(TaggingClient.class);
TagSet tagSet = TagSet.builder()
        .freeformTags(FreeformTags.builder()
                               .tags(Map.of("owner", "platform"))
                               .build())
        .definedTags(DefinedTags.builder()
                              .tags(Map.of("Operations",
                                           Map.<String, Object>of("CostCenter", "42")))
                              .build())
        .systemTags(SystemTags.builder()
                             .tags(Map.of("orcl-cloud",
                                          Map.<String, Object>of("free-tier-retained", "true")))
                             .build())
        .build();

byte[] slug = client.toByteArray(tagSet);
TagSet decoded = client.extractTagSet(slug);
```

The primary write-path pattern is:

* build a `TagSet` from request `freeformTags` and `definedTags`
* convert the `TagSet` to a binary slug before persisting or forwarding the request

The corresponding read-path pattern is:

* load the persisted tag slug
* extract a `TagSet`
* read the maps back using `getFreeformTags()`, `getDefinedTags()`, and `getSystemTags()`

For write paths that persist or update tags, the tag slug should be validated through Authorization Service before it
is stored with the resource. The tagging client creates the local slug; the Auth SDK validates it and can return the
slug that should be stored.

---

## Configuration

Optional client settings live under `oci.tagging`:

The tagging integration has a small configuration surface because it creates a local tag slug client.
It does not call the Tagging service directly and therefore does not configure an endpoint, region,
authentication provider, retry policy, or TLS settings. Those settings belong to the Identity and
Auth SDK integrations used by applications that validate or authorize tag slugs.

| Key | Default Value | Description |
|-----|---------------|-------------|
| `oci.tagging.emit-metrics` | `true` | Whether the internal tagging client should emit metrics. |

Example configuration to disable metrics in `application.yaml`:

```yaml
oci:
  tagging:
    emit-metrics: false
```

---

## Example

Build and run the sample from [`examples/tagging`](../../examples/tagging/README.md):

```shell
mvn -pl examples/tagging -am package
java -jar examples/tagging/target/helidon-oci-examples-tagging.jar
```

The sample exposes:

* `POST /tagging/tag-definitions`
* `POST /tagging/resources`

The example depends on `helidon-oci-tagging` and demonstrates converting tagged resources to tag slugs and back again.
It also demonstrates method-level `@AuthorizationPermission`, direct Auth SDK `AuthorizationRequest` injection, and OCI SDK
Identity tag definition calls. The application starts through the Helidon service registry, so the endpoint receives the
tagging and identity clients from their modules rather than constructing them directly.

---

## References

* [Tagging Client Release Information](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/tagging/tagging-onboarding/tagging-client-release-information.htm)
* [Tagging Onboarding](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/tagging/tagging-onboarding.htm)
* [Oracle Cloud Infrastructure Tagging](https://docs.oracle.com/en-us/iaas/Content/Tagging/home.htm)

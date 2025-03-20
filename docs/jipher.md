# Jipher

---

## Contents


* [Overview](#overview)
* [Maven Coordinates](#maven-coordinates)
* [Usage](#usage)
* [Configuration](#configuration)
* [References](#references)

---
## Overview
This document aims to provide guidance on how to integrate [Jipher](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher) with Helidon utilizing the Pegasus [com.oracle.pic.commons:core](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyaqldtoy4kbrhjcwazanam6ea2qjcbxyntwm5dn2hnzulq?_ctx=us-phoenix-1%2Cdevops_scm_central) library. Jipher is a JCE Security Provider developed by Oracle used for achieving FIPS (Federal Information Processing Standards) compliance.

## Maven Coordinates
Integration with  [Splat](./splat.md), [Secret Service](./secret-service.md) and 
[Identity](./identity.md) already adds the [com.oracle.pic.commons:core](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyaqldtoy4kbrhjcwazanam6ea2qjcbxyntwm5dn2hnzulq?_ctx=us-phoenix-1%2Cdevops_scm_central) dependency that provides Jipher Support. If neither of these integrations are used in your project, you can directly include the dependency to your project’s pom.xml:

```xml
<dependency>
    <groupId>com.oracle.pic.commons</groupId>
    <artifactId>core</artifactId>
</dependency>
```

## Usage
Jipher JCE can be registered using `dynamic registration` or `static registration`. Please refer to the [Jipher JCE Usage Guide](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher+JCE+Usage+Guide?src=contextnavpagetreemode) for more details. `Dynamic registration` is the method that will be pursued in this guide, as that is Jipher's more preferred approach. It is important to note that Jipher registration must be placed at a very early stage of the application's runtime execution or otherwise, unpredictable errors or behavior will be encountered.

1. Code changes
   1. Create a static block in the main class of a Helidon Application to ensure that Jipher registration happens before any subsystem component is executed, especially those related to encryption and cryptography.
      ```java
      ...
      ...
      public class ReferenceServiceMain {

          static {
               // Place Jiphe JCE provider registration code here
          }
          ...
          ...
      }
    
      ```
   2. The next few steps should be added inside the created static block.   
   3. Set `useJipherJceProvider` system property to `true` to enable Jipher FIPS provider.
      ```java
      System.setProperty("useJipherJceProvider", "true");
      ```
   4. Set `useBcFipsProvider` system property to `false` to disable Bouncy Castle FIPS provider.
      ```java
      System.setProperty("useBcFipsProvider", "false");
      ```
   5. If your application uses a JKS formatted truststore, such as the default Java truststore, set the `javax.net.ssl.trustStoreType` system property to `jks`. Otherwise, ignore this step if the truststore used by the application is in PKCS12 format or if a truststore is not used at all.
      ```java
      System.setProperty("javax.net.ssl.trustStoreType", "jks");
      ```
   6. Load the preferred provider, which in this case will put Jipher as the primary security provider. 
      ```java
      JCEProviders.load();
      ```
   7. Source code example:
       ```java
       ...
       ...
       public class ReferenceServiceMain {
           static {
               // This system property setting directs the JCEProviders class to register JipherJCE as the
               // highest priority security provider.
               System.setProperty("useJipherJceProvider", "true");
    
               // Disables BC Fips Provider which is enabled by default
               System.setProperty("useBcFipsProvider", "false");
     
               // For Java 9 or later, if your application uses a JKS formatted truststore, such as the default
               // Java truststore, set the javax.net.ssl.trustStoreType system property to jks. See the Jipher
               // Troubleshooting Guide for more details. Otherwise, remove this line if the truststore used by
               // the application is in PKCS12 format or if truststore is not used at all.
               // System.setProperty("javax.net.ssl.trustStoreType", "jks");
    
               // Executes the process of registering Jipher as the primary security provider and disables BC Fips.
               JCEProviders.load();
           }
           ...
           ...
       }
       ```
2. Java command line changes
   1. Optionally add `java.security.debug` with a value of `"provider,jipher"` if more debug logging is needed, for example when encountering issues with using Jipher. It is most advisable to not use this option in production as this will yield a quite substantial amount of output. Also check out [Jipher Debugging Options](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher+Troubleshooting#JipherTroubleshooting-Debuggingoptions) for more details.
      ```shell
      -Djava.security.debug="provider,jipher"
      ```
   2. It is mandatory to add the `--add-exports` directive with a value  of `ALL-UNNAMED,jipher.jce`  as described in the [Jipher JCE with SunJSSE](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher+JCE+with+SunJSSE#JipherJCEwithSunJSSE-Java9andlater) document.
      ```shell
      --add-exports=java.base/sun.security.internal.spec=ALL-UNNAMED,jipher.jce
      ```
   3. Command line example:
      ```shell
      java -Djava.security.debug="provider,jipher" --add-exports=java.base/sun.security.internal.spec=ALL-UNNAMED,jipher.jce -jar reference-service.jar

      ```

## References
* [What is Jipher?](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher)
* [Jipher JCE Usage Guide](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher+JCE+Usage+Guide)
* [Jipher Release Notes](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher+Release+Notes)
* [Achieving FIPS 140 Compliance with Jipher](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Achieving+FIPS+140+Compliance+with+Jipher)
* [Jipher Troubleshooting](https://confluence.oraclecorp.com/confluence/display/OCICRYPTO/Jipher+Troubleshooting#JipherTroubleshooting-java.security.debug)

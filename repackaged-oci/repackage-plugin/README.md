# Overview

Helidon OCI repackage plugin is able to:
* Repackage whole artefact contents, excluding files according to supplied regex
* Move classes and their references to different packages
* Augment classes, methods and constructors:
  * Rename methods
  * Change method name, parameters, return type and body
  * Substitute method calls from method or constructor   

```xml
<plugin>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-oci-repackage-plugin</artifactId>
    <version>1.1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>repackage-oci-libs</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- Repackage/shade whole artefact content -->
        <artifacts>
            <artifact>
                <groupId>com.oracle.pic.identity.authorization</groupId>
                <artifactId>sdk</artifactId>
                <exclusions>
                    <!-- Drop original swagger generated content -->
                    <exclusion>com/oracle/pic/identity/authorization/[^/]*\.class</exclusion>
                    <exclusion>com/oracle/pic/identity/authorization/model/[^/]*\.class</exclusion>
                    <exclusion>com/oracle/pic/identity/authorization/requests/[^/]*\.class</exclusion>
                    <exclusion>com/oracle/pic/identity/authorization/responses/[^/]*\.class</exclusion>
                </exclusions>
            </artifact>
        </artifacts>
        <classes>
            <!-- Move classes and their references to different packages -->
            <class>
                <name>ClassA</name>
                <pkg>com.from.package</pkg>
                <target-pkg>com.to.package</target-pkg>
            </class>
            <class>
                <name>ClassB</name>
                <pkg>com.from.package</pkg>
                <target-pkg>com.to.package</target-pkg>
            </class>
            <!-- Augment classes -->
            <class>
                <name>JsonMappingInitializer</name>
                <pkg>com.example</pkg>
                <changes>
                    <!-- Replace method call "A.getObjectMapper()" in  com.example.JsonMappingInitializer#initJsonMappingMethodName(String,int,Optional) -->
                    <change>
                        <name>initJsonMappingMethodName</name>
                        <params>
                            <param>java.lang.String</param>
                            <param>int</param>
                            <param>java.util.Optional</param>
                        </params>
                        <substitutions>
                            <substitution>
                                <methodCall>com.example.A#getObjectMapper</methodCall>
                                <replacement>
                                    $_ = com.example.B.getDefaultObjectMapper();
                                </replacement>
                            </substitution>
                        </substitutions>
                    </change>
                </changes>
            </class>
            <!-- Change method return type -->
            <class>
                <name>SwaggerClientUtil</name>
                <pkg>com.oracle.pic.identity.authentication.swagger.utils</pkg>
                <changes>
                    <change>
                        <name>getInstanceOfCircuitBreaker</name>
                        <newReturnType>com.oracle.bmc.circuitbreaker.OciCircuitBreaker</newReturnType>
                    </change>
                </changes>
            </class>
            <class>
                <name>SwaggerClientConfigurator</name>
                <pkg>com.oracle.pic.identity.authentication</pkg>
                <!-- Change parent class -->
                <newParentClassName>com.oracle.bmc.http.client.jersey3.apacheconfigurator.ApacheConfigurator</newParentClassName>
                <changes>
                    <change>
                        <!-- When original method name is not specified, constructor is changed -->
                        <name>customizeBuilder</name>
                        <!-- Rename method -->
                        <newName>customizeClient</newName>
                        <!-- Change param types -->
                        <newParams>
                            <param>com.oracle.bmc.http.client.HttpClientBuilder</param>
                        </newParams>
                        <!-- Change method body -->
                        <newBody>{
                            super.customizeClient($1);
                            dynamicSslContextProvider.initialize(dynamicSslContextProviderConfig);
                                try {
                                    $1.property(com.oracle.bmc.http.client.StandardClientProperties.SSL_CONTEXT, dynamicSslContextProvider.getSslContext());
                                    if(proxyURL.isPresent()) {
                                        $1.property(com.oracle.bmc.http.client.ClientProperty.create("jersey.config.proxyURL.get());
                                    }
                                } catch (java.lang.Exception ex) {
                                    throw new java.lang.RuntimeException("Failed generating client configurator", ex);
                                }
                            }
                        </newBody>
                    </change>
                </changes>
            </class>
        </classes>
    </configuration>
</plugin>
```
Moving classes:
```shell
com                         ->     com                       
└── from                    ->     └── to                  
    └── pkg                 ->         └── pkg               
        ├── ClassA.java     ->             ├── ClassA.java   
        ├── ClassB.java     ->             ├── ClassB.java   
```
And augments bytecode:
```diff
-package com.from.pkg;
+package com.to.pkg;

public class ClassA {
-    private final com.from.pkg.ClassB classB = new ClassB();
+    private final com.to.pkg.ClassB classB = new ClassB();
}
```

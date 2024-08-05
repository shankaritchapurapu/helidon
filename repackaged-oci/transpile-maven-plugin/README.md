# Overview

True repackaging, class is moved to different package, it's bytecode manipulated so its package declaration
and all references for all the repackaged classes inside aligns with new packages.

```xml
<plugin>
    <groupId>com.oracle.helidon.oci</groupId>
    <artifactId>helidon-transpile-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>transpile-classes</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <mapping>
            <map>com.from.package.ClassA:com.to.package.ClassA</map>
            <map>com.from.package.ClassB:com.to.package.ClassB</map>
            <map>com.from.package.ClassC:com.to.package.ClassC</map>
        </mapping>
    </configuration>
</plugin>
```

Moves classes:

```shell
com                         ->     com                       
└── from                    ->     └── to                  
    └── pkg                 ->         └── pkg               
        ├── ClassA.java     ->             ├── ClassA.java   
        ├── ClassB.java     ->             ├── ClassB.java   
        └── ClassC.java     ->             └── ClassC.java    
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
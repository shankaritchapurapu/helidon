# Dependencies Tests

## Overview
Ensures the integrity of the dependencies from depeendency management.

## Usage
From the top level of the project:
```agsl
cat dependencies/pom.xml | etc/scripts/depm2dep.sh
```
Then update [pom.xml](pom.xml) with the output.

## Build

```shell
mvn clean package
```
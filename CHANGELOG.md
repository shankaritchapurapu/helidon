
# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0]

This release contains minor bug fixes, Helidon 4.1.7 uptake and added support for OCI native Identity.  A minimum of Java 21 is required to use this release.

Please see [the documentation](docs/README.md) for more information.

### CHANGES

- [Identity/Authorization integration](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/commits/cfe85c60e83bd92a1740875aacacc7fa6f547cd9?revision=%22refs%2Fheads%2Fmain&_ctx=us-phoenix-1%2Cdevops_scm_central)

- [Reduce INFO level log on every request to avoid performance degradation on default setup](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyaabsnhm2voip7qm5nszorgkqybamnxqc44m2w5xcshkyq?_ctx=us-phoenix-1%2Cdevops_scm_central)

- [Fix various issues for Secret Service and Splat modules to work with OMK](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyaggifcrezdadd57rdfh3wwl75rgn7sua6ukfsskq4hinq?_ctx=us-phoenix-1%2Cdevops_scm_central)

- [Fix bad Splat and OCI Java SDK dependency](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyapd6km6ab3ff3inyqlkd4bt2jufzapdy6r5qv5gltb2ia?_ctx=us-phoenix-1%2Cdevops_scm_central)

- [Uptake Helidon 4.1.7 that includes changes to OKE Workload Identity authentication configuration needed for OMK setup](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyaroifymcnfr5valzjgbcq5bdglq6omtjb5m5bdiylku3q?_ctx=us-phoenix-1%2Cdevops_scm_central)

## [1.0.0]

This is the initial release of Helidon OCI.  A minimum of Java 21 is required to use this release.

Please see [the documentation](docs/README.md) for more information.

[1.1.0]: https://github.com/oracle/helidon/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/oracle/helidon/compare/main...1.0.0

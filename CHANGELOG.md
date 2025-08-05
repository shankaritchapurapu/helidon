
# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0]
This release contains bug fixes, dependency updates and Helidon 4.2.4 uptake. A minimum of Java 21 is required to use this release.

### CHANGES

- [Uptake Helidon 4.2.4 that includes OCI SDK version upgrade](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyarqisxqvdxd76uxmxikp34nym6za7arxupccxgxawokea?_ctx=us-phoenix-1%2Cdevops_scm_central)
- [Upgrade to Safe Tagging Client](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyaugmgekhqrotd4saw3adoqgdr2vxblhglgcjrh74thdwa?_ctx=us-phoenix-1%2Cdevops_scm_central)
- [Fix issue in Identity module where YAML paths config doesn't work as documented](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyahiknna2anvwxzszckhv5ioj26zjrnvpyj6gjkbfice4q?_ctx=us-phoenix-1%2Cdevops_scm_central)
- [Fix issue in Identity module where NoSuchMethodError on SwaggerClientUtil.getInstanceOfCircuitBreaker() is encountered](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbya7rqjutsy7gjuvkzwq6mlel7wlbgny7odrl2itzo66o2q?_ctx=us-phoenix-1%2Cdevops_scm_central)
- [Fix various issues when OCI SDK version is upgraded](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyajvlngwy2jxwdv4j5ry53vjyzjsbegbkrucbiodrsouxq?_ctx=us-phoenix-1%2Cdevops_scm_central)
- [Provide Guide for Jipher integration](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbyaipd6t5qlbtl5r2gpcn67j4gzhweyltrm5c7lveyoxwba?_ctx=us-phoenix-1%2Cdevops_scm_central)
- [Upgrade various dependencies](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/pull-requests-tabs/ocid1.devopspullrequest.oc1.phx.amaaaaaaw4vcxbya5ko2yyfyfwdbnxgfeci2jtwcwwnaqra6bxidmnlb66oa?_ctx=us-phoenix-1%2Cdevops_scm_central)

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

[1.2.0]: https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/compare?_ctx=us-phoenix-1%2Cdevops_scm_central&baseVersion=refs%2Ftags%2F1.1.0&targetVersion=refs%2Ftags%2F1.2.0
[1.1.0]: https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/compare?_ctx=us-phoenix-1%2Cdevops_scm_central&baseVersion=refs%2Ftags%2F1.0.0&targetVersion=refs%2Ftags%2F1.1.0
[1.0.0]: https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyabdhc33vnzsjrwdgj6mfsomdjlkhsx5h27akhnuiasvka/compare?_ctx=us-phoenix-1%2Cdevops_scm_central&baseVersion=refs%2Ftags%2F0.2.0&targetVersion=refs%2Ftags%2F1.0.0

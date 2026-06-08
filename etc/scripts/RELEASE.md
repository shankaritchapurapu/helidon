# Releasing Helidon OCI

## Overview

A release is performed by pushing to a release branch. The release build will publish artifacts and create a source tag. 

## Steps to do a release

```shell
# Set this to the version you are releasing
export VERSION="1.0.0"
```

1. Create local release branch to prepare for the release
    ```shell
    git pull origin main
    git checkout -b release-${VERSION}
    git log # Make sure you have what you think you have
    ```
2. Edit `ocibuild.conf` and see TODO items. You should:
   1. Set `version:` to be the released version. For example `1.0.0` (should be same as what you set the `VERSION` env variable to).
   2. On `bitbucketTag` change the `enabled` flag to `true`
3. Update version in project pom files.
   1. `etc/scripts/release.sh --version=${VERSION} update_version` then build project to make sure version change worked as expected
   2. Update `CHANGELOG` with contents of release
4. Commit changes and push local release branch to remote. This will trigger a release build.
    ```
    git push origin release-${VERSION}
    ```
5. This should start a build on the `release-${VERSION}` branch in our DevopsSCM Repo.
   1. If you see a failure in building due to missing artifact version, then we have to change build to push artifacts in order. Refer to older release version e.g. [2.0.0-RC1](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon/files/ef1f2b9b782a978280544f601b01916e29a69f4a?filePath=ocibuild.conf&refName=2.0.0-RC1&fileName=ocibuild.conf&_ctx=us-phoenix-1%2Cdevops_scm_central&commitId=4f2053a4118dce3f7bab4945cc84266f925c7074)
6. Once the build is successful, verify the release:
   1. Check the repo to make sure release tag was created (should match $VERSION)
   2. Check https://artifactory.oci.oraclecorp.com/helidon-oci-release-maven-local/com/oracle/helidon/oci/ and verify artifacts are there
   3. Try building example application using released bits.


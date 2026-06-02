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
5. Verify release
   1. Check the repo to make sure release tag was created (should match $VERSION)
   2. Check https://artifactory.oci.oraclecorp.com/helidon-oci-release-maven-local/com/oracle/helidon/oci/ and verify artifacts are there
   3. Try building example application using released bits.


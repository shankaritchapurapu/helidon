#
# Copyright (c) 2026 Oracle and/or its affiliates.
#

FROM ocr-docker-remote.artifactory.oci.oraclecorp.com/os/oraclelinux:8-slim
COPY --from=odo-docker-signed-local.artifactory.oci.oraclecorp.com/base-image-support/ol8:1.43 / /

RUN microdnf install yum
RUN yum install gzip && yum clean all

# Install Maven and override default settings.xml meant to be used in build service
RUN set -x && \
    curl -O https://jpg-data.us.oracle.com/artifactory/maven-repo-remote/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz && \
    tar -xvf apache-maven-*-bin.tar.gz  && \
    rm apache-maven-*-bin.tar.gz && \
    mv apache-maven-* maven && \
    ln -s /maven/bin/mvn /bin
COPY build_config/pipeline/build-stage-settings.xml /maven/conf/settings.xml

# Install Java
RUN rpm -ivh  https://jpg-data.us.oracle.com/artifactory/re-release-local/jdk/21.0.4/8/bundles/linux-aarch64/jdk-21.0.4+8_linux-aarch64_bin.rpm

WORKDIR /oci-helidon

COPY . .
RUN mvn -f pom.xml install -DskipTests

# Set Maven configuration to be used in test stage
ENV MAVEN_OPTS="$MAVEN_OPTS -Daether.connector.https.securityMode=insecure"
COPY build_config/pipeline/test-stage-settings.xml /maven/conf/settings.xml

CMD ["mvn", "-f", "tests/integration/pom.xml", "-Pintegration-tests", "verify"]

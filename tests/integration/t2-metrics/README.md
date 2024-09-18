# OCI T2 Metrics Native Integration Test

## Overview

This module contains tests related to integration with internal OCI Telemetry otherwise known as T2.

## Prerequisites

Please see [How to locally test your OCI SDK Integration](../../../README.md) to set up ssh tunneling for the Instance Metadata Service.

## Steps

1. Run the SSH tunnel command to open up a connection to the remote host and forward any connection on local port 8000 to the
   Instance MetaData Service endpoint (169.254.169.254:80) of the remote host. Note that this example uses `ztb-api-ad1` host
   alias that should have been set up in the OSSH config.
   ```shell
   $ ssh -v -L 8000:169.254.169.254:80 oci-reference-service-ad1 -t watch -n 90 date
   ```
2. Run the unit test using `STTest`profile, where `ST` stands for `SSH Tunneling`.
   ```shell
   $ mvn clean install -PSTTest
   ```
3. If the test succeeds, proceed and verify if metrics has been successfully posted
   using [Metrics Explorer](https://devops.oci.oraclecorp.com/telemetry/mql/explore)
   or [Grafana](https://grafana.oci.oraclecorp.com) in the DevOps Console.
    * Details of posted test metrics:
        1. Project: `xxxx`
        2. Fleet:
            1. `t2-api` - metric sent from T2 API
            2. `monitoring-sdk` - metric sent from OCI Monitoring SDK
        3. Metric Name: `t2metric`
        4. Region: `us-ashburn-1`
    * Verifying with Metrics Explorer:
        1. Go to https://devops.oci.oraclecorp.com/telemetry/mql/explore.
        2. Under `Metric Search`, fill up the values of the empty fields using the metric details mentioned above. Under the
           search field, i.e. input box with the magnifying glass, enter the metric name.
        3. If the metric is found, it will appear below the Metric Search.
        4. Click on the listed metric and a new graph will pop up.
        5. Verify that the metric appears on the graph.
    * Verifying with Grafana:
        1. Go https://devops.oci.oraclecorp.com/ and choose `Monitoring`->`Grafana` from the hamburger menu icon on the topmost
           left corner of the window. Alternatively, grafana can also be accessed directly
           from https://grafana.oci.oraclecorp.com.
        2. Click on the `Explore` icon from the left pane.
        3. On the drop-down list found at the top of the Explore window, select the region prefixed with `T2-`. For example, if
           the region being tested is `us-ashburn-1`, then the value that needs to be selected should be `T2-us-ashburn-1`.
        4. Fill up the other fields such as `Project`, `Fleet` and `Metric` while leaving the others as with their default value.
        5. Click `Run Query` button on the top rightmost corner and a graph will pop up.
        6. Verify that the metric appears on the graph.

## References

* [Overlay Telemetry (T2) Basics](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEVCENTRAL&title=Overlay+Telemetry+%28T2%29+Basics)
* [Metrics-Module Upgrade](https://confluence.oci.oraclecorp.com/display/Telemetry/Metrics-Module+Upgrade)
* [Dianoga Whitelist Provider](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEVCENTRAL&title=Dianoga+Whitelist+Provider)
* [metrics-lib (defines metrics logic and performs buffering and emitting)](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-lib/browse)
* [metrics-overlay (contains metric reporter which the metrics-lib calls to finally report the metrics from the overlay services to OCI monitoring)](https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-overlay/browse)
   

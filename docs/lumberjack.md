# Lumberjack

---

## Contents


* [Overview](#overview)
* [Usage](#usage)
* [References](#references)

---

## Overview

Service application can use [Chainsaw](https://confluence.oci.oraclecorp.com/display/LUM/3.+Chainsaw2+Onboarding) as a log collection agent to stream all internal generated logs (operational, audit, and control plane/data plane application logs) from their host or resource to the [Lumberjack](https://confluence.oci.oraclecorp.com/display/LUM/Lumberjack+V2+User%27s+Guide)
logging platform. With Lumberjack, you don't need to change your application code to integrate with that service. However, you need to [onboard](https://confluence.oci.oraclecorp.com/pages/viewpage.action?pageId=102670755), add chainsaw as a sidecar to your application and preferably output structured logs (for example using JSON format) as this provide some [benefits](https://confluence.oci.oraclecorp.com/display/LUM/Old+Structured+Logging#OldStructuredLogging-Whatarethebenefitsofstructuredlogs?).

---

## Usage

It is important to first onboard with Lumberjack using the [ V2 Onboarding steps](https://confluence.oci.oraclecorp.com/display/LUM/V2+Onboarding+steps). Below are examples of some of the items covered in the steps.
* [Shepherd code example](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-reference-infra/browse/shepherd/infrastructure/modules/identity/policy.tf#50-59)  that creates  a policy to provide access to Lumberjack.
* Registration to Lumberjack can be done via Shepherd with this [code example](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-reference-infra/browse/shepherd/infrastructure/modules/lumberjack).
* Chainsaw can be [installed](https://confluence.oci.oraclecorp.com/display/LUM/3.+Chainsaw2+Onboarding#id-3.Chainsaw2Onboarding-installchainsaw2Howtoinstallchainsaw2) and [run](https://confluence.oci.oraclecorp.com/display/LUM/3.+Chainsaw2+Onboarding#id-3.Chainsaw2Onboarding-Runchainsaw2.1) on the application with examples in this [Dockerfile](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-reference-service/browse/Dockerfile) source and [run.sh](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-reference-service/browse/run.sh) script, respectively.
* Sending logs with structured format has some [important benefits](https://confluence.oci.oraclecorp.com/display/LUM/Old+Structured+Logging#OldStructuredLogging-Whatarethebenefitsofstructuredlogs?). This [Helidon logging.properties](https://bitbucket.oci.oraclecorp.com/projects/HLDN/repos/oci-helidon-reference-service/browse/reference-service/src/main/resources/logging.properties) example was set up to create a JSON formatted structured logs that help achieve this requirement.
* In OCI, an **opc-request-id** is used to trace individual HTTP requests from the client to the server and back again. Adding the **opc-request-id** in every request  helps filter related logs in Lumberjack that include it and the [oci-request-id module](./request-id.md) is available to help with this.

Once the application successfully streams its logs to **Lumberjack** via **Chainsaw**, they can be viewed and browsed using DevOps:
1. Access Lumberjack from https://devops.oci.oraclecorp.com/logs.
2. Fill the values for the following fields:
    * Region
    * Ad
    * Tenant Name or OCID
    * Namespace
    * You can also specify keywords on the **Start typing to filter on msg or choose a field to filter...** input box to filter out specific logs.
3. Specify the **Start Time** and **End Time** to further filter logs that occurred during a specific time period.
4. Click **Search** button to display the set of logs.
5. The application logs are streamed in a JSON structured format, so additional fields other than **msg** will be recorded by Lumberjack  and can be added to the display. To do this, find fields that starts with **#** on the left window pane, click on it and a new button labeled **add column** will appear. Click this button if you wish this to be added as a new column in the display . To be specific, you can add the following fields:
    * #level
    * #logger
    * #thread
    * #timestamp

---

## References

* [Lumberjack V2 User's Guide](https://confluence.oci.oraclecorp.com/display/LUM/Lumberjack+V2+User%27s+Guide)
* [Lumberjack V2 and OCI Logging Integration](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/lumberjack/landing-lumberjack.htm?Highlight=lumberjack)

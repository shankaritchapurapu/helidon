# Lumberjack

---

## Contents


* [Overview](#overview)
* [Usage](#usage)
* [References](#references)

---

## Overview

[Lumberjack](https://confluence.oraclecorp.com/confluence/display/OCILUM/Lumberjack+V2+User%27s+Guide) is an availability domain (AD) local service that provides OCI service teams with the ability to stream all their internal generated logs (operational, audit, and data plane access) from their host or resource to a centralized internal logging platform. To ingest internal operational logs for your team and to perform investigations and analysis in real time, integrate with Lumberjack.

With Lumberjack, you don't need to change your application code. OCI service teams can instead use [Chainsaw](https://confluence.oraclecorp.com/confluence/display/OCILUM/Chainsaw+Jump+Page) as the log collection agent to send all their log data to the ingestion pipeline. Logs in Lumberjack are surfaced via the [DevOps](https://devops.oci.oraclecorp.com/logs) portal for OCI teams.

---

## Usage

It is important to first onboard with Lumberjack using the [onboarding steps](https://confluence.oraclecorp.com/confluence/display/OCILUM/LumberjackV2+Onboarding+steps). Below steps showcases, how it was done for a reference Helidon based service.

* [This code example](https://devops.oci.oraclecorp.com/devops-coderepository/repositories/ocid1.devopsrepository.oc1.phx.amaaaaaaw4vcxbyansu366vrho533ijvr54hm744wmhn5fciggojf5vbmblq/files/1217d49a3f6ec3517481ee17bab3dd19c0b2fde5?filePath=shepherd%2Finfrastructure%2Fmodules%2Fidentity%2Fpolicy.tf&refName=refs%2Fheads%2Fmain&fileName=policy.tf&_ctx=us-phoenix-1%2Cdevops_scm_central&commitId=b6fe7896e599735f703b1d311a31980dd5c4da38&highlightLines=L-41%2CL-50)  creates  a policy to provide access to Lumberjack based on Instance Principal.
* Registration to Lumberjack can be done via Shepherd with this [code example](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-infra/files?folderPath=shepherd%2Finfrastructure%2Fmodules%2Flumberjack&refName=refs%2Fheads%2Fmain&_ctx=us-phoenix-1%2Cdevops_scm_central). Lumberjack is an AD local service, so each AD requires registration. 
* Chainsaw can be [installed](https://confluence.oraclecorp.com/confluence/display/OCILUM/3.+Chainsaw2+Onboarding#id-3.Chainsaw2Onboarding-installchainsaw2Howtoinstallchainsaw2) and [run](https://confluence.oraclecorp.com/confluence/display/OCILUM/3.+Chainsaw2+Onboarding#id-3.Chainsaw2Onboarding-Runchainsaw2.1) along with the application as shown in this [Dockerfile](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-service/files/1524ea9bb1d67c566140f8294ca606f201708811?filePath=Dockerfile&refName=refs%2Fheads%2Fmain&fileName=Dockerfile&_ctx=us-phoenix-1%2Cdevops_scm_central&commitId=00d34dc718d34078d69af6b9dda943de4ab70eb7) source and [run.sh](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-service/files/f2d2be0187b125b7941e0e52be4cac3293602b98?filePath=run.sh&refName=refs%2Fheads%2Fmain&fileName=run.sh&_ctx=us-phoenix-1%2Cdevops_scm_central&commitId=f0c69d6c49184bc2cdfebec33ff3de64d97226db) script, respectively.
* Sending logs with structured format has some [important benefits](https://confluence.oraclecorp.com/confluence/pages/viewpage.action?pageId=12772679645#id-4.StructuredLogging(recommended)-Whatarethebenefitsofstructuredlogs?). This [Helidon logging.properties](https://devops.oci.oraclecorp.com/devops-coderepository/namespaces/axuxirvibvvo/projects/HLDN/repositories/oci-helidon-reference-service/files/9b24208add29be7ac57e96654ccd775ef427e6cf?filePath=reference-service%2Fsrc%2Fmain%2Fresources%2Flogging.properties&refName=refs%2Fheads%2Fmain&fileName=logging.properties&_ctx=us-phoenix-1%2Cdevops_scm_central&commitId=ad612c52378279b8670732159e9c223b0d2ec6cf) example was set up to create a JSON formatted structured logs that help achieve this requirement. While the example here is using `io.helidon.logging.jul.HelidonFormatter`, we also provide `io.helidon.logging.jul.HelidonJsonFormatter` that can be used (both with `HelidonConsoleHandler` and with the JUL `ConsoleHandler`) e.g.
 ```
 handlers=io.helidon.logging.jul.HelidonConsoleHandler
 # Use JSON formatter with default format
 io.helidon.logging.jul.HelidonConsoleHandler.formatter=io.helidon.logging.jul.HelidonJsonFormatter
 ```
 You can configure `io.helidon.logging.jul.HelidonJsonFormatter.fields` to construct the JSON object (below is the default if no format or fields specified):
 ```
 ts:%1$tQ,date:%1$tY.%1$tm.%1$td,time:%1$tH:%1$tM:%1$tS.%1tL,level:%4$s,message:%5$s,exception:%6$s,thread:!thread!,logger:%3$s
 ```
* In OCI, an **opc-request-id** is used to trace individual HTTP requests from the client to the server and back again. Adding the **opc-request-id** in every request  helps filter related logs in Lumberjack that include it and the [oci-request-id module](./request-id.md) is available to help with this.

Once the application successfully streams its logs to **Lumberjack** via **Chainsaw**, they can be viewed and browsed using DevOps portal:

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

* [OCI Internal Developer Documentation - Lumberjack and OCI Logging Integration](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/svcintegration/lumberjack/landing-lumberjack.htm?Highlight=lumberjack)
* [Lumberjack User's Guide](https://confluence.oci.oraclecorp.com/display/LUM/Lumberjack+V2+User%27s+Guide)
* [Lumberjack Onboarding Steps](https://confluence.oraclecorp.com/confluence/display/OCILUM/LumberjackV2+Onboarding+steps)
* [Shepherd Lumberjack Provider](https://internal-docs.oraclecorp.com/en-us/iaas/internalcontent/tools/shepherd/providers/lumberjack-provider.htm)

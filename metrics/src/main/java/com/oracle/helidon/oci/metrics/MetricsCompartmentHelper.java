/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.metrics;

import java.util.Map;

import com.oracle.pic.commons.util.Region;

/**
 * Maps region to T2 metrics compartment ID.
 *
 * @see
 * <a href="https://bitbucket.oci.oraclecorp.com/projects/TEL/repos/metrics-overlay/browse/public-telemetry-reporter/src/main/java/com/oracle/pic/telemetry/commons/metrics/TelemetryReporterBuilder.java#169-189">T2 logic</a>
 * @see
 * <a href="https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=DEVCENTRAL&title=Overlay+Telemetry+%2528T2%2529+Basics#OverlayTelemetry(T2)Basics-T2CompartmentIDbyRealm">T2 metrics compartments</a>
 */
final class MetricsCompartmentHelper {

    private MetricsCompartmentHelper() {
        //noop
    }

    private static final Map<String, String> REALM_TO_METRICS_COMPARTMENT = Map.of(
            // "region1" is a convenience 'pseudo-realm" name for this class only.
            "region1", "ocid1.compartment.region1..aaaaaaaasfm6ym4xn4o7bk27vwt6mswiibyqwqxo654hkf4l27whzlhcbvka",
            "oc1", "ocid1.compartment.oc1..aaaaaaaagixeyxsjv643gwx5vf6dkuwmvvf4dlf7k6sobwzbjrtce4lvndwq",
            "oc2", "ocid1.compartment.oc2..aaaaaaaaxdh2pzeumdaqc4oukomeqzqnuwvdgyqg524k536ifbhiicmdt6aa",
            "oc3", "ocid1.compartment.oc3..aaaaaaaapa74z64fu5zpjwntqbmfp6mh7vgeboswqd4wo6prnkgancwpkaoq",
            "oc4", "ocid1.compartment.oc4..aaaaaaaazhf44ssrbzgrz4plybe2q6mzsdyafjb3bwc67jgkimhaldwurpva",
            "oc5", "ocid1.compartment.oc5..aaaaaaaawratuwq6uxcid7nnfe64rk7l5yr4zrnlkgy7ufw733ft2njfsjda");

    private static final String OTHER_COMPARTMENT_FORMAT =
            "ocid1.compartment.%s..aaaaaaaawratuwq6uxcid7nnfe64rk7l5yr4zrnlkgy7ufw733ft2njfsjda";

    static String t2CompartmentIdForRegion(String region) {

        String realm = belongsToRegion1(region) ? "region1" : Region.fromPublicRegionName(region).getRealm().getName();

        return REALM_TO_METRICS_COMPARTMENT.containsKey(realm)
                ? REALM_TO_METRICS_COMPARTMENT.get(realm)
                : String.format(OTHER_COMPARTMENT_FORMAT, realm);
    }

    private static boolean belongsToRegion1(String region) {
        return region.equals("r1") || region.equals("sea") || region.equalsIgnoreCase("us-seattle-1");
    }
}

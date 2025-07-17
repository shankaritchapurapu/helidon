/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
 */

package com.oracle.tests.integration.api;

import io.helidon.microprofile.testing.junit5.HelidonTest;

import com.oracle.tests.integration.model.AttachIScsiVolumeRequest;
import com.oracle.tests.integration.model.AttachVolumeRequest;
import com.oracle.tests.integration.model.IScsiVolumeAttachment;
import com.oracle.tests.integration.model.Instance;
import com.oracle.tests.integration.model.Region;
import com.oracle.tests.integration.model.VolumeAttachment;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@HelidonTest
class ComputeResourceTest {
    private final WebTarget target;

    @Inject
    ComputeResourceTest(WebTarget target) {
        this.target = target;
    }

    @Test
    public void testAttachVolume() {
        String instanceId = "1234567890123456789012345678901234567";
        AttachVolumeRequest attachVolumeRequest = AttachIScsiVolumeRequest.builder().volumeId("my-volume").build();

        VolumeAttachment volumeAttachment =
                target.path("/v1/instances/{instance-id}/volumeAttachments")
                        .resolveTemplate("instance-id", instanceId)
                        .request(MediaType.APPLICATION_JSON)
                        .header("opc-idempotency-token", "my-token")
                        .post(Entity.json(attachVolumeRequest), VolumeAttachment.class);

        assertThat(volumeAttachment, instanceOf(IScsiVolumeAttachment.class));
        IScsiVolumeAttachment attachment = (IScsiVolumeAttachment) volumeAttachment;
        assertThat("my-volume", is(attachment.getVolumeId()));
        assertThat(3, is(attachment.getPort()));
        assertThat("2", is(attachment.getIpv4()));
        assertThat("my-attachment-id", is(attachment.getId()));
        assertThat(instanceId, is(attachment.getInstanceId()));
    }

    @Test
    public void testAttachVolumeInvalidParameter() {
        String instanceId = "myinstance";
        AttachVolumeRequest attachVolumeRequest = AttachIScsiVolumeRequest.builder().volumeId("my-volume").build();
        VolumeAttachment volumeAttachment = null;
        try {
            volumeAttachment =
                    target.path("/v1/instances/{instance-id}/volumeAttachments")
                            .resolveTemplate("instance-id", instanceId)
                            .request(MediaType.APPLICATION_JSON)
                            .header("opc-idempotency-token", "my-token")
                            .post(Entity.json(attachVolumeRequest), VolumeAttachment.class);
            assertThat("We should have got HTTP 400 Bad Request.", false);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("HTTP 400 Bad Request"));
        }
    }

    @Test
    public void testGetBinaryString() {
        byte[] result = target.path("/v1/nonJsonReturns/binaryString")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .get(byte[].class);
        assertThat(result, is("hello".getBytes()));
    }

    @Test
    public void testGetLongBinaryString() {
        byte[] result = target.path("/v1/nonJsonReturns/binaryStringWithLargeObject")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .get(byte[].class);
        assertThat(result, is("Good afternoon".getBytes()));
    }

    @Test
    public void getInstanceTest() {
        String instanceId = "1234567890123456789012345678901234567";
        Instance result = target.path("/v1/instances/{instance-id}")
                .resolveTemplate("instance-id", instanceId)
                .request(MediaType.APPLICATION_JSON)
                .get(Instance.class);

        Instance expected =
                Instance.builder()
                        .id("my-id")
                        .domain("my-domain")
                        .state(Instance.State.Running)
                        .shape("my-shape")
                        .region("Canada")
                        .build();
        assertThat(result, is(expected));
    }

    @Test
    public void getRegionTest() {
        String instanceId = "1234567890123456789012345678901234567";
        Region result = target.path("/v1/regions/{instance-id}")
                .resolveTemplate("instance-id", instanceId)
                .request(MediaType.APPLICATION_JSON)
                .get(Region.class);

        Region expected =
                Region.builder()
                        .id("my-region")
                        .endpoint("endpoint:")
                        .description("a region somewhere far away")
                        .build();
        assertThat(result, is(expected));
    }

}

package com.oracle.test.api;

import com.oracle.test.model.AttachIScsiVolumeRequest;
import com.oracle.test.model.AttachVolumeRequest;
import com.oracle.test.model.IScsiVolumeAttachment;
import com.oracle.test.model.Instance;
import com.oracle.test.model.Region;
import com.oracle.test.model.VolumeAttachment;

import io.helidon.microprofile.tests.junit5.HelidonTest;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

@Disabled // TODO
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

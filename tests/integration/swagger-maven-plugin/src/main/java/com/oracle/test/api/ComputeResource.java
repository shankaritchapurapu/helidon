package com.oracle.test.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

import com.oracle.test.model.AttachIScsiVolumeRequest;
import com.oracle.test.model.AttachVolumeRequest;
import com.oracle.test.model.AvailabilityDomain;
import com.oracle.test.model.IScsiVolumeAttachment;
import com.oracle.test.model.Instance;
import com.oracle.test.model.LaunchInstanceRequest;
import com.oracle.test.model.LifecycleStates;
import com.oracle.test.model.Region;
import com.oracle.test.model.VolumeAttachment;

import com.oracle.test.api.AbstractComputeBaseResource;

@ApplicationScoped
public class ComputeResource extends AbstractComputeBaseResource {

    @Override
    public VolumeAttachment attachVolume(
            String instanceId,
            AttachVolumeRequest attachVolumeRequest,
            String opcIdempotencyToken
    ) {
        VolumeAttachment volumeAttachment =
                IScsiVolumeAttachment.builder()
                        .id("my-attachment-id")
                        .state(VolumeAttachment.State.Attached)
                        .iqn("1")
                        .ipv4("2")
                        .port(3)
                        .volumeId("my-volume")
                        .instanceId("1234567890123456789012345678901234567")
                        .build();
        return volumeAttachment;
    }

    @Override
    public void detachVolume(String instanceId,
                             String volumeAttachmentId
    ) {
    }

    @Override
    public byte[] getBinaryString(
    ) {
        return "hello".getBytes();
    }

    @Override
    public InputStream getBinaryStringWithLargeObject(
    ) {
        return new ByteArrayInputStream("Good afternoon".getBytes());
    }

    @Override
    public Instance getInstance(
            String instanceId
    ) {
        return Instance.builder()
                .id("my-id")
                .domain("my-domain")
                .state(Instance.State.Running)
                .shape("my-shape")
                .region("Canada")
                .build();
    }

    @Override
    public Region getRegion(
            String instanceId
    ) {
        return Region.builder()
                .id("my-region")
                .endpoint("endpoint:")
                .description("a region somewhere far away")
                .build();
    }

    @Override
    public VolumeAttachment getVolumeAttachment(
            String instanceId,
            String volumeAttachmentId
    ) {
        return null;
    }

    @Override
    public Instance launchInstance(
            LaunchInstanceRequest launchInstanceRequest,
            String opcIdempotencyToken
    ) {
        return null;
    }

    @Override
    public List<AvailabilityDomain> listAvailabilityDomains(
            String instanceId
    ) {
        return null;
    }

    @Override
    public List<Instance> listInstances(
    ) {
        return null;
    }

    @Override
    public List<Region> listRegions(
    ) {
        List<Region> list = new ArrayList<>();
        list.add(Region.builder().id("1").build());
        list.add(Region.builder().id("2").build());
        return list;
    }

    @Override
    public List<VolumeAttachment> listVolumeAttachments(
            String instanceId,
            List<LifecycleStates> lifecycleState
    ) {
        return null;
    }

    @Override
    public void putBinaryString(
            byte[] binaryString
    ) {
    }

    @Override
    public void putLargeBinaryString(
            InputStream binaryString
    ) {
    }

    @Override
    public String showConsoleHistoryData(
            String instanceId,
            Integer offset,
            Integer length
    ) {
        return null;
    }

    @Override
    public void terminateInstance(
            String instanceId
    ) {
    }

    @Override
    public void voidPostWithArg(
            AttachVolumeRequest attachVolumeRequest
    ) {
    }

    @Override
    public void voidPostWithNoArg(
    ) {
    }

}
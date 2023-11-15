package com.oracle.test.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
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
            String opcIdempotencyToken,
            Principal principal,
            AuthorizationRequest authorizationRequest
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
                             String volumeAttachmentId,
                             Principal principal,
                             AuthorizationRequest authorizationRequest
    ) {
    }

    @Override
    public byte[] getBinaryString(
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return "hello".getBytes();
    }

    @Override
    public InputStream getBinaryStringWithLargeObject(
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return new ByteArrayInputStream("Good afternoon".getBytes());
    }

    @Override
    public Instance getInstance(
            String instanceId,
            Principal principal,
            AuthorizationRequest authorizationRequest
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
            String instanceId,
            Principal principal,
            AuthorizationRequest authorizationRequest
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
            String volumeAttachmentId,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return null;
    }

    @Override
    public Instance launchInstance(
            LaunchInstanceRequest launchInstanceRequest,
            String opcIdempotencyToken,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return null;
    }

    @Override
    public List<AvailabilityDomain> listAvailabilityDomains(
            String instanceId,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return null;
    }

    @Override
    public List<Instance> listInstances(
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return null;
    }

    @Override
    public List<Region> listRegions(
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        List<Region> list = new ArrayList<>();
        list.add(Region.builder().id("1").build());
        list.add(Region.builder().id("2").build());
        return list;
    }

    @Override
    public List<VolumeAttachment> listVolumeAttachments(
            String instanceId,
            List<LifecycleStates> lifecycleState,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return null;
    }

    @Override
    public void putBinaryString(
            byte[] binaryString,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
    }

    @Override
    public void putLargeBinaryString(
            InputStream binaryString,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
    }

    @Override
    public String showConsoleHistoryData(
            String instanceId,
            Integer offset,
            Integer length,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
        return null;
    }

    @Override
    public void terminateInstance(
            String instanceId,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
    }

    @Override
    public void voidPostWithArg(
            AttachVolumeRequest attachVolumeRequest,
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
    }

    @Override
    public void voidPostWithNoArg(
            Principal principal,
            AuthorizationRequest authorizationRequest
    ) {
    }

}
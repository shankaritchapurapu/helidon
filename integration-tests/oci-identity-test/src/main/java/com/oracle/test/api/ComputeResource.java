package com.oracle.test.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import com.oracle.helidon.oci.identity.AuthenticationSupportingFilter;
import com.oracle.pic.identity.authentication.SecurityContext;
import com.oracle.test.model.AttachVolumeRequest;
import com.oracle.test.model.AvailabilityDomain;
import com.oracle.test.model.IScsiVolumeAttachment;
import com.oracle.test.model.Instance;
import com.oracle.test.model.LaunchInstanceRequest;
import com.oracle.test.model.LifecycleStates;
import com.oracle.test.model.Region;
import com.oracle.test.model.VolumeAttachment;

@ApplicationScoped
public class ComputeResource extends AbstractComputeBaseResource {
    @Context
    HttpHeaders requestHeaders;

    @Override
    public VolumeAttachment attachVolume(
            String instanceId,
            AttachVolumeRequest attachVolumeRequest,
            String opcIdempotencyToken) {
        validateInternalState(true);

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
                             String volumeAttachmentId) {
        validateInternalState(true);
    }

    @Override
    public byte[] getBinaryString() {
        validateInternalState(false);

        return "hello".getBytes();
    }

    @Override
    public InputStream getBinaryStringWithLargeObject() {
        validateInternalState(false);

        return new ByteArrayInputStream("Good afternoon".getBytes());
    }

    @Override
    public Instance getInstance(
            String instanceId) {
        validateInternalState(false);

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
            String instanceId) {
        validateInternalState(false);

        return Region.builder()
                .id("my-region")
                .endpoint("endpoint:")
                .description("a region somewhere far away")
                .build();
    }

    @Override
    public VolumeAttachment getVolumeAttachment(
            String instanceId,
            String volumeAttachmentId) {
        validateInternalState(true);
        return null;
    }

    @Override
    public Instance launchInstance(
            LaunchInstanceRequest launchInstanceRequest,
            String opcIdempotencyToken) {
        validateInternalState(true);
        return null;
    }

    @Override
    public List<AvailabilityDomain> listAvailabilityDomains(
            String instanceId) {
        validateInternalState(true);
        return null;
    }

    @Override
    public List<Instance> listInstances() {
        validateInternalState(true);
        return null;
    }

    @Override
    public List<Region> listRegions() {
        validateInternalState(true);

        List<Region> list = new ArrayList<>();
        list.add(Region.builder().id("1").build());
        list.add(Region.builder().id("2").build());
        return list;
    }

    @Override
    public List<VolumeAttachment> listVolumeAttachments(
            String instanceId,
            List<LifecycleStates> lifecycleState) {
        validateInternalState(false);
        return null;
    }

    @Override
    public void putBinaryString(
            byte[] binaryString) {
        validateInternalState(true);
    }

    @Override
    public void putLargeBinaryString(
            InputStream binaryString) {
        validateInternalState(true);
    }

    @Override
    public String showConsoleHistoryData(
            String instanceId,
            Integer offset,
            Integer length) {
        validateInternalState(true);
        return null;
    }

    @Override
    public void terminateInstance(
            String instanceId) {
        validateInternalState(true);
    }

    @Override
    public void voidPostWithArg(
            AttachVolumeRequest attachVolumeRequest) {
        validateInternalState(true);
    }

    @Override
    public void voidPostWithNoArg() {
        validateInternalState(true);
    }

    private void validateInternalState(boolean expectHeader) {
        assert(principal.get() == getPrincipal().orElseThrow());
        Objects.requireNonNull(authorizationRequest);
        if (expectHeader) {
            Objects.requireNonNull(requestHeaders.getHeaderString(AuthenticationSupportingFilter.TAG_DEFAULT_HEADER));
        } else if (requestHeaders.getHeaderString(AuthenticationSupportingFilter.TAG_DEFAULT_HEADER) != null) {
            throw new IllegalStateException("unexpected header present");
        }
        SecurityContext bmcSecCtx = getAuthenticator().orElseThrow().authenticateRequest();
        Objects.requireNonNull(bmcSecCtx);
//        if (!bmcSecCtx.isSuccess()) {
//            throw new IllegalStateException();
//        }
    }

}
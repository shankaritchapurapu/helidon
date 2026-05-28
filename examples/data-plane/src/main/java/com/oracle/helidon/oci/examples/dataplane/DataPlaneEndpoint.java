/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.examples.dataplane;

import java.util.List;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.Status;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.oracle.helidon.oci.audit.AuditV2Config;
import com.oracle.helidon.oci.errorcode.ErrorCodes;
import com.oracle.helidon.oci.errorcode.RenderableException;
import com.oracle.helidon.oci.requestid.OciRequestId;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;
import com.oracle.pic.sherlock.collector.AuditPayloadAppender;
import com.oracle.pic.sherlock.collector.AuditRIO;
import com.oracle.pic.sherlock.collector.OperationSynchronousType;

/**
 * Small reference endpoint that demonstrates request-id, OCI error-code, identity,
 * audit, and Kiev support in a single Helidon SE service.
 */
@SuppressWarnings("deprecation")
@RestServer.Endpoint
@Http.Path("/data-plane")
@Service.Singleton
class DataPlaneEndpoint {
    private final DataPlaneReferenceService referenceService;
    private final boolean auditEnabled;
    private final String serviceName;

    @Service.Inject
    DataPlaneEndpoint(DataPlaneReferenceService referenceService,
                      AuditV2Config auditConfig) {
        this.referenceService = referenceService;
        this.auditEnabled = auditConfig.enabled();
        this.serviceName = DataPlaneReferenceService.DATA_STORE_NAME;
    }

    @Http.GET
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    DataPlaneStatus status(OciRequestId requestId) {
        return new DataPlaneStatus(serviceName,
                                   requestId.upstreamHeaderValue(),
                                   auditEnabled,
                                   referenceService.count());
    }

    @Http.GET
    @Http.Path("/probe")
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String probe() {
        // Keep this as a simple deployment probe; Helidon health checks belong under observe health.
        return "Data-plane operational probe passed";
    }

    @Http.GET
    @Http.Path("/robots")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    RobotCollection list(@Http.QueryParam("compartmentId") Optional<String> compartmentId,
                         @Http.QueryParam("displayName") Optional<String> displayName) {
        return referenceService.list(compartmentId.orElse(null), displayName.orElse(null));
    }

    @Http.GET
    @Http.Path("/robots/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    RobotResponse get(@Http.PathParam("id") String id, ServerResponse response) {
        return referenceService.get(id)
                .map(DataPlaneEndpoint::success)
                .orElseGet(() -> error(response, notFound(id)));
    }

    @Http.POST
    @Http.Path("/robots")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    @AuthorizationPermission("DATA_PLANE_ROBOT_WRITE")
    RobotResponse create(@Http.Entity CreateRobotRequest request,
                         ServerRequest serverRequest,
                         ServerResponse response,
                         Principal principal) {
        if (request == null) {
            return error(response, missingParameter("Request body is required."));
        }

        String displayName = request.displayName();
        if (isMissing(displayName)) {
            return error(response, missingParameter("displayName must be provided."));
        }

        String compartmentId = request.compartmentId();
        if (isMissing(compartmentId)) {
            return error(response, missingParameter("compartmentId must be provided."));
        }

        Robot robot = referenceService.create(displayName, compartmentId);
        audit(serverRequest, "CreateRobot", robot.id(), robot.compartmentId(), principal);
        return success(robot);
    }

    @Http.PUT
    @Http.Path("/robots/{id}")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    @AuthorizationPermission("DATA_PLANE_ROBOT_UPDATE")
    RobotResponse update(@Http.PathParam("id") String id,
                         @Http.Entity UpdateRobotRequest request,
                         ServerRequest serverRequest,
                         ServerResponse response,
                         Principal principal) {
        if (request == null) {
            return error(response, missingParameter("Request body is required."));
        }

        String displayName = request.displayName();
        if (isMissing(displayName)) {
            return error(response, missingParameter("displayName must be provided."));
        }

        Optional<Robot> updatedRobot = referenceService.update(id, displayName);
        if (updatedRobot.isEmpty()) {
            return error(response, notFound(id));
        }

        Robot robot = updatedRobot.orElseThrow();
        audit(serverRequest, "UpdateRobot", robot.id(), robot.compartmentId(), principal);
        return success(robot);
    }

    @Http.DELETE
    @Http.Path("/robots/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    @AuthorizationPermission("DATA_PLANE_ROBOT_DELETE")
    RobotResponse delete(@Http.PathParam("id") String id,
                         ServerRequest serverRequest,
                         ServerResponse response,
                         Principal principal) {
        Optional<Robot> deletedRobot = referenceService.delete(id);
        if (deletedRobot.isEmpty()) {
            return error(response, notFound(id));
        }

        Robot robot = deletedRobot.orElseThrow();
        audit(serverRequest, "DeleteRobot", robot.id(), robot.compartmentId(), principal);
        return success(robot);
    }

    private static void audit(ServerRequest request,
                              String eventName,
                              String resourceId,
                              String compartmentId,
                              Principal principal) {
        request.context()
                .get(AuditPayloadAppender.class.getName(), AuditPayloadAppender.class)
                .ifPresent(appender -> {
                    appender.setEventName(eventName, OperationSynchronousType.None);
                    appender.setResourceId(resourceId);
                    appender.overrideCompartmentId(compartmentId);
                    appender.overridePrincipalId(principal.getSubjectId());
                    appender.overridePrincipalTenantId(principal.getTenantId());
                    appender.appendToAuditRios(List.of(new AuditRIO(compartmentId, resourceId)));
                });
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private static RobotResponse success(Robot robot) {
        return new RobotResponse(Status.OK_200.code(), robot, null);
    }

    private static RobotResponse error(ServerResponse response, RenderableException renderable) {
        response.status(renderable.errorCode().status());
        return new RobotResponse(renderable.errorCode().status().code(), null, toErrorResponse(renderable));
    }

    private static OciErrorResponse toErrorResponse(RenderableException renderable) {
        return new OciErrorResponse(renderable.errorCode().errorCode(),
                                    renderable.getMessage(),
                                    renderable.originalMessage().orElse(null),
                                    renderable.originalMessageTemplate().orElse(null),
                                    renderable.messageArguments().isEmpty() ? null : renderable.messageArguments());
    }

    private static RenderableException notFound(String robotId) {
        return new RenderableException(ErrorCodes.NotAuthorizedOrNotFound,
                                       "No robot found for id: " + robotId);
    }

    private static RenderableException missingParameter(String message) {
        return new RenderableException(ErrorCodes.MissingParameter, message);
    }
}

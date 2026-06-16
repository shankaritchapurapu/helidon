/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.audit;

import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerRequest;

import com.oracle.pic.sherlock.collector.AuditPayloadAppender;

/**
 * Provides the current request's audit payload appender for endpoint injection.
 */
@Service.PerRequest
@Weight(Weighted.DEFAULT_WEIGHT - 30)
class AuditPayloadAppenderFactory implements Supplier<AuditPayloadAppender> {

    private final ServerRequest request;

    @Service.Inject
    AuditPayloadAppenderFactory(ServerRequest request) {
        this.request = request;
    }

    @Override
    public AuditPayloadAppender get() {
        return AuditPayloadAppenders.require(request);
    }
}

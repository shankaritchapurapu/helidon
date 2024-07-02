package com.oracle.helidon.oci.errorcode.webserver;

import io.helidon.common.config.Config;
import io.helidon.http.HeaderValues;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.ErrorHandler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.spi.ServerFeature;

import com.oracle.helidon.oci.errorcode.RenderableException;

public class ErrorCodeServerFeature implements ServerFeature {
    public static final String TYPE = "oci-error-code";
    private final String name;

    private ErrorCodeServerFeature(String name) {
        this.name = name;
    }

    public static ErrorCodeServerFeature create(Config config, String name) {
        return new ErrorCodeServerFeature(name);
    }

    public static ServerFeature create() {
        return create(Config.empty(), TYPE);
    }

    @Override
    public void setup(ServerFeatureContext serverFeatureContext) {
        for (String socket : serverFeatureContext.sockets()) {
            serverFeatureContext.socket(socket)
                    .httpRouting()
                    .error(RenderableException.class, new RenderableErrorHandler());
        }
        serverFeatureContext.socket(WebServer.DEFAULT_SOCKET_NAME)
                .httpRouting()
                .error(RenderableException.class, new RenderableErrorHandler());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String type() {
        return TYPE;
    }

    private static class RenderableErrorHandler implements ErrorHandler<RenderableException> {
        @Override
        public void handle(ServerRequest req, ServerResponse res, RenderableException e) {
            res.status(e.errorCode().status())
                    .header(HeaderValues.CONTENT_TYPE_JSON)
                    .send(e.errorDetail());
        }
    }
}

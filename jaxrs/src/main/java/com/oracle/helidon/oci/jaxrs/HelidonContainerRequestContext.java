/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.io.InputStream;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import io.helidon.webserver.http.ServerRequest;

/**
 * Wrapper that adapts Helidon's ServerRequest to JAX-RS ContainerRequestContext.
 *
 * <p>For post-matching filters, this context also provides access to {@link ResourceInfo}
 * which contains information about the matched resource method.
 */
public class HelidonContainerRequestContext implements ContainerRequestContext {

    /** Property key for storing ResourceInfo in the context. */
    public static final String RESOURCE_INFO_PROPERTY = "javax.ws.rs.container.ResourceInfo";
    /** Property key for storing the TLS client certificate chain. */
    public static final String X509_CERTIFICATE_PROPERTY = "javax.servlet.request.X509Certificate";

    private final ServerRequest request;
    private final HelidonUriInfo uriInfo;
    private final HelidonHttpHeaders httpHeaders;
    private final Map<String, Object> properties = new HashMap<>();
    private final ResourceInfo resourceInfo;
    private SecurityContext securityContext;
    private boolean aborted = false;
    private int abortStatus = 0;
    private String abortMessage;
    private InputStream entityStream;

    /**
     * Create a request context with ResourceInfo for post-matching filters.
     *
     * @param request the server request
     * @param resourceInfo the matched resource info
     */
    public HelidonContainerRequestContext(ServerRequest request, ResourceInfo resourceInfo) {
        Objects.requireNonNull(resourceInfo, "resourceInfo is null");
        this.request = request;
        this.uriInfo = new HelidonUriInfo(request);
        this.httpHeaders = new HelidonHttpHeaders(request);
        this.resourceInfo = resourceInfo;
        this.properties.put(RESOURCE_INFO_PROPERTY, resourceInfo);
        preloadClientCertificates();
    }

    private void preloadClientCertificates() {
        request.remotePeer()
                .tlsCertificates()
                .map(this::toX509Certificates)
                .filter(certificates -> certificates.length > 0)
                .ifPresent(certificates -> properties.put(X509_CERTIFICATE_PROPERTY, certificates));
    }

    private X509Certificate[] toX509Certificates(Certificate[] certificates) {
        return Arrays.stream(certificates)
                .filter(X509Certificate.class::isInstance)
                .map(X509Certificate.class::cast)
                .toArray(X509Certificate[]::new);
    }

    /**
     * Get the ResourceInfo for the matched resource method.
     * Only available for post-matching filters.
     *
     * @return the resource info, or null if not set
     */
    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    /**
     * Returns an unmodifiable snapshot of all properties associated with this request
     * context. The returned map is a read-only copy of the current {@code properties} and
     * reflects their state at the time of the call.
     *
     * @return an unmodifiable map containing the current request properties
     */
    public Map<String, Object> properties() {
        return Map.copyOf(properties);
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public void setProperty(String name, Object object) {
        properties.put(name, object);
    }

    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    @Override
    public void setRequestUri(URI requestUri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Request getRequest() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getMethod() {
        return request.prologue().method().text();
    }

    @Override
    public void setMethod(String method) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return httpHeaders.getRequestHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return httpHeaders.getHeaderString(name);
    }

    @Override
    public Date getDate() {
        return httpHeaders.getDate();
    }

    @Override
    public Locale getLanguage() {
        return httpHeaders.getLanguage();
    }

    @Override
    public int getLength() {
        return httpHeaders.getLength();
    }

    @Override
    public MediaType getMediaType() {
        return httpHeaders.getMediaType();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return httpHeaders.getAcceptableMediaTypes();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return httpHeaders.getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return httpHeaders.getCookies();
    }

    @Override
    public boolean hasEntity() {
        return request.content().hasEntity();
    }

    @Override
    public InputStream getEntityStream() {
        return entityStream != null ? entityStream : request.content().inputStream();
    }

    @Override
    public void setEntityStream(InputStream input) {
        this.entityStream = input;
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        this.securityContext = context;
    }

    @Override
    public void abortWith(Response response) {
        this.aborted = true;
        this.abortStatus = response.getStatus();
        this.abortMessage = response.getEntity() != null ? response.getEntity().toString() : "";
    }

    public boolean isAborted() {
        return aborted;
    }

    public int getAbortStatus() {
        return abortStatus;
    }

    public String getAbortMessage() {
        return abortMessage;
    }

    public ServerRequest getServerRequest() {
        return request;
    }
}

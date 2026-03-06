/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.ws.rs.container.ResourceInfo;

/**
 * An implementation of {@link javax.ws.rs.container.ResourceInfo} for Helidon.
 */
public class HelidonResourceInfo implements ResourceInfo {

    static final Map<String, Method> METHOD_MAP = new HashMap<>();
    static final ReadWriteLock METHOD_MAP_LOCK = new ReentrantReadWriteLock();

    private final String serviceType;
    private final String methodSignature;
    private final String mapKey;

    private Method method;
    private Class<?> resourceClass;

    /**
     * Constructor based on service and method signature.
     *
     * @param serviceType the service to be loaded
     * @param methodSignature the method signature
     */
    public HelidonResourceInfo(String serviceType, String methodSignature) {
        this.serviceType = serviceType;
        this.methodSignature = methodSignature;
        this.mapKey = serviceType + "::" + methodSignature;
    }

    @Override
    public Method getResourceMethod() {
        if (method != null) {
            return method;
        }

        Lock readLock = METHOD_MAP_LOCK.readLock();
        readLock.lock();
        try {
            method = METHOD_MAP.get(mapKey);
            if (method != null) {
                return method;
            }
        } finally {
            readLock.unlock();
        }

        loadResourceClass();

        for (Method m : resourceClass.getDeclaredMethods()) {
            if (methodSignature(m).equals(methodSignature)) {
                Lock writeLock = METHOD_MAP_LOCK.writeLock();
                writeLock.lock();
                try {
                    METHOD_MAP.put(mapKey, m);
                    method = m;
                } finally {
                    writeLock.unlock();
                }
                return method;
            }
        }

        throw new IllegalStateException("Method not found: " + methodSignature);
    }

    @Override
    public Class<?> getResourceClass() {
        if (resourceClass != null) {
            return resourceClass;
        }
        loadResourceClass();
        return resourceClass;
    }

    void loadResourceClass() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            resourceClass = loader.loadClass(serviceType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static String methodSignature(Method m) {
        String returnType = m.getReturnType().getTypeName();
        String name = m.getName();
        String params = Arrays.stream(m.getParameterTypes())
                .map(Class::getTypeName)
                .collect(Collectors.joining(", "));
        return String.format("%s %s(%s)", returnType, name, params).trim();
    }
}

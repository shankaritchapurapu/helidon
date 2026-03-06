/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */
package com.oracle.helidon.oci.jaxrs;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HelidonResourceInfo}.
 */
class HelidonResourceInfoTest {

    /**
     * Simple service class used by the tests.
     */
    public static class SampleService {
        public String hello(String name) {
            return "hi " + name;
        }

        public void doNothing() {
        }
    }

    @BeforeEach
    void clearCache() {
        HelidonResourceInfo.METHOD_MAP.clear();
    }

    @Test
    void shouldLoadResourceClass() {
        HelidonResourceInfo info = new HelidonResourceInfo(
                SampleService.class.getName(),
                "void doNothing()");   // signature does not matter for this test
        Class<?> clazz = info.getResourceClass();
        assertEquals(SampleService.class, clazz,
                     "The returned class should be the SampleService class");
    }

    @Test
    void shouldThrowWhenClassNotFound() {
        HelidonResourceInfo info = new HelidonResourceInfo(
                "non.existing.ClassName",
                "void nothing()");

        RuntimeException ex = assertThrows(RuntimeException.class,
                                           info::getResourceClass,
                                           "Loading a non‑existent class must wrap ClassNotFoundException in RuntimeException");
        assertInstanceOf(ClassNotFoundException.class, ex.getCause(),
                         "The cause should be a ClassNotFoundException");
    }

    @Test
    void methodSignatureNoParameters() throws Exception {
        Method m = SampleService.class.getMethod("doNothing");
        String signature = HelidonResourceInfo.methodSignature(m);
        assertEquals("void doNothing()", signature);
    }

    @Test
    void methodSignatureWithParameter() throws Exception {
        Method m = SampleService.class.getMethod("hello", String.class);
        String signature = HelidonResourceInfo.methodSignature(m);
        assertEquals("java.lang.String hello(java.lang.String)", signature);
    }

    @Test
    void shouldFindMethodWhenNotCached() {
        String signature = "java.lang.String hello(java.lang.String)";
        HelidonResourceInfo info = new HelidonResourceInfo(SampleService.class.getName(), signature);

        // First call – cache is empty, method should be located via reflection
        Method method = info.getResourceMethod();

        assertNotNull(method, "Method must be found");
        assertEquals("hello", method.getName());
        assertEquals(String.class, method.getReturnType());

        // Verify that the method was stored in the static cache
        String mapKey = SampleService.class.getName() + "::" + signature;
        Method cached = HelidonResourceInfo.METHOD_MAP.get(mapKey);
        assertSame(method, cached,
                   "The located method must be stored in METHOD_MAP for future look‑ups");
    }

    @Test
    void shouldReturnCachedMethodWithoutRelookingUp() throws Exception {
        // Populate the cache manually
        Method original = SampleService.class.getMethod("hello", String.class);
        String signature = HelidonResourceInfo.methodSignature(original);
        String mapKey = SampleService.class.getName() + "::" + signature;
        HelidonResourceInfo.METHOD_MAP.put(mapKey, original);

        // New instance – cache already contains the entry
        HelidonResourceInfo info = new HelidonResourceInfo(SampleService.class.getName(), signature);

        Method method = info.getResourceMethod();

        assertSame(original, method,
                   "When the entry is already in METHOD_MAP the same Method instance must be returned");
    }

    @Test
    void shouldThrowWhenMethodSignatureUnknown() {
        // Use a signature that does not match any method of SampleService
        String bogusSignature = "int unknown(java.lang.String)";
        HelidonResourceInfo info = new HelidonResourceInfo(SampleService.class.getName(), bogusSignature);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                                                info::getResourceMethod,
                                                "If no method matches the signature an "
                                                        + "IllegalStateException must be thrown");
        assertTrue(ex.getMessage().contains(bogusSignature));
    }
}


package com.oracle.helidon.oci.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PreconditionsTest {

    @Test
    void checkArgumentTest() {
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkArgument(false));
        Preconditions.checkArgument(true);

        try {
            Preconditions.checkArgument(false, "error");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("error", e.getMessage());
        }
        Preconditions.checkArgument(true, "error");

        try {
            Preconditions.checkArgument(false, "error %s", "one");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("error one", e.getMessage());
        }
        Preconditions.checkArgument(true, "error %", "one");
    }

    @Test
    void checkNotNullTest() {
        assertThrows(NullPointerException.class, () -> Preconditions.checkNotNull(null, "error"));
        Preconditions.checkNotNull(new Object(), "error");
    }

    @Test
    void checkNotNullWithIllegalArgExceptionTest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.checkNotNullWithIllegalArgException(null, "error"));
        Preconditions.checkNotNullWithIllegalArgException(new Object(), "error");
    }

    @Test
    void isNullOrEmptyTest() {
        assertTrue(Preconditions.isNullOrEmpty(""));
        assertTrue(Preconditions.isNullOrEmpty(null));
        assertFalse(Preconditions.isNullOrEmpty("value"));
    }

    @Test
    void checkPositiveTest() {
        Preconditions.checkPositive(1, "test");
        Preconditions.checkPositive(10, "test");
        Preconditions.checkPositive(Integer.MAX_VALUE, "test");
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkPositive(0, "test"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkPositive(-1, "test"));
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.checkPositive(Integer.MIN_VALUE, "test"));
    }

    @Test
    void checkPositiveLongTest() {
        Preconditions.checkPositive(1L, "test");
        Preconditions.checkPositive(10L, "test");
        Preconditions.checkPositive(Long.MAX_VALUE, "test");
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkPositive(0L, "test"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkPositive(-1L, "test"));
        assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkPositive(Long.MIN_VALUE, "test"));
    }

    @Test
    void checkNonNegativeTest() {
        Preconditions.checkNonNegative(1, "test");
        Preconditions.checkNonNegative(0, "test");
        Preconditions.checkNonNegative(Integer.MAX_VALUE, "test");
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkNonNegative(-1, "test"));
        assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkNonNegative(-100, "test"));
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.checkNonNegative(Integer.MIN_VALUE, "test"));
    }

    @Test
    void checkNonNegativeLongTest() {
        Preconditions.checkNonNegative(1L, "test");
        Preconditions.checkNonNegative(0L, "test");
        Preconditions.checkNonNegative(Long.MAX_VALUE, "test");
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkNonNegative(-1L, "test"));
        assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkNonNegative(-100L, "test"));
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.checkNonNegative(Long.MIN_VALUE, "test"));
    }

}
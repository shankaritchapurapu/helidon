package com.oracle.helidon.oci.requestid;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class OciRequestIdImplTest {
    @Test
    void testValidNotStripped() {
        String valid = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        String actual = OciRequestIdImpl.stripNotAllowedCharacters(valid);

        assertThat(actual, is(valid));
    }

    @Test
    void testInvalidStripped() {
        char[] allChars = new char[255];
        for (int i = 0; i < allChars.length; i++) {
            allChars[i] = (char) i;
        }
        String valid = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
        String toTest = new String(allChars);
        String actual = OciRequestIdImpl.stripNotAllowedCharacters(toTest);

        assertThat(actual, is(valid));
    }

    @Test
    void testNonAscii() {
        String valid = "id";
        String toTest = "\u010D\u0159id\u017E";
        String actual = OciRequestIdImpl.stripNotAllowedCharacters(toTest);

        assertThat(actual, is(valid));
    }
}
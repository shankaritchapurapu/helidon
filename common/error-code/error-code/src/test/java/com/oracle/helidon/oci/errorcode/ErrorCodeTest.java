/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oracle.helidon.oci.errorcode;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.errorcode.ErrorCode.CannotParseRequest;
import static io.helidon.common.testing.junit5.OptionalMatcher.optionalEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ErrorCodeTest {
    @Test
    void testErrorDetailNotNullable() {
        assertThrows(NullPointerException.class,
                     () -> ErrorDetail.create(null, "A", "B", "C", Map.of()));

        assertThrows(NullPointerException.class,
                     () -> ErrorDetail.create(CannotParseRequest, null, "B", "C", Map.of()));

        ErrorDetail detail = ErrorDetail.create(CannotParseRequest, "Message", null, null, null);
        assertThat(detail.getErrorCode(), is(CannotParseRequest));
        assertThat(detail.getMessage(), is("Message"));
        assertThat(detail.getOriginalMessage(), nullValue());
        assertThat(detail.getOriginalMessageTemplate(), nullValue());
        assertThat(detail.getMessageArguments(), nullValue());
    }

    @Test
    void testRenderableExceptionNullability() {
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(null, "A"));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(null, "A", new Object[0]));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(null, "A", new IllegalArgumentException()));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(null, "A", new Object[0]));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(null, "A", "B", "C", Map.of(), new IllegalAccessError()));

        assertThrows(NullPointerException.class,
                     () -> new RenderableException(CannotParseRequest, null));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(CannotParseRequest, null, new Object[0]));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(CannotParseRequest, null, new IllegalArgumentException()));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(CannotParseRequest, null, new Object[0]));
        assertThrows(NullPointerException.class,
                     () -> new RenderableException(CannotParseRequest, null, "B", "C", Map.of(), new IllegalAccessError()));

        var e = new RenderableException(CannotParseRequest, "Message", null, null, null, null);
        assertThat(e.getMessage(), is("Message"));
        assertThat(e.errorCode(), is(CannotParseRequest));
        assertThat(e.originalMessage(), optionalEmpty());
        assertThat(e.originalMessageTemplate(), optionalEmpty());
        assertThat(e.messageArguments(), is(Map.of()));
    }

    @Test
    void testAllCodesHaveReasonPhrase() {
        for (ErrorCode value : ErrorCode.values()) {
            assertThat(value.name(), value.status().reasonPhrase(), not(""));
            assertThat(value.name(), value.status().code(), greaterThan(0));
        }
    }
}

/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.identity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static com.oracle.helidon.oci.identity.DigestStreamer.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class DigestStreamerTest {

    @Test
    void testItAllWays() {
        String input = "Hello World!\nHello Helidon OCI Pegasus!";

        String digest1 = calculateDigest(input.getBytes(StandardCharsets.UTF_8));
        String digest2 = calculateDigest(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        String digest3 = calculateDigest(
                RepeatableInputStreamer.create(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));

        assertThat(digest1, equalTo("mBUBHKPDwdPIMzPAY6pBI6UZM4rkAcReE+WBf1bAv5g="));
        assertThat(digest2, equalTo("mBUBHKPDwdPIMzPAY6pBI6UZM4rkAcReE+WBf1bAv5g="));
        assertThat(digest3, equalTo("mBUBHKPDwdPIMzPAY6pBI6UZM4rkAcReE+WBf1bAv5g="));
    }

}

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

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Handles wrapping an input stream to apply digest creation.
 */
class DigestStreamer {
    static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";

    private DigestStreamer() {
    }

    /**
     * Wraps an input stream to calculate the message digest while processing.
     *
     * @param stream the original stream
     * @return the wrapped digest input stream
     */
    public DigestInputStream create(InputStream stream) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            return new DigestInputStream(stream, md);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Calculates a digest by consuming the entire input stream.
     *
     * @param stream the stream to consume
     * @return the digest
     */
    public static String calculateDigest(InputStream stream) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            DigestUtils utils = new DigestUtils(md);
            return Base64.encodeBase64String(utils.digest(stream));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String calculateDigest(byte[] content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            byte[] digest = messageDigest.digest(content);
            return Base64.encodeBase64String(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * AuthN request expects signed headers as a map with String keys and List of String values.
     */
    static Map<String, List<String>> convertSignedHeadersToAuthnReqFormat(
            Map<String, String> signedHeaders) {
        Map<String, List<String>> map = new HashMap<>();
        for (Map.Entry<String, String> entry : signedHeaders.entrySet()) {
            map.put(entry.getKey(), List.of(entry.getValue()));
        }
        return map;
    }

}

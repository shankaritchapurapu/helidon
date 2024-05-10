/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.secret;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;

import com.oracle.pic.commons.crypto.PEMFileRSAPrivateKeySupplier;
import com.oracle.pic.commons.util.CertUtils;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

public class PkiCertificate {

    @JsonbProperty
    private RSAPrivateKey key;
    @JsonbProperty
    private List<X509Certificate> intermediates;
    @JsonbProperty
    private X509Certificate cert;

    public static PkiCertificate newInstance(String jsonBlob, String pass) {
        try (var b = JsonbBuilder.newBuilder()
                .withConfig(new JsonbConfig().withDeserializers(new PkiPrivateKey(pass), new PkiX509Certificate()))
                .build()) {
            return b.fromJson(jsonBlob, PkiCertificate.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RSAPrivateKey getKey() {
        return key;
    }

    public void setKey(RSAPrivateKey key) {
        this.key = key;
    }

    public List<X509Certificate> getIntermediates() {
        return intermediates;
    }

    public void setIntermediates(List<X509Certificate> intermediates) {
        this.intermediates = intermediates;
    }

    public X509Certificate getCert() {
        return cert;
    }

    public void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    public String getLeafCertAsPEM() {
        return CertUtils.convertCertificateToPEM(cert);
    }

    public String getKeyAsPEM() {
        return CertUtils.convertRSAPrivateKeyToPEM(key);
    }

    public String getIntermediatesAsPEM() {
        return CertUtils.convertCertificatesToPEM(intermediates);
    }

    static class PkiPrivateKey implements JsonbDeserializer<RSAPrivateKey> {

        private final String pass;

        public PkiPrivateKey(String pass) {
            this.pass = pass;
        }

        @Override
        public RSAPrivateKey deserialize(JsonParser jsonParser, DeserializationContext ctx, Type type) {
            byte[] bytes = jsonParser.getString().getBytes();
            PEMFileRSAPrivateKeySupplier supplier = PEMFileRSAPrivateKeySupplier.newInstance(bytes, pass);
            return supplier.getKey("key").orElseThrow();
        }
    }

    static class PkiX509Certificate implements JsonbDeserializer<X509Certificate> {
        @Override
        public X509Certificate deserialize(JsonParser jsonParser, DeserializationContext ctx, Type type) {
            try {
                return CertUtils.parsePEM(jsonParser.getString());
            } catch (IOException | CertificateException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

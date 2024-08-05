/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonGenerator;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PkiCertificateDeserializationTest {

    static final String PRIV_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQCagHQPtub/WhTJ
            Fjh8xwKqE0VMJMPYAhOzj4LiAsEPLVP2/0G+Zcd5HTNXaam4zE3jPbj0ovU1Kh82
            J8gbDHCUABzrIAA3joJiijMVmmdpZfBnv3Cq6lwoU849UKLLuwQ05a1MBzOQ9e9y
            0PIBJF0vuRz1N7s0LZ7ymccex31WZl/13CdFX70PZavBC+8qAKYsyg2JBmbg5wLV
            YjfjTQVE+ASOd9g6rHZ+FjnwJlQweouFX4O2htmWz/Bvfc7J69xR6UrJCGKV4v8x
            gqbckSU8aPanmjhEpCJEpBTamzII1m5CsoeiJZcnxFSrreKPVEgMb59iMfT2RVD9
            1RneEyivAgMBAAECggEBAJLtXXIkSgDENcgVbZqb1xjOQrRqsiQVtY6pB5rPa7F3
            NcZKcMcUG2I+NrkGLvUitV9JLz7WScJJUG0737x5VAYrJVUklZA/4ha7vmDo+Tbu
            TnUbRZYEUae4KwV8TZTZQOLdGLSYlJ1ihFf4BGy5nDTqIXPBkDHKwMGNJCNNKxCh
            yJEhgeUpO7fHnlFQqvVg9dNI67TFzozmMVphl9PGiaz1X7yhjS+5OaCCGsqGSRRi
            scxotjm9zOS5d/cXrM/27LGLxqYQ5zxvH7VV/If0OqVhXna4QxF/dg2fcx/gqCxL
            ujR3ZB6Wkq1YSXBaXzxPgRk8BrFLowxzJKjLRGn9gsECgYEAyLIbxyBzwJ4lkx64
            9scBWo18wV8jBkBpbFUIU1NgbAU1LH/wOCadAPoAQ9ey2kNJOe5TK/VNRa9RmVlr
            e053nZo/6PbPz83SSBxICv15AlIyCgJyTLLw0fJzIDiNI79hQ+7evOFKWMhfP5Hg
            D8ygrGe3hJSXLHtc0g2zVeILUpcCgYEAxROil2vOOi7kKQcUI8l9VdMP++DdqZlb
            VoUBV0457hBt7fM3ptenHw2CeDyxG5sXbpwMlBOfYqLOJ9SZM9r/RnRDRGdHOUv5
            ZmiOaXjL2z1NZtH7oh5dso2dA6+YJul481FwOOCWsP2Sw3j8PZANYI6h28E5GpC8
            KZVBoqRu1akCgYEAp+E9FTvTN+vSWJeZd2yOeFXkZskTPjaBOUqD9VfIT3OnwDqh
            EgdSzC9UK2vyQ1uHlHsEfb/rTIBUDKfdHb9Mlbwi+9gBi72KyiFGLlNLdyKUkwTq
            4bfJdIWPwgMsJRwA29ej4HHARDYQXtfCitTMDouP2sqFDoN+6ILnYc79U68CgYEA
            v7lbay4g4e+uXc+aIzqo6S69FRBs9MrR1fjtFg9vQg/Psjb0RCb2Wbpd3DVHI5Ed
            GalMf14bp+y03IDyTkVWJSeELW7AFpi3144ORcOTpwUbYNUVlWEvTD6CqwxP3EM0
            PJpUr7CmSSdXeT0hP479xJreEhkHQbXeqCGhSeXoQYkCgYEAvbFjXQh5t3lideEu
            YGdaCX57DnIxVTXmmgtoG4XYNkaPZS9JdUw4Nv+ggCSRA4qdBU+Tjj9/U8pSxwU2
            dXWlFtxmXJHxkul5cXAbE/UctnJOMSi3q/tKfAmwu/8T8pmj3eA1ljBYl3RPPPsw
            h44kzY0S2GcW3Plsgf34NWqpOyQ=
            -----END PRIVATE KEY-----
            """;

    static final String LEAF_CERT = """
            -----BEGIN CERTIFICATE-----
            MIICsjCCAZoCFHSfN5FZtVYj6nNUSXnQqZKTfYorMA0GCSqGSIb3DQEBCwUAMBUx
            EzARBgNVBAMTCkhlbGlkb24tQ0EwIBcNMjAwNjExMTQwOTE3WhgPMjA3OTA5MjAx
            NDA5MTdaMBQxEjAQBgNVBAMTCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQAD
            ggEPADCCAQoCggEBAPn1ushL2LthSermly2ITme9/1JyRK4EvDJ5Kh60DjYkTcuZ
            Kr8rPaGyBUc2oBsyY1OjgEykiMOE4gc6kf8UgPG5Cm24koRtejTsN5O+NINp2zSF
            2YIMOd8XKTGHKxIU6Cd6Sc+DgIDvibYrLuXPgx3ZifGVHOOb9nYjbg51aRFDXzep
            H+DchznBdKhLpTHq97Q4M0C9vOMJUu/v9J6vBJQBlp+cBkf+TMyDYnt/erW6TwES
            Pb6vnzw+fkmTLeWDr69ykUqTf7KbRB4fQkZzxGjo0E6oyOS5mKXHT9ZKR53MLZui
            UJ2Y+nreiZp3Q8IBL5NOkPeq+wrIPpY1gGFOr2kCAwEAATANBgkqhkiG9w0BAQsF
            AAOCAQEACkg/r7mhcjK2HtBQw50mdflM9PYudGeuOZpNV9WzfiFw9JkjHi1botFF
            XXZ/QELHQ60HZsgOaiel1ci5Ih0GOL5BLvsmSsvHMeQoGRvfmc21Gt09F1WyO/jr
            Z3o0QweOXlgfsVvrjFKeEe36DvUD5zmVGL7/Mn/JYeEeqzhyUSeI/IOFCHgwoRJ+
            A8c20P+iiTosKS4DWL5qLQhmjRp/VGJAxHAao2slt4gY+LTqvOQP1/1r0dTyNaku
            Hq05Et+nWcBk+upHcB9rs2rz95UVD98QU9AyOaSKOQjw3OXGRvWNnTdQRhsKQL4y
            QbNnm0Ud3Ac5e3iCVHWGzKfpoYv6vg==
            -----END CERTIFICATE-----
            """;

    static final String INTERMEDIATE_CERT = """
            -----BEGIN CERTIFICATE-----
            MIIDDTCCAfWgAwIBAgIJAJe/JSjE/o6LMA0GCSqGSIb3DQEBCwUAMBUxEzARBgNV
            BAMTCkhlbGlkb24tQ0EwIBcNMjAwNjExMTQwOTEzWhgPMjA3OTA5MjAxNDA5MTNa
            MBUxEzARBgNVBAMTCkhlbGlkb24tQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw
            ggEKAoIBAQCUf6dmoZh5o+xbbmKpT8coKBFLchvGq0b7fIkPmAA46GWHS+FeJrbq
            FJyPHy6htdh2/fkIf3VxFlJXy7wLgQENoBEZc6IyLtOMSPL88cyUssAHZcOxTgOU
            qb8T7jgQuLnP04mPAjFcQfxNxraD2Oak7X4zFqfVbWiIWFXCm3DrB12sJYPKa5FY
            kAh35EKTBUS9c+48hrpA+2UZw+r6JMJcPnE5e0HMdUFlqJkVAK+hrAcWYUF3RSj8
            w5zJb3+bMcFitWrDB+LKqDfbUokmx/a8GF1gm140ir1jphakcxWNfHQsHWOpD+iH
            i/oSdWuBP3wxo6kcP5Il2EzNbEgeu2KpAgMBAAGjXjBcMB0GA1UdDgQWBBRMeKJr
            +qpGO64t0p/yH0JWOB2ZYDALBgNVHQ8EBAMCAqQwDwYDVR0TBAgwBgEB/wIBAzAd
            BgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDQYJKoZIhvcNAQELBQADggEB
            AI2BHZHJkMAh1r78NS9pKC53IFaNh7P14far6R5YDc9K5y1/qdMTqONWYNR2hMS5
            XU1cTQQmRBr01/vtUzrD1OF9K5uob+ZuoWc9Q58DpJgtV/gfPdK9AwShbV7XQWJy
            kDzTm0hx7zm9CDTZJWaLzKgcUOJ7W5FRFTycW+YGrI45Pe3rDE/PWiDtSv/9Lrjd
            dWbrvq+EtGZQG5kAVFVAT0uuhmGHAy67j/920jVPuFXjqemXaqurGqFTLi/Tf68+
            wZ0ozing3criqSCYTRHCflyAgjSYRlbZGTguEwH6bqdxj1O9jlbOaEdojZQzeaKv
            aJpCn0/c5qRd/2nyFBWrm9w=
            -----END CERTIFICATE-----
            """;

    @Test
    void deserialize() throws CertificateException {
        JsonObject material = Json.createObjectBuilder()
                .add("key", PRIV_KEY)
                .add("cert", LEAF_CERT)
                .add("intermediates", Json.createArrayBuilder()
                        .add(INTERMEDIATE_CERT))
                .build();

        StringWriter stringWriter = new StringWriter();
        Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, "true")).createWriter(stringWriter).write(material);
        String json = stringWriter.toString();
        PkiCertificate pkiCertificate = PkiCertificate.newInstance(json, "password");
        assertThat(pkiCertificate.getKey().getAlgorithm(), is("RSA"));
        assertThat(pkiCertificate.getCert().getSubjectX500Principal().getName(), is("CN=localhost"));
        assertThat(pkiCertificate.getIntermediates().getFirst().getSubjectX500Principal().getName(), is("CN=Helidon-CA"));
    }
}

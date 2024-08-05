/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.common.javax.jaxrs.shim;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

public class JavaxVariant extends Variant {
    public JavaxVariant(jakarta.ws.rs.core.Variant delegate) {
        super(new JavaxMediaType(delegate.getMediaType()), delegate.getLanguage(), delegate.getEncoding());
    }

    public static class JavaxVariantListBuilder extends VariantListBuilder {
        private final jakarta.ws.rs.core.Variant.VariantListBuilder delegate;

        public JavaxVariantListBuilder(jakarta.ws.rs.core.Variant.VariantListBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<Variant> build() {
            return delegate.build().stream()
                    .map(JavaxVariant::new)
                    .map(Variant.class::cast)
                    .toList();
        }

        @Override
        public VariantListBuilder add() {
            delegate.add();
            return this;
        }

        @Override
        public VariantListBuilder languages(Locale... languages) {
            delegate.languages(languages);
            return this;
        }

        @Override
        public VariantListBuilder encodings(String... encodings) {
            delegate.encodings(encodings);
            return this;
        }

        @Override
        public VariantListBuilder mediaTypes(MediaType... mediaTypes) {
            delegate.mediaTypes(Arrays.stream(mediaTypes)
                                        .map(JakartaMediaType::new)
                                        .map(jakarta.ws.rs.core.MediaType.class::cast)
                                        .toArray(jakarta.ws.rs.core.MediaType[]::new));
            return this;
        }
    }
}

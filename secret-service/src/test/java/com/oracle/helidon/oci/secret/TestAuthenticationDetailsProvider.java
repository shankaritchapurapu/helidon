/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.secret;

import java.io.InputStream;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;

@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT + 1000)
@Service.ExternalContracts(BasicAuthenticationDetailsProvider.class)
class TestAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {
    @Override
    public String getKeyId() {
        return "";
    }

    @Override
    public InputStream getPrivateKey() {
        return null;
    }

    @Override
    public String getPassPhrase() {
        return "";
    }

    @Override
    public char[] getPassphraseCharacters() {
        return new char[0];
    }
}

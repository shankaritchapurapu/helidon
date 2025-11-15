/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.workflow.client;

import java.io.InputStream;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;


/**
 * Will be used when BasicAuthenticationDetailsProvider is looked up from registry, like when injected in OciWorkflowClient()
 */
@Service.Provider
@Weight(Weighted.DEFAULT_WEIGHT + 1000)
@Service.ExternalContracts(BasicAuthenticationDetailsProvider.class)
class DummyAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {

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

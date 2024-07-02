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

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.configurable.Resource;

/**
 * Blueprint configuration for {@link com.oracle.helidon.oci.secret.DefaultSecretServiceTlsManager}.
 */
@Prototype.Blueprint
@Prototype.Configured
interface SecretServiceTlsManagerConfigBlueprint extends Prototype.Factory<SecretServiceTlsManager> {

    /**
     * The schedule for trigger a reload, downloading PKI material from SSv2.
     *
     * @return the schedule for reload
     */
    @Option.Configured
    ReloadConfig reload();

    /**
     * The SSv2 path to PKI provided certificate material.
     *
     * @return the secret service path
     */
    @Option.Configured
    PkiConfig pki();

    /**
     * CA trust path.
     *
     * @return path to CA pem file
     */
    @Option.Configured
    Resource trust();

}
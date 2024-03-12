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

import io.helidon.builder.api.Prototype;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * Blueprint configuration for {@link OciCertificatesTlsManager}.
 */
@Prototype.Blueprint
@Configured
interface SecretServiceTlsManagerConfigBlueprint extends Prototype.Factory<SecretServiceTlsManager> {

    /**
     * The schedule for trigger a reload check, testing whether there is a new {@link io.helidon.common.tls.Tls} instance
     * available.
     *
     * @return the schedule for reload
     */
    @ConfiguredOption
    String schedule();

    @ConfiguredOption
    String pkiCertificatePath();

}

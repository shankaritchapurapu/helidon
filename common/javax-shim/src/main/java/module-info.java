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

import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxClientBuilder;
import com.oracle.helidon.oci.common.javax.jaxrs.shim.JavaxRuntimeDelegate;

module helidon.oci.common.javax.shim {
    requires java.ws.rs;
    requires jakarta.ws.rs;
    requires oci.java.sdk.common.httpclient;

    exports com.oracle.helidon.oci.common.javax.jaxrs.shim;

    provides javax.ws.rs.client.ClientBuilder with JavaxClientBuilder;
    provides javax.ws.rs.ext.RuntimeDelegate with JavaxRuntimeDelegate;
}
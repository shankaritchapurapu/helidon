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

import java.io.InputStream;
import java.net.URI;
import java.security.KeyPair;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Vetoed;

import com.oracle.pic.identity.authentication.AuthenticationClient;
import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authentication.ServiceAuthenticationClient;
import com.oracle.pic.identity.authentication.SignedRequestAuthenticationClient;
import com.oracle.pic.identity.authentication.entities.BaseOnBehalfOfRequest;
import com.oracle.pic.identity.authentication.key.WarnHardCodedRSAPrivateKeySupplier;
import com.oracle.pic.identity.authentication.signedRequest.Algorithm;
import com.oracle.pic.identity.authentication.signedRequest.RequestSigner;
import com.oracle.pic.identity.authentication.utils.TokenKeyPair;
import org.glassfish.jersey.internal.guava.Preconditions;

/**
 * This is used for integration tests on desktop to avoid having to deal with auth since there is no
 * way to bypass/disable CertificateSupplier.
 */
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-identity/src/main/java/com/oracle/oci/sfw/netty/identity/test/client/PassThruServiceAuthenticationClient.java
@Vetoed
class PassThruServiceAuthenticationClient implements ServiceAuthenticationClient {
  private static final String SIGNATURE_VERSION = "1";
  private static final String dummyOcid =
      "ocid1.tenancy.oc1.."
          + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/"
          + "ocid1.user.oc1.."
          + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/"
          + "ff:30:30:ad:aa:7f:8a:ad:96:d4:4d:f2:2d:df:61:94";
  private final SignedRequestAuthenticationClient signerClient;

  PassThruServiceAuthenticationClient() {
    this.signerClient =
        new AuthenticationClient.SignedRequestBuilder(new WarnHardCodedRSAPrivateKeySupplier())
            .build();
  }

  @Override
  public Map<String, String> getSignedRequestHeaders(
      String method,
      URI uri,
      Map<String, List<String>> headers,
      byte[] body) {
    Preconditions.checkArgument(
        headers.containsKey(IdentityHeaders.HOST), "HOST HttpHeader is required!");
    Preconditions.checkArgument(
        headers.containsKey(IdentityHeaders.CONTENT_TYPE), "CONTENT_TYPE HttpHeader is required!");

    return signerClient.getSignedRequestHeaders(
        dummyOcid,
        Algorithm.RSA256,
        method,
        uri,
        Map.of(
            IdentityHeaders.HOST,
            List.of(headers.get(IdentityHeaders.HOST).get(0)),
            IdentityHeaders.CONTENT_TYPE,
            List.of(headers.get(IdentityHeaders.CONTENT_TYPE).get(0))),
        body,
        SIGNATURE_VERSION);
  }

  @Override
  public Map<String, String> getSignedRequestHeaders(
      String method,
      URI uri,
      Map<String, List<String>> headers,
      byte[] body,
      boolean allowBodyForGet) {
    return getSignedRequestHeaders(method, uri, headers, body);
  }

  @Override
  public String getKeyId() {
    return "FAKE-SECURITY-TOKEN";
  }

  @Override
  public Map<String, String> getSignedHeaderWithRPST(
      String token,
      String method,
      URI uri,
      Map<String, List<String>> headers,
      KeyPair keyPair,
      byte[] body) {
    return getSignedRequestHeaders(method, uri, headers, body);
  }

  @Override
  public Map<String, String> getSignedHeaderWithRPST(
      String token,
      String method,
      URI uri,
      Map<String, List<String>> headers,
      KeyPair keyPair,
      byte[] body,
      boolean bool) {
    return getSignedRequestHeaders(method, uri, headers, body);
  }

  @Override
  public String getOnBehalfOfToken(
      Principal primaryPrincipal, Principal servicePrincipal, String targetServiceName) {
    return null;
  }

  @Override
  public String getOnBehalfOfToken(
      Principal primaryPrincipal,
      Principal servicePrincipal,
      Set<String> targetServiceNames,
      Duration expiration) {
    return null;
  }

  @Override
  public String getDelegationToken(
      Principal primaryPrincipal,
      Principal servicePrincipal,
      Set<String> targetServiceNames,
      Set<String> delegateGroups) {
    return null;
  }

  @Override
  public String getDelegationToken(
      Principal primaryPrincipal,
      Principal servicePrincipal,
      Set<String> targetServiceNames,
      Set<String> delegateGroups,
      Duration expiration) {
    return null;
  }

  @Override
  public String getDelegationTokenWithMatchingRules(
      Principal principal, Principal principal1, Set<String> set, Set<String> set1) {
    return null;
  }

  @Override
  public String getDelegationTokenWithMatchingRules(
      Principal principal,
      Principal principal1,
      Set<String> set,
      Set<String> set1,
      Duration duration) {
    return null;
  }

  @Override
  public String getDelegationTokenWithInstanceOcid(String s) {
    return null;
  }

  @Override
  public String exchangeDelegationTokenMatchingRule(
      String delegationToken,
      BaseOnBehalfOfRequest.RequestType requestType,
      Set<String> matchingRules) {
    return "NO-OP";
  }

  @Override
  public TokenKeyPair exchangeBearerToken(String bearerToken) {
    return null;
  }

  @Override
  public TokenKeyPair tokenExchangeObo(
      String clientBearerToken, String serviceBearerToken, Set<String> targetServices) {
    return null;
  }

  @Override
  public void close() {
    // no-op
  }

  @Override
  public Optional<String> getOpcRequestId() {
    return Optional.empty();
  }

  @Override
  public String getUserAPIKey(String s) {
    return null;
  }

  @Override
  public InputStream getPrivateKey() {
    return null;
  }

  @Override
  public RequestSigner getRequestSigner() {
    return null;
  }

  @Override
  public String getAuthServicePublicKey(String s) {
    return null;
  }

}

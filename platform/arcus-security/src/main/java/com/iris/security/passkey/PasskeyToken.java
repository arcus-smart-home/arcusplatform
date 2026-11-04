/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.security.passkey;

import org.apache.shiro.authc.HostAuthenticationToken;
import org.apache.shiro.authc.RememberMeAuthenticationToken;

/**
 * A passkey (WebAuthn) login attempt: the id of the login request the browser
 * was given and the browser's signed response to it. Verified by the passkey realm.
 */
@SuppressWarnings("serial") // not intended for long term / cross-vm serialization
public class PasskeyToken implements HostAuthenticationToken, RememberMeAuthenticationToken {
   private final String requestId;
   private final String credential;
   private String host;
   private boolean rememberMe = false;

   /**
    * @param requestId the id returned with the login options
    * @param credential the PublicKeyCredential returned by navigator.credentials.get(), as JSON
    */
   public PasskeyToken(String requestId, String credential) {
      this.requestId = requestId;
      this.credential = credential;
   }

   @Override
   public Object getPrincipal() {
      return requestId;
   }

   @Override
   public String getCredentials() {
      return credential;
   }

   public String getRequestId() {
      return requestId;
   }

   @Override
   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   @Override
   public boolean isRememberMe() {
      return rememberMe;
   }

   public void setRememberMe(boolean rememberMe) {
      this.rememberMe = rememberMe;
   }

   @Override
   public String toString() {
      return "PasskeyToken [requestId=" + requestId + ", host=" + host + ", rememberMe=" + rememberMe + "]";
   }
}

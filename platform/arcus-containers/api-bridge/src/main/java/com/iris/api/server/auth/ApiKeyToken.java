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
package com.iris.api.server.auth;

import org.apache.shiro.authc.HostAuthenticationToken;

@SuppressWarnings("serial")
public class ApiKeyToken implements HostAuthenticationToken {

   private final String rawKey;
   private String host;

   public ApiKeyToken(String rawKey) {
      this.rawKey = rawKey;
   }

   public ApiKeyToken(String rawKey, String host) {
      this.rawKey = rawKey;
      this.host = host;
   }

   public String getRawKey() {
      return rawKey;
   }

   @Override
   public Object getPrincipal() {
      return rawKey;
   }

   @Override
   public Object getCredentials() {
      return rawKey;
   }

   @Override
   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   @Override
   public String toString() {
      return "ApiKeyToken [host=" + host + "]";
   }
}

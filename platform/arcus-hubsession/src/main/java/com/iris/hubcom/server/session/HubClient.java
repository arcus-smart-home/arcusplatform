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
package com.iris.hubcom.server.session;

import java.util.Date;

import org.apache.shiro.authc.AuthenticationException;

import com.iris.bridge.server.client.Client;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.principal.DefaultPrincipal;
import com.iris.security.principal.Principal;

public class HubClient implements Client {

   public static HubClient unauthenticated() {
      return new HubClient("AAA-0000") {
         @Override
         public boolean isAuthenticated() { return false; }
      };
   }
   
   private Date created;
   private Principal principal;
   private HubClientToken token;

   public HubClient(String hubId) {
      this.principal = new DefaultPrincipal(hubId, null);
      this.token = HubClientToken.fromHubID(hubId);
      this.created = new Date();
   }

   public HubClientToken getToken() {
      return token;
   }

   @Override
   public boolean isAuthenticated() {
      return true;
   }

   @Override
   public Principal getPrincipal() {
      return principal;
   }

   @Override
   public String getSessionId() {
      return "";
   }

   @Override
   public AuthorizationContext getAuthorizationContext() {
      return AuthorizationContext.EMPTY_CONTEXT;
   }

   @Override
   public Date getLoginTime() {
      return created;
   }

   @Override
   public Date getExpirationTime() {
      return null;
   }

   @Override
   public void resetExpirationTime() {
   }

   @Override
   public void login(Object credentials) throws AuthenticationException {
      // no-op
   }

   @Override
   public void logout() {
      // no-op
   }

}


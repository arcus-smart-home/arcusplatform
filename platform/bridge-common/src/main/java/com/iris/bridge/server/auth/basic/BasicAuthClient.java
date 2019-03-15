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
/**
 * 
 */
package com.iris.bridge.server.auth.basic;

import java.util.Collections;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.client.Client;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.principal.DefaultPrincipal;
import com.iris.security.principal.Principal;

/**
 * 
 */
public class BasicAuthClient implements Client {
   private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthClient.class);

   private final AuthorizationContext authContext =
         new AuthorizationContext(null, null, Collections.emptyList());

   private String username;
   private String password;

   public BasicAuthClient() {
   }

   public BasicAuthClient(String username,String password) {
      this.username = username;
      this.password = password;
   }
   
   public BasicAuthCredentials getCredentials(){
      return new BasicAuthCredentials(username, password);
   }

   @Override
   public boolean isAuthenticated() {
      return true;
   }

   @Override
   public Principal getPrincipal() {
      Principal principal = new DefaultPrincipal(username, null);
      return principal;
   }

   @Override
   public String getSessionId() {
      return null;
   }

   @Override
   public Date getLoginTime() {
      return null;
   }

   @Override
   public Date getExpirationTime() {
      return null;
   }

   @Override
   public void resetExpirationTime() {
   }

   @Override
   public AuthorizationContext getAuthorizationContext() {
      return authContext;
   }

   @Override
   public void login(Object credentials) {
   
   }

   @Override
   public void logout() {
   }

}


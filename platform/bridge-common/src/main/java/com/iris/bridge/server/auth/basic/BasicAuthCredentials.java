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
package com.iris.bridge.server.auth.basic;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;

public class BasicAuthCredentials {
   private static final String AUTH_TYPE_BASIC = "Basic";

   private String username;
   private String password;

   public BasicAuthCredentials(String username, String password) {
      this.username = username;
      this.password = password;
   }

   public String getUsername() {
      return username;
   }

   public String getPassword() {
      return password;
   }

   public static BasicAuthCredentials fromAuthHeaderString(String authString) {
      if(authString==null){
         throw new IllegalArgumentException("basic auth credentials expected");
      }
      if(authString.startsWith(AUTH_TYPE_BASIC)){
         authString = authString.substring(AUTH_TYPE_BASIC.length()).trim();
      }
      else{
         throw new IllegalArgumentException("basic auth credentials expected");
      }
      if (!StringUtils.isEmpty(authString)) {
         try{
            String[] auth = new String(Utils.b64Decode(authString)).split(":");
            String username = auth[0];
            String password = auth[1];
            return new BasicAuthCredentials(username, password);

         }
         catch(Exception e){
            throw new IllegalArgumentException("basic auth credentials expected");
         }
      }
      return null;
   }
}


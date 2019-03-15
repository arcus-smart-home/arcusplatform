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
package com.iris.oculus.modules.session;

/**
 * @author tweidlin
 *
 */
public class LoginCredentials {
   private Action action;
   private String serviceUri;
   private String username;
   private char[] password;

   public LoginCredentials() {

   }

   public LoginCredentials(Action action, String serviceUri, String username, char[] password) {
      this.action = action;
      this.serviceUri = serviceUri;
      this.username = username;
      this.password = password;
   }

   /**
    * @return the action
    */
   public Action getAction() {
      return action;
   }

   /**
    * @param action the action to set
    */
   public void setAction(Action action) {
      this.action = action;
   }

   public String getServiceUri() {
      return serviceUri;
   }

   public void setServiceUri(String serviceUri) {
      this.serviceUri = serviceUri;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public char[] getPassword() {
      return password;
   }

   public void setPassword(char[] password) {
      this.password = password;
   }

   public enum Action {
      LOGIN,
      CREATE_ACCOUNT,
      RESET_PASSWORD,
      ACCEPT_INVITE
   }
}


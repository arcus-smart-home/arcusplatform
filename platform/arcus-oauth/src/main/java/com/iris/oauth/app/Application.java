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
package com.iris.oauth.app;

import java.util.List;
import java.util.Set;

public class Application {

   private String id;
   private String secret;
   private String redirect;
   private String scope;
   private String name;
   private String loginTmpl;
   private List<Permission> permissions;
   private Set<String> extraQueryParams;
   private boolean sendsRedirectQueryParam;
   private String thirdParty;
   private String thirdPartyApp;
   private String thirdPartyDisplayName;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getSecret() {
      return secret;
   }

   public void setSecret(String secret) {
      this.secret = secret;
   }

   public String getRedirect() {
      return redirect;
   }

   public void setRedirect(String redirect) {
      this.redirect = redirect;
   }

   public String getScope() {
      return scope;
   }

   public void setScope(String scope) {
      this.scope = scope;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getLoginTmpl() {
      return loginTmpl;
   }

   public void setLoginTmpl(String loginTmpl) {
      this.loginTmpl = loginTmpl;
   }

   public List<Permission> getPermissions() {
      return permissions;
   }

   public void setPermissions(List<Permission> permissions) {
      this.permissions = permissions;
   }

   public Set<String> getExtraQueryParams() {
      return extraQueryParams;
   }

   public void setExtraQueryParams(Set<String> extraQueryParams) {
      this.extraQueryParams = extraQueryParams;
   }

   public boolean isSendsRedirectQueryParam() {
      return sendsRedirectQueryParam;
   }

   public void setSendsRedirectQueryParam(boolean sendsRedirectQueryParam) {
      this.sendsRedirectQueryParam = sendsRedirectQueryParam;
   }

   public String getThirdParty() {
      return thirdParty;
   }

   public void setThirdParty(String thirdParty) {
      this.thirdParty = thirdParty;
   }

   public String getThirdPartyApp() {
      return thirdPartyApp;
   }

   public void setThirdPartyApp(String thirdPartyApp) {
      this.thirdPartyApp = thirdPartyApp;
   }
   
   public String getThirdPartyDisplayName() {
      return thirdPartyDisplayName;
   }

   public void setThirdPartyDisplayName(String thirdPartyDisplayName) {
      this.thirdPartyDisplayName = thirdPartyDisplayName;
   }
}


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
package com.iris.oauth;

import java.util.Set;

import org.apache.shiro.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class OAuthConfig {

   @Inject(optional = true) @Named("oauth.code.ttl.minutes")
   private int codeTtlMinutes = 5;

   @Inject(optional = true) @Named("oauth.access.ttl.minutes")
   private int accessTtlMinutes = 10;

   @Inject(optional = true) @Named("oauth.refresh.ttl.days")
   private int refreshTtlDays = 30;

   @Inject(optional = true) @Named("oauth.refresh.ttl.buffer.days")
   private int refreshTtlBufferDays = 5;

   @Inject(optional = true) @Named("oauth.apps.path")
   private String appsPath = "conf/applications.json";

   @Inject @Named("oauth.base.url")
   private String oauthBaseUrl;

   @Inject @Named("static.resource.base.url")
   private String staticResourceBaseUrl;

   @Inject(optional = true) @Named("oauth.bind.endpoints")
   private String bindEndpoints = "/oauth/places";

   @Inject @Named("oauth.forgot.password.url")
   private String forgotPasswordUrl;

   public int getCodeTtlMinutes() {
      return codeTtlMinutes;
   }

   public void setCodeTtlMinutes(int codeTtlMinutes) {
      this.codeTtlMinutes = codeTtlMinutes;
   }

   public int getAccessTtlMinutes() {
      return accessTtlMinutes;
   }

   public void setAccessTtlMinutes(int accessTtlMinutes) {
      this.accessTtlMinutes = accessTtlMinutes;
   }

   public int getRefreshTtlDays() {
      return refreshTtlDays;
   }

   public void setRefreshTtlDays(int refreshTtlDays) {
      this.refreshTtlDays = refreshTtlDays;
   }

   public int getRefreshTtlBufferDays() {
      return refreshTtlBufferDays;
   }

   public void setRefreshTtlBufferDays(int refreshTtlBufferDays) {
      this.refreshTtlBufferDays = refreshTtlBufferDays;
   }

   public String getAppsPath() {
      return appsPath;
   }

   public void setAppsPath(String appsPath) {
      this.appsPath = appsPath;
   }

   public String getOauthBaseUrl() {
      return oauthBaseUrl;
   }

   public void setOauthBaseUrl(String oauthBaseUrl) {
      this.oauthBaseUrl = oauthBaseUrl;
   }

   public String getStaticResourceBaseUrl() {
      return staticResourceBaseUrl;
   }

   public void setStaticResourceBaseUrl(String staticResourceBaseUrl) {
      this.staticResourceBaseUrl = staticResourceBaseUrl;
   }

   public String getBindEndpoints() {
      return bindEndpoints;
   }

   public void setBindEndpoints(String bindEndpoints) {
      this.bindEndpoints = bindEndpoints;
   }

   public Set<String> bindEndpoints() {
      return StringUtils.splitToSet(getBindEndpoints(), ",");
   }

   public String getForgotPasswordUrl() {
      return forgotPasswordUrl;
   }

   public void setForgotPasswordUrl(String forgotPasswordUrl) {
      this.forgotPasswordUrl = forgotPasswordUrl;
   }
}


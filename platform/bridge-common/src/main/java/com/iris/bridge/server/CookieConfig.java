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
package com.iris.bridge.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class CookieConfig {

   private static final String DOMAIN_NAME_PROP = "domain.name";
   private static final String PROP_AUTH_COOKIE_NAME = "auth.cookie.name";
   private static final String DFLT_AUTH_COOKIE_NAME = "irisAuthToken";
	private static final String PROP_COOKIE_SECURE_ONLY = "auth.cookie.secure";

   @Inject(optional=true)
   @Named(PROP_AUTH_COOKIE_NAME)
   private String authCookieName = DFLT_AUTH_COOKIE_NAME;

   @Inject(optional=true)
   @Named(PROP_COOKIE_SECURE_ONLY)
   private boolean secureOnly = true;

   @Inject(optional = true)
   @Named(DOMAIN_NAME_PROP)
   private String domainName = null;

   public String getAuthCookieName() {
      return authCookieName;
   }

   public void setAuthCookieName(String authCookieName) {
      this.authCookieName = authCookieName;
   }

   public boolean isSecureOnly() {
      return secureOnly;
   }

   public void setSecureOnly(boolean secureOnly) {
      this.secureOnly = secureOnly;
   }

   public String getDomainName() {
      return domainName;
   }

   public void setDomainName(String domainName) {
      this.domainName = domainName;
   }

   public boolean isDomainNameSet() {
      return domainName != null && !"none".equals(domainName);
   }
}


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
package com.iris.platform.address.validation.smartystreets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class SmartyStreetsClientConfig
{
   public static final String NAME_SERVICE_BASE_URL = "smartystreets.servicebaseurl";
   public static final String NAME_AUTH_ID          = "smartystreets.authid";
   public static final String NAME_AUTH_TOKEN       = "smartystreets.authtoken";
   public static final String NAME_CANDIDATES       = "smartystreets.candidates";
   public static final String NAME_TIMEOUT_SECS     = "smartystreets.timeoutsecs";

   @Inject(optional = true) @Named(NAME_SERVICE_BASE_URL)
   private String serviceBaseUrl = "https://us-street.api.smartystreets.com/street-address";

   @Inject(optional = true) @Named(NAME_AUTH_ID)
   private String authId = "";

   @Inject(optional = true) @Named(NAME_AUTH_TOKEN)
   private String authToken = "";

   @Inject(optional = true) @Named(NAME_CANDIDATES)
   private int candidates = 3;

   @Inject(optional = true) @Named(NAME_TIMEOUT_SECS)
   private int timeoutSecs = 5;

   public String getServiceBaseUrl()
   {
      return serviceBaseUrl;
   }

   public String getAuthId()
   {
      return authId;
   }

   public String getAuthToken()
   {
      return authToken;
   }

   public int getCandidates()
   {
      return candidates;
   }

   public int getTimeoutSecs()
   {
      return timeoutSecs;
   }
}


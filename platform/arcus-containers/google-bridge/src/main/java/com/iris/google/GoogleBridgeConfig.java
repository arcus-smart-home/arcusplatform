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
package com.iris.google;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.voice.VoiceBridgeConfig;

@Singleton
public class GoogleBridgeConfig extends VoiceBridgeConfig {

   @Inject
   @Named("google.bridge.oauth.appid")
   private String oauthAppId;

   public String getOauthAppId() {
      return oauthAppId;
   }

   public void setOauthAppId(String oauthAppId) {
      this.oauthAppId = oauthAppId;
   }
}


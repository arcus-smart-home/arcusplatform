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
package com.iris.alexa.shs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.voice.VoiceBridgeConfig;

@Singleton
public class ShsConfig extends VoiceBridgeConfig {

   @Inject
   @Named("alexa.oauth.shs.appid")
   private String shsAppId;

   @Inject(optional = true)
   @Named("alexa.shs.health.check.timout.secs")
   private int healthCheckTimeoutSecs = 5;

   public String getShsAppId() {
      return shsAppId;
   }

   public void setShsAppId(String shsAppId) {
      this.shsAppId = shsAppId;
   }

   public int getHealthCheckTimeoutSecs() {
      return healthCheckTimeoutSecs;
   }

   public void setHealthCheckTimeoutSecs(int healthCheckTimeoutSecs) {
      this.healthCheckTimeoutSecs = healthCheckTimeoutSecs;
   }
}


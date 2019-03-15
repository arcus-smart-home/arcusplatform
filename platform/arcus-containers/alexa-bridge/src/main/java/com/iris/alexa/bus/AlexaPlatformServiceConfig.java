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
package com.iris.alexa.bus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class AlexaPlatformServiceConfig {

   @Inject(optional=true) @Named("alexa.bus.service.threads.max")
   private int maxListenerThreads = 20;

   @Inject(optional=true) @Named("alexa.bus.service.threads.keepalive")
   private int listenerThreadKeepAliveMs = 5000;

   @Inject(optional=true) @Named("alexa.bus.service.timeoutsecs.default")
   private int defaultTimeoutSecs = 10;

   public int getMaxListenerThreads() {
      return maxListenerThreads;
   }

   public void setMaxListenerThreads(int maxListenerThreads) {
      this.maxListenerThreads = maxListenerThreads;
   }

   public int getListenerThreadKeepAliveMs() {
      return listenerThreadKeepAliveMs;
   }

   public void setListenerThreadKeepAliveMs(int listenerThreadKeepAliveMs) {
      this.listenerThreadKeepAliveMs = listenerThreadKeepAliveMs;
   }

   public int getDefaultTimeoutSecs() {
      return defaultTimeoutSecs;
   }

   public void setDefaultTimeoutSecs(int defaultTimeoutSecs) {
      this.defaultTimeoutSecs = defaultTimeoutSecs;
   }
}


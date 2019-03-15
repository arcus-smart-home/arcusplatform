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
package com.iris.voice;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class VoiceBridgeConfig {

   public static final String NAME_EXECUTOR = "VoiceBridge#executor";
   public static final String NAME_BRIDGEADDRESS = "VoiceBridge#bridgeAddress";
   public static final String NAME_BRIDGEASSISTANT = "VoiceBridge#bridgeAssistant";

   @Inject(optional = true)
   @Named("voice.bridge.request.timeout.ms")
   private long requestTimeoutMs = TimeUnit.SECONDS.toMillis(30);

   @Inject(optional = true)
   @Named("voice.bridge.handler.max.threads")
   private int handlerMaxThreads = 20;

   @Inject(optional = true)
   @Named("voice.bridge.handler.thread.keep.alive.ms")
   private long handlerThreadKeepAliveMs = TimeUnit.MINUTES.toMillis(5);

   public long getRequestTimeoutMs() {
      return requestTimeoutMs;
   }

   public void setRequestTimeoutMs(long requestTimeoutMs) {
      this.requestTimeoutMs = requestTimeoutMs;
   }

   public int getHandlerMaxThreads() {
      return handlerMaxThreads;
   }

   public void setHandlerMaxThreads(int handlerMaxThreads) {
      this.handlerMaxThreads = handlerMaxThreads;
   }

   public long getHandlerThreadKeepAliveMs() {
      return handlerThreadKeepAliveMs;
   }

   public void setHandlerThreadKeepAliveMs(long handlerThreadKeepAliveMs) {
      this.handlerThreadKeepAliveMs = handlerThreadKeepAliveMs;
   }
}


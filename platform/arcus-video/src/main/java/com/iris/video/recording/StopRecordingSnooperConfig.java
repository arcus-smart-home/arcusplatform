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
package com.iris.video.recording;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class StopRecordingSnooperConfig {
   @Inject(optional = true) @Named("video.threads.max")
   private int maxThreads = 20;

   @Inject(optional = true) @Named("video.threads.keepalivems")
   private final int threadKeepAliveMs = 100;

   /**
    * @return the maxThreads
    */
   public int getMaxThreads() {
      return maxThreads;
   }

   /**
    * @param maxThreads the maxThreads to set
    */
   public void setMaxThreads(int maxThreads) {
      this.maxThreads = maxThreads;
   }

   /**
    * @return the threadKeepAliveMs
    */
   public int getThreadKeepAliveMs() {
      return threadKeepAliveMs;
   }
}


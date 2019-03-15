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
package com.iris.service.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SchedulerServiceConfig {

   // majority of the work should be in the dispatch threads
   @Inject(optional = true) @Named("platform.service.threads.max")
   private int threads = 20;
   @Inject(optional = true) @Named("platform.service.threads.keepAliveMs")
   private int keepAliveMs = 10000;
   
   /**
    * @return the threads
    */
   public int getThreads() {
      return threads;
   }
   
   /**
    * @param threads the threads to set
    */
   public void setThreads(int threads) {
      this.threads = threads;
   }
   
   /**
    * @return the keepAliveMs
    */
   public int getKeepAliveMs() {
      return keepAliveMs;
   }
   
   /**
    * @param keepAliveMs the keepAliveMs to set
    */
   public void setKeepAliveMs(int keepAliveMs) {
      this.keepAliveMs = keepAliveMs;
   }

}


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
package com.iris.platform.scheduler;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SchedulerConfig {
   public static final String PARAM_WINDOW_SIZE_SEC              = "scheduler.windowSizeSec";
   public static final String PARAM_HORIZON_SEC                  = "scheduler.horizonSec";
   public static final String PARAM_SCHEDULING_THREAD_POOL_SIZE  = "scheduler.schedulingThreadPoolSize";
   public static final String PARAM_DISPATCH_THREAD_POOL_SIZE    = "scheduler.dispatchThreadPoolSize";
   public static final String PARAM_DEFAULT_EXPIRATION_TIME_SEC  = "scheduler.defaultExpirationTimeSec";
   public static final String PARAM_SCHEDULER_SANITY_CHECK       = "scheduler.sanity.check";

   @Inject(optional = true) @Named(PARAM_WINDOW_SIZE_SEC)
   private int windowSizeSec = 60;
   @Inject(optional = true) @Named(PARAM_HORIZON_SEC)
   private int schedulerHorizonSec = 600;
   @Inject(optional = true) @Named(PARAM_SCHEDULING_THREAD_POOL_SIZE)
   private int schedulingThreadPoolSize = 5;
   @Inject(optional = true) @Named(PARAM_DISPATCH_THREAD_POOL_SIZE)
   private int dispatchThreadPoolSize = 40;
   @Inject(optional = true) @Named(PARAM_DEFAULT_EXPIRATION_TIME_SEC)
   private int defaultExpirationTimeSec = (int) TimeUnit.DAYS.toSeconds(1);
   @Inject(optional = true) @Named(PARAM_SCHEDULER_SANITY_CHECK)
   private boolean sanityCheckExisting = false;

   /**
    * @return the windowSizeSec
    */
   public int getWindowSizeSec() {
      return windowSizeSec;
   }
   
   /**
    * @param windowSizeSec the windowSizeSec to set
    */
   public void setWindowSizeSec(int windowSizeSec) {
      this.windowSizeSec = windowSizeSec;
   }
   
   /**
    * @return the schedulerHorizonSec
    */
   public int getSchedulerHorizonSec() {
      return schedulerHorizonSec;
   }
   
   /**
    * @param schedulerHorizonSec the schedulerHorizonSec to set
    */
   public void setSchedulerHorizonSec(int schedulerHorizonSec) {
      this.schedulerHorizonSec = schedulerHorizonSec;
   }

   /**
    * @return the schedulingThreadPoolSize
    */
   public int getSchedulingThreadPoolSize() {
      return schedulingThreadPoolSize;
   }

   /**
    * @param schedulingThreadPoolSize the schedulingThreadPoolSize to set
    */
   public void setSchedulingThreadPoolSize(int schedulingThreadPoolSize) {
      this.schedulingThreadPoolSize = schedulingThreadPoolSize;
   }

   /**
    * @return the dispatchThreadPoolSize
    */
   public int getDispatchThreadPoolSize() {
      return dispatchThreadPoolSize;
   }

   /**
    * @param dispatchThreadPoolSize the dispatchThreadPoolSize to set
    */
   public void setDispatchThreadPoolSize(int dispatchThreadPoolSize) {
      this.dispatchThreadPoolSize = dispatchThreadPoolSize;
   }

   /**
    * @return the defaultExpirationTimeSec
    */
   public int getDefaultExpirationTimeSec() {
      return defaultExpirationTimeSec;
   }

   /**
    * @param defaultExpirationTimeSec the defaultExpirationTimeSec to set
    */
   public void setDefaultExpirationTimeSec(int defaultExpirationTimeSec) {
      this.defaultExpirationTimeSec = defaultExpirationTimeSec;
   }

   /**
    * @return whether to sanity check or not
    */
   public boolean getSanityCheckExisting() {
      return sanityCheckExisting;
   }

   /**
    * @param sanityCheckExisting whether to sanity check or not
    */
   public void setSanityCheckExisting(boolean sanityCheckExisting) {
      this.sanityCheckExisting = sanityCheckExisting;
   }
}


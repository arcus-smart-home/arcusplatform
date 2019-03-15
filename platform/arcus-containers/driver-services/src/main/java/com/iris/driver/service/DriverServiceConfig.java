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
package com.iris.driver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DriverServiceConfig {
   private static final Logger log = LoggerFactory.getLogger(DriverServiceConfig.class);

   // most of the work happens in the driver pool
   @Inject(optional=true) @Named("driver.service.threads.max")
   private int maxThreads = 100;

   @Inject(optional=true) @Named("driver.service.threads.keepAliveMs")
   private int threadKeepAliveMs = 10000;

   @Inject(optional=true) @Named("driver.restorelostdevices")
   private boolean restoreLostDevices = false;

   @Inject(optional=true) @Named("driver.service.sync.size.warning")
   private int syncSizeWarning = 512 * 1024;

   /**
    * @return the maxThreads
    */
   public int getMaxThreads() {
      return maxThreads;
   }
   
   /**
    * @return the threadKeepAliveMs
    */
   public int getThreadKeepAliveMs() {
      return threadKeepAliveMs;
   }
   
   /**
    * @return the restoreLostDevices
    */
   public boolean isRestoreLostDevices() {
      return restoreLostDevices;
   }
   
   /**
    * @param restoreLostDevices the restoreLostDevices to set
    */
   public void setRestoreLostDevices(boolean restoreLostDevices) {
      this.restoreLostDevices = restoreLostDevices;
   }

   public int getSyncSizeWarning() {
      return syncSizeWarning;
   }
}


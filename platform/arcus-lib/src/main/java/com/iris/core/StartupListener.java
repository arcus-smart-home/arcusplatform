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
package com.iris.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bootstrap.ServiceLocator;

/**
 * Things implementing this interface will be notified when
 * the application is fully configured and started.
 */
// TODO would be nice if this was just an annotation
public interface StartupListener {

   void onStarted();
   
   public static void publishStarted() {
      Logger logger = LoggerFactory.getLogger(StartupListener.class);
      for(StartupListener listener: ServiceLocator.getInstancesOf(StartupListener.class)) {
         try {
            logger.debug("Notifying listener [{}] of app startup", listener);
            listener.onStarted();
         }
         catch(Exception e) {
            logger.warn("Error notifying listener [{}] of onStarted", listener, e);
         }
      }
   }
}


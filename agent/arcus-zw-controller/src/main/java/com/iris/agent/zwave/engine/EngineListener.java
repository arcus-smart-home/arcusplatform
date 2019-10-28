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
package com.iris.agent.zwave.engine;

/**
 * Listener for ZWaveEngine events.
 * 
 * @author Erik Larson
 */
public interface EngineListener {
   
   //////
   // Lifecycle Events
   /////

   /**
    * Called when the bootstrapping process has completed.
    */
   void onBootstrapSuccess(long homeId);
   
   /**
    * Called if the bootstrapping process has failed.
    */
   void onBootstrapFailure();
   
   /**
    * Called if the network has begun resetting.
    */
   void onNetworkResetting();
   
   /**
    * Called if the network has finished resetting.
    */
   void onNetworkResettingFinished();
   
   /**
    * Called when the network has been shutdown.
    */
   void onNetworkShutdown();
   
   
   //////
   // Notifications
   /////
   
   /**
    * Called when the network issues an unsolicited message.
    *  
    * @param the message
    */
   void onNotification(ZWaveEngineMsg value);
}

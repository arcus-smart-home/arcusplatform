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
package com.iris.agent.zwave;

/**
 * 
 * Constants and configuration data for the Zip Controller.
 * 
 * @author Erik Larson
 */
public class ZWConfig {
   /////////////////////////////////////////
   // Offline Monitoring Configuration Knobs
   /////////////////////////////////////////
   
   public static int getIncreaseFloor() {
   return 10;
   }
   
   public static int getBaseOfflineCheckPeriodInSecs() {
   return 60;
   }
   
   public static int getMinimumOfflineTimeoutInSecs() {
   return 300;
   }
   
   public static int getOfflineCheckPollingDelayInMillis() {
   return 200;
   }
   
   public static int getMaxOfflineChecksBeforeMetering() {
   return 10;
   }
   
   public static int getMinimumOfflineTimeoutIncreaseInMillis() {
   return 0;
   }
   
   public static int getMeteringIncreaseInMillis() {
   return 1000;
   }
   
   public static int getOfflinePollingDelayIncreaseInMillis() {
   return 0;
   }
   
   public static int getNumberOfStrikesBeforeDeviceGoesOffline() {
   return 2;
   }
   
   /**
    * Set to false if writing a test program that uses the Zip Controller
    * without the rest of the agent.
    */
   public final static boolean HAS_AGENT = true;
   
   /**
    * The number of listening cycles that come up empty before shutting
    * down a client.
    */
   public final static int LISTEN_LOOP_TERMINATION = 40;
   
   /**
    * Constant to use to indicate that a client should not be shut down
    * no matter how many listening cycles come up empty. Generally only used for 
    * for the controller node.
    */
   public final static int NO_LISTEN_LOOP_TERMINATION = 0;
   
   /**
    * The number of times to retry a message for which there is no response.
    */
   public final static int RETRY_LIMIT = 5;
   
   /**
    * The amount of time between retries.
    */
   //TODO: Until retry policy is setup.
   public final static long MINIMUM_RETRY_PERIOD = 1500;
   
   /**
    * The ZWave Broadcast Node Id
    */
   public final static int BROADCAST_NODE_ID = 0xFF;
   
   /**
    * The ZWave Gateway Node Id
    */
   public final static int GATEWAY_NODE_ID = 0x01;
}

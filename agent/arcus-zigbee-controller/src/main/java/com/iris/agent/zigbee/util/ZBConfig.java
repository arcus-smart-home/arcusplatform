/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.util;

public class ZBConfig {

   public static int getBaseOfflineCheckPeriodInSecs() {
      return 60;
   }

   public static int getMinimumOfflineTimeoutInSecs() {
      return 300;
   }

   public static int getOfflineCheckPollingDelayInMillis() {
      return 200;
   }

   public static int getIncreaseFloor() {
      return 10;
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
}

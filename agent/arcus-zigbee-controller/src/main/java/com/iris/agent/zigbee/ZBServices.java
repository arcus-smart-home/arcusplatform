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

package com.iris.agent.zigbee;

import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.agent.zigbee.service.ZBOfflineService;

public class ZBServices {
   public static final ZBServices INSTANCE = new ZBServices();

   private ZBNetwork zbNetwork;
   private ZigbeeDriver driver;
   private ZBOfflineService zbOfflineService;

   private ZBServices() {}

   public ZBNetwork getNetwork() {
      if (zbNetwork == null) {
         zbNetwork = new ZBNetwork();
      }
      return zbNetwork;
   }

   public ZigbeeDriver getDriver() {
      return driver;
   }

   public void setDriver(ZigbeeDriver driver) {
      this.driver = driver;
   }

   public ZBOfflineService getOfflineService() {
      if (zbOfflineService == null) {
         zbOfflineService = new ZBOfflineService();
      }
      return zbOfflineService;
   }
}

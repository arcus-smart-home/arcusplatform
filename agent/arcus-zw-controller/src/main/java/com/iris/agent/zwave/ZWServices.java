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

import com.iris.agent.zwave.engine.ZWaveEngine;
import com.iris.agent.zwave.service.ZWOfflineService;

/**
 * Acts as a non-reflective service locator.
 * 
 * @author Erik Larson
 */
public class ZWServices {
   public final static ZWServices INSTANCE = new ZWServices();
   
   // Services to load before any lazy loaded services.
   private ZWAttributes zwAttributes = new ZWAttributes();
   private ZWLEDsAndSounds zwLEDsAndSounds = new ZWLEDsAndSounds();

   // Services to lazy load.
   private ZWNetwork zwNetwork;
   private ZWaveEngine zWaveEngine;
   private ZWOfflineService zwOfflineService;
   
   private ZWServices() {}
   
   public ZWAttributes getZWAttributes() {
      return zwAttributes;
   }

   public ZWLEDsAndSounds getZWLedsAndSounds() {
      return zwLEDsAndSounds;
   }
   
   public ZWaveEngine getZWaveEngine() {
      if (zWaveEngine == null) {
         //zWaveEngine = new ??
      }
      return zWaveEngine;
   }

   public ZWNetwork getNetwork() {
      if (zwNetwork == null) {
         zwNetwork = new ZWNetwork();
      }
      return zwNetwork;
   }
      
   public ZWOfflineService getZWOfflineService() {
      if (zwOfflineService == null) {
         zwOfflineService = new ZWOfflineService();
      }
      return zwOfflineService;
   }
}

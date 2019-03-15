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
package com.iris.alexa.message.v2.response;

import java.util.Collections;
import java.util.List;

import com.iris.alexa.message.v2.Appliance;

public class DiscoverAppliancesResponse implements ResponsePayload {

   private List<Appliance> discoveredAppliances;

   public List<Appliance> getDiscoveredAppliances() {
      return discoveredAppliances == null ? Collections.emptyList() : discoveredAppliances;
   }

   public void setDiscoveredAppliances(List<Appliance> discoveredAppliances) {
      this.discoveredAppliances = discoveredAppliances;
   }

   @Override
   public String getNamespace() {
      return "Alexa.ConnectedHome.Discovery";
   }

   @Override
   public String toString() {
      return "DiscoverAppliancesResponse [discoveredAppliances="
            + discoveredAppliances + ']';
   }

}


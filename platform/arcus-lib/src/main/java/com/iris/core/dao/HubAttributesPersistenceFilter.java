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
package com.iris.core.dao;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.HubVolumeCapability;
import com.iris.messages.capability.HubZigbeeCapability;
import com.iris.messages.capability.HubZwaveCapability;

public class HubAttributesPersistenceFilter {

   private static final Set<String> EXCLUDE = ImmutableSet.<String>of(
         Capability.ATTR_CAPS,                        // already a column
         HubAdvancedCapability.ATTR_AGENTVER,         // already a column
         HubAdvancedCapability.ATTR_BOOTLOADERVER,    // already a column
         HubAdvancedCapability.ATTR_HARDWAREVER,      // already a column
         HubAdvancedCapability.ATTR_MAC,              // already a column
         HubAdvancedCapability.ATTR_MFGINFO,          // already a column
         HubAdvancedCapability.ATTR_OSVER,            // already a column
         HubAdvancedCapability.ATTR_SERIALNUM,        // already a column
         HubCapability.ATTR_ACCOUNT,                  // already a column
         HubCapability.ATTR_ID,                       // already a column
         HubCapability.ATTR_MODEL,                    // already a column
         HubCapability.ATTR_PLACE,                    // already a column
         HubCapability.ATTR_STATE,                    // already a column
         HubCapability.ATTR_TIME,                     // computed - no value changes
         HubCapability.ATTR_VENDOR,                   // already a column
         HubNetworkCapability.ATTR_DNS,               // supposed to be a string but the hub reports an array
         HubNetworkCapability.ATTR_INTERFACES,        // computed - no value changes
         HubNetworkCapability.ATTR_UPTIME,            // computed - no value changes
         HubSoundsCapability.ATTR_PLAYING,            // not interesting
         HubSoundsCapability.ATTR_SOURCE,             // not interesting
         HubVolumeCapability.ATTR_VOLUME,             // not interesting
         HubZigbeeCapability.ATTR_UPTIME,             // computed - no value changes
         HubZwaveCapability.ATTR_UPTIME               // computed - no value changes

   );

   public Map<String,Object> filter(Map<String,Object> attrs) {
      if(attrs == null) {
         return Collections.emptyMap();
      }

      return attrs.entrySet().stream()
            .filter((e) -> e.getValue() != null)
            .filter((e) -> !EXCLUDE.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
   }
}


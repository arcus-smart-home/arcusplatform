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
package com.iris.driver.groovy.zigbee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.protocol.zigbee.ZigbeeProtocol;

public class ZigbeeFixtures {
   public static AttributeMap createProtocolAttributes() {     
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_EUI64, Long.class), 9749369604284978L);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_NWK, Integer.class), 38433);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_MAXITS, Integer.class), 82);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_MAXOTS, Integer.class), 82);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_NFLAGS, Integer.class), 16385);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_SMASK, Integer.class), 0);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_MANUFACTURER, Integer.class), -15649);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_DCAP, Integer.class), 0);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_MAXBUF, Integer.class), 82);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_MCAP, Integer.class), -114);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_PDESC, Integer.class), -16112);
      attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_VENDOR, String.class), "CentraLite");
      // attributes.set(AttributeKey.create(ZigbeeProtocol.ATTR_MODEL
      Set<Object> profiles = new HashSet<>();
      profiles.add(generateProfileAttributes((short)260, new byte[] {1, 2}));
      profiles.add(generateProfileAttributes((short)-15850, new byte[] {-16}));
      
      attributes.set(AttributeKey.createSetOf(ZigbeeProtocol.ATTR_PROFILES, Object.class), profiles);
      return attributes;
   }
   
   private static Map<String, Object> generateProfileAttributes(short profileId, byte[] endpointIds) {
      List<Object> endpoints = new ArrayList<>();
      for (byte endpointId : endpointIds) {
         Map<String, Object> endpointAttrs = generateEndpointAttributes(endpointId);
         endpoints.add(endpointAttrs);
      }
      Map<String, Object> profileAttrs = new HashMap<>();
      profileAttrs.put("hubzbprofile:id", profileId);
      profileAttrs.put("hubzbprofile:endpoints", endpoints);
      return profileAttrs;
   }
   
   private static Map<String, Object> generateEndpointAttributes(byte endpoint) {
      Map<String, Object> attributes = new HashMap<>();
      
      attributes.put("hubzbendpoint:devver", 0);
      attributes.put("hubzbendpoint:datecode", "");
      attributes.put("hubzbendpoint:pwr", 3);
      attributes.put("hubzbendpoint:appver", 48);
      attributes.put("hubzbendpoint:id", endpoint);
      attributes.put("hubzbendpoint:manuf", "AlertMe.com");
      attributes.put("hubzbendpoint:zclver", 1);
      attributes.put("hubzbendpoint:hwver", 1);
      attributes.put("hubzbendpoint:model", "BTN00140004");
      attributes.put("hubzbendpoint:stkver", 2);
      attributes.put("hubzbendpoint:devid", 0);
      
      List<Object> clients = new ArrayList<>();
      Map<String, Object> clientAttr = new HashMap<>();
      clientAttr.put("hubzbcluster:id", (short)6);
      clientAttr.put("hubzbcluster:server", false);
      clients.add(clientAttr);
      attributes.put("hubzbendpoint:ccls", clients);
      
      List<Object> servers = new ArrayList<>();
      for (short clusterid : new short[] {0, 32, 1, 1026, 3}) {
         Map<String, Object> serverAttr = new HashMap<>();
         serverAttr.put("hubzbcluster:id", clusterid);
         serverAttr.put("hubzbcluster:server", true);
      }
      attributes.put("hubzbendpoint:scls", servers);
      
      return attributes;
   }
}


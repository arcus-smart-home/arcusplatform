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

import groovy.lang.GroovyObjectSupport;

import java.util.Map;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.protocol.constants.ZigbeeConstants;
import com.iris.protocol.zigbee.ZigbeeProtocol;

public class Hub extends GroovyObjectSupport {
   private static final AttributeKey<Map<String,Object>> KEY = AttributeKey.createMapOf(ZigbeeConstants.ATTR_HUB, Object.class);

   @Override
   public Object getProperty(String property) {
      AttributeMap map = GroovyContextObject.getContext().getProtocolAttributes();
      if (map == null) {
         return null;
      }

      Map<String,Object> hubAttrs = map.get(KEY);
      if (hubAttrs == null) {
         return null;
      }

      Object value = hubAttrs.get(property);

      switch (property) {
      case ZigbeeProtocol.ATTR_HUB_EUI64:
         return ((Number)value).longValue();

      default:
         return value;
      }
   }

   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Properties cannot be set on the Hub object");
   }
}


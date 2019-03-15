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
package com.iris.driver.groovy.ipcd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.protocol.ipcd.IpcdProtocol;

import groovy.lang.GroovyObjectSupport;

public class IpcdAttributes extends GroovyObjectSupport {
   private final static int NAMESPACE_LENGTH = IpcdProtocol.NAMESPACE.length() + 1;
   private final Map<String, Object> attributes;
   
   IpcdAttributes(AttributeMap attributes) {
      if (attributes != null && !attributes.isEmpty()) {
         Map<String, Object> map = new HashMap<>(attributes.size());
         for (AttributeKey<?> key : attributes.keySet()) {
            map.put(key.getName().substring(NAMESPACE_LENGTH), attributes.get(key));
         }
         this.attributes = Collections.unmodifiableMap(map);
      }
      else {
         this.attributes = Collections.emptyMap();
      }
   }
   
   @Override
   public Object getProperty(String property) {
      Object value = attributes.get(property);
      if (value != null) {
         return value;
      }
      return super.getProperty(property);
   }

   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Properties cannot be set on the IpcdAttributes object");
   }
}


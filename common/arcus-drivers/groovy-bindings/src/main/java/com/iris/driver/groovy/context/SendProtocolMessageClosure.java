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
/**
 *
 */
package com.iris.driver.groovy.context;

import groovy.lang.Closure;

import org.apache.commons.lang3.StringUtils;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DriverConstants;
import com.iris.driver.groovy.GroovyContextObject;

/**
 * Allows a byte-array to be sent to the associated
 * device. The correlation id is retrieved from
 * the current context.
 */
public class SendProtocolMessageClosure extends Closure<Object> {

   public SendProtocolMessageClosure(Object owner) {
      super(owner);
   }

   protected void doCall(byte[] buffer) {
      doCall(null, buffer, -1);
   }

   protected void doCall(byte[] buffer, int timeoutMs) {
      doCall(null, buffer, timeoutMs);
   }

   protected void doCall(String protocolName, byte[] buffer) {
      doCall(protocolName, buffer, -1);
   }

   protected void doCall(String protocolName, byte[] buffer, int timeoutMs) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      if(StringUtils.isEmpty(protocolName)) {
         protocolName = context.getAttributeValue(DriverConstants.DEVADV_ATTR_PROTOCOL);
      }
      if(StringUtils.isEmpty(protocolName)) {
         throw new IllegalArgumentException("No protocol is specified for this device, must explicitly specify protocol name");
      }
      context.sendToDevice(protocolName, buffer, timeoutMs);
   }

}


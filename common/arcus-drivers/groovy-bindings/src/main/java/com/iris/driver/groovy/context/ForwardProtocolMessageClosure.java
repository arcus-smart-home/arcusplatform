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
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.protocol.Protocol;
import com.iris.protocol.Protocols;

/**
 * Allows a byte-array to be sent to the associated
 * device. The correlation id is retrieved from
 * the current context.
 */
public class ForwardProtocolMessageClosure extends Closure<Object> {

   public ForwardProtocolMessageClosure(Object owner) {
      super(owner);
   }

   protected <M> void doCall(String dest, M payload) {
      doCall(dest, null, payload, -1);
   }

   protected <M> void doCall(String dest, M payload, int timeoutMs) {
      doCall(dest, null, payload, timeoutMs);
   }

   protected <M> void doCall(String dest, String protocolName, M payload) {
      doCall(dest, protocolName, payload, -1);
   }

   protected <M> void doCall(String dest, String protocolName, M payload, int timeoutMs) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      Protocol<M> prot = getProtocol(context, protocolName);
      DeviceProtocolAddress addr = getDestinationAddress(dest);
      context.forwardToDevice(addr, prot, payload, timeoutMs);
   }

   protected void doCall(String dest, byte[] buffer) {
      doCall(dest, null, buffer, -1);
   }

   protected void doCall(String dest, byte[] buffer, int timeoutMs) {
      doCall(dest, null, buffer, timeoutMs);
   }

   protected void doCall(String dest, String protocolName, byte[] buffer) {
      doCall(dest, protocolName, buffer, -1);
   }

   protected void doCall(String dest, String protocolName, byte[] buffer, int timeoutMs) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      protocolName = getProtocolName(context, protocolName);
      DeviceProtocolAddress addr = getDestinationAddress(dest);
      context.forwardToDevice(addr, protocolName, buffer, timeoutMs);
   }

   private String getProtocolName(DeviceDriverContext context, String protocolName) {
      if(StringUtils.isEmpty(protocolName)) {
         protocolName = context.getAttributeValue(DriverConstants.DEVADV_ATTR_PROTOCOL);
      }
      if(StringUtils.isEmpty(protocolName)) {
         throw new IllegalArgumentException("No protocol is specified for this device, must explicitly specify protocol name");
      }
      return protocolName;
   }

   @SuppressWarnings("unchecked")
   private <M> Protocol<M> getProtocol(DeviceDriverContext context, String protocolName) {
      return (Protocol<M>) Protocols.getProtocolByName(getProtocolName(context, protocolName));
   }

   private DeviceProtocolAddress getDestinationAddress(String address) {
      if(StringUtils.isEmpty(address)) {
         throw new IllegalArgumentException("A destination address must be specified to forward");
      }
      Address addr = Address.fromString(address);
      if(addr instanceof DeviceProtocolAddress) {
         return (DeviceProtocolAddress) addr;
      }
      throw new IllegalArgumentException("The destination address must be a device protocol address.");
   }

}


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

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.IpcdCommand;

public class IpcdMessageUtil {
   private final static int NO_TIMEOUT = -1;
   private final static int DEFAULT_TIMEOUT = NO_TIMEOUT;

   public static void send(String json) {
      send(json, DEFAULT_TIMEOUT);
   }
   
   public static void send(String json, int msTimeout) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(IpcdProtocol.NAMESPACE, json.getBytes(), msTimeout);
   }
   
   public static void send(IpcdMessage ipcdMsg) {
      send(ipcdMsg, null, DEFAULT_TIMEOUT);
   }
   
   public static void send(IpcdMessage ipcdMsg, int msTimeout) {
      send(ipcdMsg, null, msTimeout);
   }
   
   public static void send(IpcdMessage ipcdMsg, String txnid) {
      send(ipcdMsg, txnid, DEFAULT_TIMEOUT);
   }
   
   public static void send(IpcdMessage ipcdMsg, String txnid, int msTimeout) {
      if (txnid != null && ipcdMsg instanceof IpcdCommand) {
         ((IpcdCommand)ipcdMsg).setTxnid(txnid);
      }
      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(IpcdProtocol.INSTANCE, ipcdMsg, msTimeout);
   }
}


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

import java.util.HashMap;
import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.ipcd.bindings.Commands;
import com.iris.driver.groovy.ipcd.bindings.Data;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.StatusType;

import groovy.lang.GroovyObjectSupport;

public class IpcdContext extends GroovyObjectSupport {
   
   private final static String COMMAND_PROPERTY             = "Commands";
   private final static String DATA_PROPERTY                = "Data";
   private final static String MSG_TYPE_COMMAND_PROPERTY    = "MsgCommand";
   private final static String MSG_TYPE_RESPONSE_PROPERTY   = "MsgResponse";
   private final static String MSG_TYPE_EVENT_PROPERTY      = "MsgEvent";
   private final static String MSG_TYPE_REPORT_PROPERTY     = "MsgReport";
   private final static String STATUS_TYPE_SUCCESS_PROPERTY = "Success";
   private final static String STATUS_TYPE_WARN_PROPERTY    = "Warn";
   private final static String STATUS_TYPE_FAIL_PROPERTY    = "Fail";
   private final static String STATUS_TYPE_ERROR_PROPERTY   = "Error";
   private final static String IPCD_ATTRIBUTES_PROPERTY     = "Attributes";
   
   private final Map<String, Object> properties = new HashMap<>();
   
   private ContextualEventHandler<IpcdMessage> ipcdDispatchHandler = null;
   private IpcdAttributes ipcdAttributes = null;
   
   public IpcdContext() {
      properties.put(COMMAND_PROPERTY, new Commands());
      properties.put(DATA_PROPERTY,    new Data());
      properties.put(MSG_TYPE_COMMAND_PROPERTY,    MessageType.command);
      properties.put(MSG_TYPE_RESPONSE_PROPERTY,   MessageType.response);
      properties.put(MSG_TYPE_EVENT_PROPERTY,      MessageType.event);
      properties.put(MSG_TYPE_REPORT_PROPERTY,     MessageType.report);
      properties.put(STATUS_TYPE_SUCCESS_PROPERTY, StatusType.success);
      properties.put(STATUS_TYPE_WARN_PROPERTY,    StatusType.warn);
      properties.put(STATUS_TYPE_FAIL_PROPERTY,    StatusType.fail);
      properties.put(STATUS_TYPE_ERROR_PROPERTY,   StatusType.error);
   }
   
   @Override
   public Object getProperty(String property) {
      if (property.equals(IPCD_ATTRIBUTES_PROPERTY)) {
         return getIpcdAttributes();
      }
      Object value = properties.get(property);
      return value != null ? value : super.getProperty(property);
   }

   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Properties may not be set on the IPCD context object");
   }

   public void send(String json) {
      IpcdMessageUtil.send(json);
   }
   
   public void send(String json, int timeoutMs) {
      IpcdMessageUtil.send(json, timeoutMs);
   }
   
   public void send(IpcdMessage ipcdMsg) {
      IpcdMessageUtil.send(ipcdMsg);
   }
   
   public void send(IpcdMessage ipcdMsg, int timeoutMs) {
      IpcdMessageUtil.send(ipcdMsg, timeoutMs);
   }
   
   public void dispatch(IpcdMessage ipcdMsg) {
      if (ipcdDispatchHandler != null) {
         DeviceDriverContext context = GroovyContextObject.getContext();
         try {
            ipcdDispatchHandler.handleEvent(context, ipcdMsg);
         } catch (Exception e) {
            throw new RuntimeException("Exception while attempting to dispatch ipcd message.", e);
         }
      }
   }
   
   void setDispatchHandler(ContextualEventHandler<IpcdMessage> handler) {
      ipcdDispatchHandler = handler;
   }
   
   private IpcdAttributes getIpcdAttributes() {
      if (ipcdAttributes == null) {
         DeviceDriverContext context = GroovyContextObject.getContext();
         ipcdAttributes = new IpcdAttributes(context.getProtocolAttributes());
      }
      return ipcdAttributes;
   }
}


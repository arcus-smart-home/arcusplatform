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

import java.util.List;
import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.AbstractDispatchingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.protocol.ipcd.message.model.IpcdReport;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.ValueChange;

public class IpcdDispatchHandler 
   extends AbstractDispatchingHandler<Object> 
   implements ContextualEventHandler<IpcdMessage> 
{
   public static IpcdDispatchHandler.Builder builder() {
      return new Builder();
   }

   protected IpcdDispatchHandler(Map<String, ContextualEventHandler<? super Object>> handlers) {
      super(handlers);
   }
   
   @Override
   public boolean handleEvent(DeviceDriverContext context, IpcdMessage msg) throws Exception {
      MessageType messageType = msg.getMessageType();
      if (messageType == MessageType.report) {
         dispatchReports(context, (IpcdReport)msg);
      }
      else if (messageType == MessageType.event) {
         dispatchEvents(context, (IpcdEvent)msg);
      }
      return false;
   }
   
   protected boolean dispatchEvents(DeviceDriverContext context, IpcdEvent ipcdEvent) throws Exception {
      List<String> events = ipcdEvent.getEvents();
      List<ValueChange> changes = ipcdEvent.getValueChanges();
      if ((events == null || events.isEmpty()) && (changes == null || changes.isEmpty())) {
         // Consider the message handled since there was nothing to do.
         return true;
      }
      // As long as one thing in the event is handled. It's considered handled.
      boolean handled = false;
      if (events != null) {
         for (String event : events) {
            if (handleEvent(context, event)) {
               handled = true;
            }
         }
      }
      if (changes != null) {
         for (ValueChange change : changes) {
            if (handleValueChange(context, change)) {
               handled = true;
            }
         }
      }
      return handled;
   }
   
   protected boolean dispatchReports(DeviceDriverContext context, IpcdReport ipcdReport) throws Exception {
      Map<String, Object> reports = ipcdReport.getReport();
      if (reports == null || reports.isEmpty()) {
         // Go ahead and return true. Everything that was in the message (i.e. nothing) was handled.
         return true;
      }
      // As long as one report is handled, the message is considered handled.
      boolean handled = false;
      for (Map.Entry<String, Object> report : reports.entrySet()) {
         if (handleReport(context, report.getKey(), report.getValue())) {
            handled = true;
         }
      }
      return handled;
   }
   
   protected boolean handleValueChange(DeviceDriverContext context, ValueChange change) throws Exception {
      return deliver(makeValueChangeKey(change), context, change);
   }
   
   protected boolean handleEvent(DeviceDriverContext context, String event) throws Exception {
      return deliver(makeEventKey(event), context, event);
   }
   
   protected boolean handleReport(DeviceDriverContext context, String parameter, Object value) throws Exception {
      if (deliver(makeReportKey(parameter), context, value)) {
         return true;
      }
      return deliver(makeDispatchKey(MessageType.report), context, value);
   }
   
   public static class Builder extends AbstractDispatchingHandler.Builder<Object, IpcdDispatchHandler> {
      private Builder() {}
      
      public Builder addHandler(MessageType type, String event, String parameter, ContextualEventHandler<Object> handler) {
         doAddHandler(makeDispatchKey(type, event, parameter), handler);
         return this;
      }

      @Override
      protected IpcdDispatchHandler create(Map<String, ContextualEventHandler<? super Object>> handlers) {
         return new IpcdDispatchHandler(handlers);
      }
   }
   
   static String makeValueChangeKey(ValueChange change) {
      return makeDispatchKey(MessageType.event, "onValueChange", change.getParameter());
   }
   
   static String makeEventKey(String event) {
      return makeDispatchKey(MessageType.event, event, null);
   }
   
   static String makeReportKey(String parameter) {
      return makeDispatchKey(MessageType.report, null, parameter);
   }
   
   static String makeDispatchKey(MessageType type) {
      return makeDispatchKey(type, null, null);
   }
   
   static String makeDispatchKey(MessageType type, String event) {
      return makeDispatchKey(type, event, null);
   }
   
   static String makeDispatchKey(MessageType type, String event, String parameter) {
      StringBuffer sb = new StringBuffer();
      sb.append(type.name().toLowerCase());
      sb.append(":");
      sb.append(event != null ? event.toLowerCase() : "ANY");
      sb.append(":");
      sb.append(parameter != null ? parameter.toLowerCase() : "ANY");
      return sb.toString();
   }
}

